/**
 * Copyright 2026 SkillTree
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package skills.services.help

import groovy.util.logging.Slf4j
import jakarta.persistence.criteria.Predicate
import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.auth.UserInfoService
import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.SkillException
import skills.controller.exceptions.SkillsValidator
import skills.controller.request.model.HelpReplyRequest
import skills.controller.request.model.HelpRequestCreateRequest
import skills.controller.result.model.HelpReplyRes
import skills.controller.result.model.HelpRequestRes
import skills.controller.result.model.HelpStatsRes
import skills.controller.result.model.TableResult
import skills.services.CustomValidationResult
import skills.services.CustomValidator
import skills.services.userActions.DashboardAction
import skills.services.userActions.DashboardItem
import skills.services.userActions.UserActionInfo
import skills.services.userActions.UserActionsHistoryService
import skills.storage.accessors.ProjDefAccessor
import skills.storage.model.HelpReply
import skills.storage.model.HelpRequest
import skills.storage.model.ProjDef
import skills.storage.model.SkillDef
import skills.storage.model.UserAttrs
import skills.storage.model.auth.RoleName
import skills.storage.repos.HelpReplyRepo
import skills.storage.repos.HelpRequestRepo
import skills.storage.repos.SkillDefRepo
import skills.storage.repos.UserAttrsRepo
import skills.storage.repos.UserRoleRepo
import skills.utils.InputSanitizer

@Service
@Slf4j
class HelpService {

    @Autowired
    HelpRequestRepo helpRequestRepo

    @Autowired
    HelpReplyRepo helpReplyRepo

    @Autowired
    UserInfoService userInfoService

    @Autowired
    UserRoleRepo userRoleRepo

    @Autowired
    UserAttrsRepo userAttrsRepo

    @Autowired
    SkillDefRepo skillDefRepo

    @Autowired
    ProjDefAccessor projDefAccessor

    @Autowired
    CustomValidator customValidator

    @Autowired
    HelpNotifier helpNotifier

    @Autowired
    UserActionsHistoryService userActionsHistoryService

    @Value('${skills.config.help.maxTitleLength:100}')
    int maxTitleLength

    @Value('${skills.config.help.maxBodyLength:3000}')
    int maxBodyLength

    @Value('${skills.config.help.maxOpenPerUserPerProject:10}')
    int maxOpenPerUserPerProject

    private static final String STAFF = 'STAFF'
    private static final String STUDENT = 'STUDENT'
    static final List<String> ORDER_BY_OPTIONS = ['created', 'updated', 'title', 'status', 'userId']

    // ---------------------------------------------------------------- caller identity

    /** the caller's staff role in this project, null if the caller is not project staff */
    private HelpReply.AuthorRole staffRole(String projectId) {
        if (userInfoService.isCurrentUserASuperDuperUser()) {
            return HelpReply.AuthorRole.ADMIN
        }
        String userId = userInfoService.currentUserId
        List<RoleName> roles = userRoleRepo.findAllByProjectIdAndUserId(projectId, userId).collect { it.role.roleName }
        if (roles.contains(RoleName.ROLE_PROJECT_ADMIN)) {
            return HelpReply.AuthorRole.ADMIN
        }
        if (roles.contains(RoleName.ROLE_PROJECT_APPROVER)) {
            return HelpReply.AuthorRole.APPROVER
        }
        return null
    }

    private HelpReply.AuthorRole requireStaff(String projectId) {
        HelpReply.AuthorRole role = staffRole(projectId)
        if (!role) {
            throw new SkillException("User [${userInfoService.currentUserId}] is not allowed to manage help requests for project [${projectId}]", projectId, null, ErrorCode.AccessDenied)
        }
        return role
    }

    /**
     * Loads and authorizes. studentEndpoint=true: caller must own the request. Otherwise caller must be project staff.
     * Both checks verify the request belongs to the project in the path. A non-owner student gets the same error as a missing id.
     */
    private HelpRequest loadAuthorized(String projectId, Integer id, boolean studentEndpoint, boolean forUpdate = false) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        if (!studentEndpoint) {
            requireStaff(projectId)
        }
        HelpRequest request = forUpdate ? helpRequestRepo.findByIdForUpdate(id) : helpRequestRepo.findById(id).orElse(null)
        if (!request || request.projectId != projectId) {
            throw new SkillException("Help request [${id}] was not found for project [${projectId}]", projectId, null, ErrorCode.BadParam)
        }
        if (studentEndpoint && request.userId != userInfoService.currentUserId) {
            throw new SkillException("Help request [${id}] was not found for project [${projectId}]", projectId, null, ErrorCode.BadParam)
        }
        return request
    }

    // ---------------------------------------------------------------- create / read

    @Transactional
    HelpRequestRes createQuestion(String projectId, HelpRequestCreateRequest req) {
        ProjDef projDef = projDefAccessor.getProjDef(projectId)
        SkillsValidator.isNotNull(req, "request", projectId)
        SkillsValidator.isNotNull(req.scopeType, "scopeType", projectId)
        SkillsValidator.isNotBlank(req.title, "title", projectId)
        SkillsValidator.isNotBlank(req.body, "body", projectId)
        SkillsValidator.isTrue(req.title.trim().length() <= maxTitleLength, "title must not exceed [${maxTitleLength}] characters", projectId)
        SkillsValidator.isTrue(req.body.length() <= maxBodyLength, "body must not exceed [${maxBodyLength}] characters", projectId)
        validateText(req.title, "title", projectId)
        validateText(req.body, "body", projectId)

        Integer skillRefId = null
        if (req.scopeType == HelpRequest.ScopeType.PROJECT) {
            SkillsValidator.isTrue(StringUtils.isBlank(req.skillId), "skillId must not be provided when scopeType is PROJECT", projectId)
        } else {
            SkillsValidator.isNotBlank(req.skillId, "skillId", projectId)
            SkillDef.ContainerType expectedType = req.scopeType == HelpRequest.ScopeType.SUBJECT ? SkillDef.ContainerType.Subject : SkillDef.ContainerType.Skill
            SkillDef skillDef = skillDefRepo.findByProjectIdAndSkillIdAndType(projectId, req.skillId, expectedType)
            if (!skillDef) {
                throw new SkillException("Failed to find ${req.scopeType} [${req.skillId}] in project [${projectId}]", projectId, req.skillId, ErrorCode.BadParam)
            }
            skillRefId = skillDef.id
        }

        String userId = userInfoService.currentUserId
        long numOpen = helpRequestRepo.countByProjectIdAndUserIdAndStatus(projectId, userId, HelpRequest.Status.OPEN)
        SkillsValidator.isTrue(numOpen < maxOpenPerUserPerProject,
                "You already have [${numOpen}] open questions in this project, the maximum is [${maxOpenPerUserPerProject}]. Please resolve some before asking more.", projectId)

        HelpRequest saved = helpRequestRepo.saveAndFlush(new HelpRequest(
                projectId: projectId,
                userId: userId,
                scopeType: req.scopeType,
                skillRefId: skillRefId,
                title: InputSanitizer.sanitize(req.title.trim()),
                body: InputSanitizer.sanitize(req.body),
        ))
        // same transaction as the question
        UserAttrs userAttrs = userAttrsRepo.findByUserIdIgnoreCase(userId)
        helpNotifier.questionAsked(saved, userAttrs?.userIdForDisplay ?: userId, projDef.name)
        return convertOne(saved)
    }

    @Transactional(readOnly = true)
    List<HelpRequestRes> getMyQuestions(String projectId) {
        projDefAccessor.getProjDef(projectId)
        List<HelpRequest> requests = helpRequestRepo.findAllByProjectIdAndUserIdOrderByCreatedDesc(projectId, userInfoService.currentUserId)
        return convertMany(requests)
    }

    @Transactional(readOnly = true)
    HelpRequestRes getForStudent(String projectId, Integer id) {
        return convertOne(loadAuthorized(projectId, id, true), true)
    }

    @Transactional(readOnly = true)
    HelpRequestRes getForStaff(String projectId, Integer id) {
        return convertOne(loadAuthorized(projectId, id, false), true)
    }

    @Transactional(readOnly = true)
    TableResult getForStaff(String projectId, HelpRequest.Status status, HelpRequest.ScopeType scopeType, String skillId, String userFilter, PageRequest pageRequest) {
        requireStaff(projectId)
        SkillsValidator.isTrue(pageRequest.sort.every { ORDER_BY_OPTIONS.contains(it.property) }, "orderBy must be one of ${ORDER_BY_OPTIONS}", projectId)

        Integer skillRefId = null
        if (StringUtils.isNotBlank(skillId)) {
            SkillDef skillDef = skillDefRepo.findByProjectIdAndSkillIdAndTypeIn(projectId, skillId, [SkillDef.ContainerType.Skill, SkillDef.ContainerType.Subject])
            if (!skillDef) {
                return new TableResult()
            }
            skillRefId = skillDef.id
        }
        Specification<HelpRequest> spec = { root, query, cb ->
            List<Predicate> predicates = [cb.equal(root.get('projectId'), projectId)]
            if (status) {
                predicates << cb.equal(root.get('status'), status)
            }
            if (scopeType) {
                predicates << cb.equal(root.get('scopeType'), scopeType)
            }
            if (skillRefId != null) {
                predicates << cb.equal(root.get('skillRefId'), skillRefId)
            }
            if (StringUtils.isNotBlank(userFilter)) {
                predicates << cb.like(cb.lower(root.get('userId')), "%${userFilter.trim().toLowerCase()}%".toString())
            }
            return cb.and(predicates as Predicate[])
        } as Specification<HelpRequest>
        Page<HelpRequest> page = helpRequestRepo.findAll(spec, pageRequest)
        TableResult res = new TableResult()
        res.totalCount = helpRequestRepo.count({ root, query, cb -> cb.equal(root.get('projectId'), projectId) } as Specification<HelpRequest>)
        res.count = page.totalElements
        res.data = convertMany(page.content)
        return res
    }

    @Transactional(readOnly = true)
    HelpStatsRes getStats(String projectId) {
        requireStaff(projectId)
        return new HelpStatsRes(openCount: helpRequestRepo.countByProjectIdAndStatus(projectId, HelpRequest.Status.OPEN))
    }

    // ---------------------------------------------------------------- replies / resolve / reopen

    @Transactional
    HelpRequestRes reply(String projectId, Integer id, HelpReplyRequest req, boolean studentEndpoint) {
        SkillsValidator.isNotNull(req, "request", projectId)
        SkillsValidator.isNotBlank(req.body, "body", projectId)
        SkillsValidator.isTrue(req.body.length() <= maxBodyLength, "body must not exceed [${maxBodyLength}] characters", projectId)
        validateText(req.body, "body", projectId)
        HelpRequest request = loadAuthorized(projectId, id, studentEndpoint, true)
        requireOpen(request)

        HelpReply.AuthorRole role = actorRole(request, studentEndpoint)
        helpReplyRepo.save(new HelpReply(
                helpRequestId: request.id,
                authorUserId: userInfoService.currentUserId,
                authorRole: role,
                body: InputSanitizer.sanitize(req.body),
        ))
        request.updated = new Date()
        helpRequestRepo.save(request)
        audit(DashboardAction.Reply, request, role)
        return convertOne(request, true)
    }

    @Transactional
    HelpRequestRes resolve(String projectId, Integer id, boolean studentEndpoint) {
        HelpRequest request = loadAuthorized(projectId, id, studentEndpoint, true)
        requireOpen(request)
        HelpReply.AuthorRole role = actorRole(request, studentEndpoint)
        request.status = HelpRequest.Status.RESOLVED
        request.resolvedByUserId = userInfoService.currentUserId
        request.resolvedByRole = role == HelpReply.AuthorRole.STUDENT ? HelpRequest.ResolvedByRole.STUDENT : HelpRequest.ResolvedByRole.STAFF
        request.resolvedOn = new Date()
        helpRequestRepo.save(request)
        helpNotifier.questionResolved(request.id)
        audit(DashboardAction.Resolve, request, role)
        return convertOne(request, true)
    }

    @Transactional
    HelpRequestRes reopen(String projectId, Integer id, boolean studentEndpoint) {
        HelpRequest request = loadAuthorized(projectId, id, studentEndpoint, true)
        if (request.status != HelpRequest.Status.RESOLVED) {
            throw new SkillException("Help request [${id}] is not resolved so it cannot be reopened", projectId, null, ErrorCode.BadParam)
        }
        HelpReply.AuthorRole role = actorRole(request, studentEndpoint)
        SkillsValidator.isTrue(helpRequestRepo.countByProjectIdAndUserIdAndStatus(projectId, request.userId, HelpRequest.Status.OPEN) < maxOpenPerUserPerProject,
                "Reopening would exceed the maximum of [${maxOpenPerUserPerProject}] open questions per user", projectId)
        request.status = HelpRequest.Status.OPEN
        request.resolvedByUserId = null
        request.resolvedByRole = null
        request.resolvedOn = null
        helpRequestRepo.save(request)
        audit(DashboardAction.Reopen, request, role)
        return convertOne(request, true)
    }

    // ---------------------------------------------------------------- helpers

    private void requireOpen(HelpRequest request) {
        if (request.status != HelpRequest.Status.OPEN) {
            throw new SkillException("Help request [${request.id}] is already resolved. Reopen it first.", request.projectId, null, ErrorCode.BadParam)
        }
    }

    /** The role is decided here, never taken from the request. The asker always acts as STUDENT on their own question. */
    private HelpReply.AuthorRole actorRole(HelpRequest request, boolean studentEndpoint) {
        if (request.userId == userInfoService.currentUserId) {
            return HelpReply.AuthorRole.STUDENT
        }
        return requireStaff(request.projectId)
    }

    private void audit(DashboardAction action, HelpRequest request, HelpReply.AuthorRole role) {
        if (role == HelpReply.AuthorRole.STUDENT) {
            return // only staff actions are audited
        }
        userActionsHistoryService.saveUserAction(new UserActionInfo(
                action: action, item: DashboardItem.HelpRequest,
                itemId: request.title, itemRefId: request.id,
                projectId: request.projectId,
                actionAttributes: [askedBy: request.userId, role: role.name()]
        ))
    }

    private void validateText(String text, String attrName, String projectId) {
        CustomValidationResult result = customValidator.validateDescription(new CustomValidator.ValidateDescReq(description: text, projectId: projectId))
        if (!result.valid) {
            throw new SkillException("[${attrName}] is invalid: ${result.msg}", projectId, null, ErrorCode.BadParam)
        }
    }

    private HelpRequestRes convertOne(HelpRequest request, boolean includeReplies = false) {
        return convertMany([request], includeReplies).first()
    }

    private List<HelpRequestRes> convertMany(List<HelpRequest> requests, boolean includeReplies = false) {
        if (!requests) {
            return []
        }
        Map<Integer, List<HelpReply>> repliesByRequest = (requests.id ? helpReplyRepo.findAllByHelpRequestIdInOrderByCreatedAscIdAsc(requests.id) : []).groupBy { it.helpRequestId }
        Set<String> userIds = requests.collect { it.userId } as Set
        userIds.addAll(requests.findAll { it.resolvedByUserId }.collect { it.resolvedByUserId })
        repliesByRequest.values().each { replies -> userIds.addAll(replies.collect { it.authorUserId }) }
        Map<String, String> displayById = userAttrsRepo.findAllByUserIdIn(userIds).collectEntries { [(it.userId): it.userIdForDisplay] }
        Set<Integer> skillRefIds = requests.findAll { it.skillRefId != null }.collect { it.skillRefId } as Set
        Map<Integer, SkillDef> skillsById = skillRefIds ? skillDefRepo.findAllById(skillRefIds).collectEntries { [(it.id): it] } : [:]

        return requests.collect { HelpRequest r ->
            List<HelpReply> replies = repliesByRequest[r.id] ?: []
            HelpReply last = replies ? replies.last() : null
            SkillDef skill = r.skillRefId != null ? skillsById[r.skillRefId] : null
            String awaiting = null
            if (r.status == HelpRequest.Status.OPEN) {
                awaiting = (last && last.authorRole != HelpReply.AuthorRole.STUDENT) ? STUDENT : STAFF
            }
            return new HelpRequestRes(
                    id: r.id,
                    projectId: r.projectId,
                    userId: r.userId,
                    userIdForDisplay: displayById[r.userId] ?: r.userId,
                    scopeType: r.scopeType.name(),
                    skillId: skill?.skillId,
                    skillName: skill?.name,
                    skillDeleted: r.scopeType != HelpRequest.ScopeType.PROJECT && skill == null,
                    title: r.title,
                    body: r.body,
                    status: r.status.name(),
                    resolvedByUserIdForDisplay: r.resolvedByUserId ? (displayById[r.resolvedByUserId] ?: r.resolvedByUserId) : null,
                    resolvedByRole: r.resolvedByRole?.name(),
                    resolvedOn: r.resolvedOn,
                    created: r.created,
                    updated: r.updated,
                    replyCount: replies.size(),
                    lastReplyByRole: last?.authorRole?.name(),
                    lastReplyByUserIdForDisplay: last ? (displayById[last.authorUserId] ?: last.authorUserId) : null,
                    lastReplyOn: last?.created,
                    awaiting: awaiting,
                    replies: includeReplies ? replies.collect { HelpReply reply ->
                        new HelpReplyRes(
                                id: reply.id,
                                authorUserId: reply.authorUserId,
                                authorUserIdForDisplay: displayById[reply.authorUserId] ?: reply.authorUserId,
                                authorRole: reply.authorRole.name(),
                                body: reply.body,
                                created: reply.created)
                    } : null
            )
        }
    }
}
