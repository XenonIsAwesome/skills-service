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
package skills.intTests.help

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService
import skills.storage.model.auth.RoleName
import skills.storage.repos.UserActionsHistoryRepo

class HelpHubSpecs extends DefaultIntSpec {

    @Autowired
    UserActionsHistoryRepo userActionsHistoryRepo

    String projId
    SkillsService owner
    SkillsService admin2
    SkillsService approver
    SkillsService student
    SkillsService otherStudent

    def setup() {
        def proj = SkillsFactory.createProject(1)
        def subj = SkillsFactory.createSubject(1, 1)
        def skills = SkillsFactory.createSkills(2, 1, 1)
        projId = proj.projectId
        skillsService.createProjectAndSubjectAndSkills(proj, subj, skills)

        List<String> users = getRandomUsers(4)
        admin2 = createService(users[0])
        approver = createService(users[1])
        student = createService(users[2])
        otherStudent = createService(users[3])
        owner = skillsService
        owner.addUserRole(admin2.userName, projId, RoleName.ROLE_PROJECT_ADMIN.toString())
        owner.addUserRole(approver.userName, projId, RoleName.ROLE_PROJECT_APPROVER.toString())
    }

    private Map ask(Map overrides = [:]) {
        return [scopeType: 'PROJECT', title: 'How do I start?', body: 'I am lost', *: overrides]
    }

    def "student creates a question and reads it back"() {
        when:
        def created = student.createHelpRequest(projId, ask())
        def mine = student.getMyHelpRequests(projId)
        def one = student.getHelpRequest(projId, created.id)

        then:
        created.status == 'OPEN'
        created.scopeType == 'PROJECT'
        created.awaiting == 'STAFF'
        mine.collect { it.id } == [created.id]
        one.title == 'How do I start?'
        one.replies == []
    }

    def "scope validation: skill and subject must exist and match the scope type"() {
        when:
        def subj = student.createHelpRequest(projId, ask(scopeType: 'SUBJECT', skillId: 'TestSubject1'))
        def skill = student.createHelpRequest(projId, ask(scopeType: 'SKILL', skillId: 'skill1'))

        then:
        subj.skillName
        subj.scopeType == 'SUBJECT'
        skill.skillId == 'skill1'
        skill.skillName

        when:
        student.createHelpRequest(projId, ask(scopeType: 'SKILL', skillId: 'TestSubject1'))
        then:
        thrown(SkillsClientException)

        when:
        student.createHelpRequest(projId, ask(scopeType: 'SUBJECT', skillId: 'skill1'))
        then:
        thrown(SkillsClientException)

        when:
        student.createHelpRequest(projId, ask(scopeType: 'SKILL', skillId: 'doesNotExist'))
        then:
        thrown(SkillsClientException)

        when:
        student.createHelpRequest(projId, ask(scopeType: 'SKILL'))
        then:
        thrown(SkillsClientException)

        when:
        student.createHelpRequest(projId, ask(scopeType: 'PROJECT', skillId: 'skill1'))
        then:
        thrown(SkillsClientException)
    }

    def "title and body are required and length limited"() {
        when:
        student.createHelpRequest(projId, ask(title: ''))
        then:
        thrown(SkillsClientException)

        when:
        student.createHelpRequest(projId, ask(body: ' '))
        then:
        thrown(SkillsClientException)

        when:
        student.createHelpRequest(projId, ask(title: 'a' * 101))
        then:
        thrown(SkillsClientException)

        when:
        student.createHelpRequest(projId, ask(body: 'a' * 3001))
        then:
        thrown(SkillsClientException)
    }

    def "a student cannot read or modify another student's question"() {
        def created = student.createHelpRequest(projId, ask())

        when:
        otherStudent.getHelpRequest(projId, created.id)
        then:
        thrown(SkillsClientException)

        when:
        otherStudent.replyToHelpRequest(projId, created.id, 'hi')
        then:
        thrown(SkillsClientException)

        when:
        otherStudent.resolveHelpRequest(projId, created.id)
        then:
        thrown(SkillsClientException)

        when:
        student.resolveHelpRequest(projId, created.id)
        otherStudent.reopenHelpRequest(projId, created.id)
        then:
        thrown(SkillsClientException)

        expect:
        otherStudent.getMyHelpRequests(projId).isEmpty()
        student.getHelpRequest(projId, created.id).status == 'RESOLVED'
    }

    def "a question id must belong to the project in the path"() {
        def proj2 = SkillsFactory.createProject(2)
        skillsService.createProject(proj2)
        def created = student.createHelpRequest(projId, ask())

        when:
        student.getHelpRequest(proj2.projectId, created.id)
        then:
        thrown(SkillsClientException)

        when:
        owner.getHelpRequestAsStaff(proj2.projectId, created.id)
        then:
        thrown(SkillsClientException)
    }

    def "a student cannot call the staff endpoints"() {
        def created = student.createHelpRequest(projId, ask())

        when:
        student.getHelpRequestsAsStaff(projId)
        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.httpStatus == HttpStatus.FORBIDDEN

        when:
        student.getHelpRequestAsStaff(projId, created.id)
        then:
        e = thrown(SkillsClientException)
        e.httpStatus == HttpStatus.FORBIDDEN

        when:
        student.replyToHelpRequestAsStaff(projId, created.id, 'x')
        then:
        e = thrown(SkillsClientException)
        e.httpStatus == HttpStatus.FORBIDDEN

        when:
        student.getHelpStatsAsStaff(projId)
        then:
        e = thrown(SkillsClientException)
        e.httpStatus == HttpStatus.FORBIDDEN
    }

    def "admin of another project cannot use the staff endpoints"() {
        def proj2 = SkillsFactory.createProject(2)
        otherStudent.createProject(proj2)
        def created = student.createHelpRequest(projId, ask())

        when:
        otherStudent.getHelpRequestAsStaff(projId, created.id)
        then:
        thrown(SkillsClientException)
    }

    def "approver and admin can list, read, reply, resolve and reopen"() {
        def q1 = student.createHelpRequest(projId, ask(title: 'q1'))
        def q2 = otherStudent.createHelpRequest(projId, ask(title: 'q2', scopeType: 'SKILL', skillId: 'skill1'))

        when:
        def list = approver.getHelpRequestsAsStaff(projId)
        def stats = approver.getHelpStatsAsStaff(projId)
        def read = approver.getHelpRequestAsStaff(projId, q1.id)
        def replied = approver.replyToHelpRequestAsStaff(projId, q1.id, 'try this')
        def adminReply = admin2.replyToHelpRequestAsStaff(projId, q1.id, 'or this')

        then:
        list.totalCount == 2
        list.data.collect { it.title }.sort() == ['q1', 'q2']
        stats.openCount == 2
        read.title == 'q1'
        replied.awaiting == 'STUDENT'
        replied.replies.authorRole == ['APPROVER']
        adminReply.replies.authorRole == ['APPROVER', 'ADMIN']
        adminReply.lastReplyByRole == 'ADMIN'

        when: 'student replies the ball is back with staff'
        def studentReply = student.replyToHelpRequest(projId, q1.id, 'thanks, but still stuck')

        then:
        studentReply.awaiting == 'STAFF'
        studentReply.replies.authorRole == ['APPROVER', 'ADMIN', 'STUDENT']

        when:
        def resolved = approver.resolveHelpRequestAsStaff(projId, q1.id)
        then:
        resolved.status == 'RESOLVED'
        resolved.resolvedByRole == 'STAFF'
        resolved.resolvedOn
        resolved.awaiting == null
        approver.getHelpStatsAsStaff(projId).openCount == 1

        when:
        def reopened = admin2.reopenHelpRequestAsStaff(projId, q1.id)
        then:
        reopened.status == 'OPEN'
        reopened.resolvedByRole == null
        approver.getHelpStatsAsStaff(projId).openCount == 2
    }

    def "staff list filters"() {
        student.createHelpRequest(projId, ask(title: 'p'))
        student.createHelpRequest(projId, ask(title: 's', scopeType: 'SKILL', skillId: 'skill1'))
        def sub = otherStudent.createHelpRequest(projId, ask(title: 'sub', scopeType: 'SUBJECT', skillId: 'TestSubject1'))
        otherStudent.resolveHelpRequest(projId, sub.id)

        expect:
        owner.getHelpRequestsAsStaff(projId, [status: 'OPEN']).data.collect { it.title }.sort() == ['p', 's']
        owner.getHelpRequestsAsStaff(projId, [status: 'RESOLVED']).data.collect { it.title } == ['sub']
        owner.getHelpRequestsAsStaff(projId, [scopeType: 'SKILL']).data.collect { it.title } == ['s']
        owner.getHelpRequestsAsStaff(projId, [skillId: 'TestSubject1']).data.collect { it.title } == ['sub']
        owner.getHelpRequestsAsStaff(projId, [userFilter: student.userName]).data.collect { it.title }.sort() == ['p', 's']
        owner.getHelpRequestsAsStaff(projId, [:], 1, 2).data.size() == 1
        owner.getHelpRequestsAsStaff(projId, [:], 1, 2).totalCount == 3
    }

    def "staff list validates paging params"() {
        when:
        owner.getHelpRequestsAsStaff(projId, [:], 201)
        then:
        thrown(SkillsClientException)

        when:
        owner.getHelpRequestsAsStaff(projId, [:], 10, 0)
        then:
        thrown(SkillsClientException)

        when:
        owner.getHelpRequestsAsStaff(projId, [:], 10, 1, 'password')
        then:
        thrown(SkillsClientException)
    }

    def "student can resolve and reopen own question, resolved thread rejects replies"() {
        def q = student.createHelpRequest(projId, ask())

        when:
        def resolved = student.resolveHelpRequest(projId, q.id)
        then:
        resolved.status == 'RESOLVED'
        resolved.resolvedByRole == 'STUDENT'
        resolved.resolvedByUserIdForDisplay

        when:
        student.replyToHelpRequest(projId, q.id, 'more')
        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.message.contains('already resolved')

        when:
        owner.replyToHelpRequestAsStaff(projId, q.id, 'more')
        then:
        e = thrown(SkillsClientException)
        e.message.contains('already resolved')

        when:
        student.resolveHelpRequest(projId, q.id)
        then:
        e = thrown(SkillsClientException)
        e.message.contains('already resolved')

        when:
        def reopened = student.reopenHelpRequest(projId, q.id)
        student.replyToHelpRequest(projId, q.id, 'more')
        then:
        reopened.status == 'OPEN'
        student.getHelpRequest(projId, q.id).replies.size() == 1

        when:
        student.reopenHelpRequest(projId, q.id)
        then:
        thrown(SkillsClientException)
    }

    def "open question cap per user per project"() {
        10.times { student.createHelpRequest(projId, ask(title: "q${it}".toString())) }

        when:
        student.createHelpRequest(projId, ask())
        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.message.contains('maximum')

        when: 'resolving one frees a slot, another user is unaffected'
        student.resolveHelpRequest(projId, student.getMyHelpRequests(projId)[0].id)
        student.createHelpRequest(projId, ask())
        otherStudent.createHelpRequest(projId, ask())
        then:
        notThrown(SkillsClientException)
    }

    def "a staff member replying to own question is recorded as the student"() {
        def q = approver.createHelpRequest(projId, ask())

        when:
        def res = approver.replyToHelpRequest(projId, q.id, 'self reply')
        def resolved = approver.resolveHelpRequest(projId, q.id)

        then:
        res.replies.authorRole == ['STUDENT']
        resolved.resolvedByRole == 'STUDENT'
    }

    def "an approver cannot read other students' questions via the student endpoints"() {
        def q = student.createHelpRequest(projId, ask())

        when:
        approver.getHelpRequest(projId, q.id)
        then:
        thrown(SkillsClientException)
    }

    def "a reply cannot choose its own author role"() {
        def q = student.createHelpRequest(projId, ask())

        when:
        student.wsHelper.apiPost("/projects/${projId}/help/${q.id}/replies".toString(), [body: 'sneaky', authorRole: 'ADMIN'])
        def detail = student.getHelpRequest(projId, q.id)

        then:
        detail.replies.authorRole == ['STUDENT']
    }

    def "markdown body is sanitized"() {
        when:
        def q = student.createHelpRequest(projId, ask(body: 'hello <script>alert(1)</script> world'))
        then:
        !q.body.toString().contains('<script>')
    }

    def "deleted skill is reported as such"() {
        def q = student.createHelpRequest(projId, ask(scopeType: 'SKILL', skillId: 'skill1'))

        when:
        owner.deleteSkill([projectId: projId, subjectId: 'TestSubject1', skillId: 'skill1'])
        def res = student.getHelpRequest(projId, q.id)

        then:
        res.scopeType == 'SKILL'
        res.skillDeleted
        res.skillId == null
    }

    def "staff actions are audited, student actions are not"() {
        def q = student.createHelpRequest(projId, ask())

        when:
        student.replyToHelpRequest(projId, q.id, 'student reply')
        approver.replyToHelpRequestAsStaff(projId, q.id, 'staff reply')
        approver.resolveHelpRequestAsStaff(projId, q.id)
        admin2.reopenHelpRequestAsStaff(projId, q.id)
        def history = userActionsHistoryRepo.findAll().findAll { it.item.name() == 'HelpRequest' }

        then:
        history.collect { it.action.name() }.sort() == ['Reopen', 'Reply', 'Resolve']
    }

    def "unknown project is rejected"() {
        when:
        student.createHelpRequest('doesNotExist', ask())
        then:
        thrown(SkillsClientException)
    }
}
