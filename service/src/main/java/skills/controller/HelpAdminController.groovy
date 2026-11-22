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
package skills.controller

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import skills.controller.exceptions.SkillsValidator
import skills.controller.request.model.HelpReplyRequest
import skills.controller.result.model.HelpRequestRes
import skills.controller.result.model.HelpStatsRes
import skills.controller.result.model.TableResult
import skills.profile.EnableCallStackProf
import skills.services.help.HelpService
import skills.storage.model.HelpRequest

import static org.springframework.data.domain.Sort.Direction.ASC
import static org.springframework.data.domain.Sort.Direction.DESC

/** Staff (admin, approver) help hub endpoints. Secured by the /admin/** rules plus a project staff check in the service. */
@RestController
@RequestMapping("/admin/projects/{projectId}/help")
@Slf4j
@EnableCallStackProf
class HelpAdminController {

    @Autowired
    HelpService helpService

    @GetMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE)
    TableResult getHelpRequests(@PathVariable("projectId") String projectId,
                                @RequestParam int limit,
                                @RequestParam int page,
                                @RequestParam String orderBy,
                                @RequestParam Boolean ascending,
                                @RequestParam(required = false) HelpRequest.Status status,
                                @RequestParam(required = false) HelpRequest.ScopeType scopeType,
                                @RequestParam(required = false, defaultValue = "") String skillId,
                                @RequestParam(required = false, defaultValue = "") String userFilter) {
        PageRequest pageRequest = createPagingRequestWithValidation(projectId, limit, page, orderBy, ascending)
        return helpService.getForStaff(projectId, status, scopeType, skillId, userFilter, pageRequest)
    }

    @GetMapping(value = "/stats", produces = MediaType.APPLICATION_JSON_VALUE)
    HelpStatsRes stats(@PathVariable("projectId") String projectId) {
        return helpService.getStats(projectId)
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    HelpRequestRes get(@PathVariable("projectId") String projectId, @PathVariable("id") Integer id) {
        return helpService.getForStaff(projectId, id)
    }

    @PostMapping(value = "/{id}/replies", produces = MediaType.APPLICATION_JSON_VALUE)
    HelpRequestRes reply(@PathVariable("projectId") String projectId, @PathVariable("id") Integer id, @RequestBody HelpReplyRequest request) {
        return helpService.reply(projectId, id, request, false)
    }

    @PostMapping(value = "/{id}/resolve", produces = MediaType.APPLICATION_JSON_VALUE)
    HelpRequestRes resolve(@PathVariable("projectId") String projectId, @PathVariable("id") Integer id) {
        return helpService.resolve(projectId, id, false)
    }

    @PostMapping(value = "/{id}/reopen", produces = MediaType.APPLICATION_JSON_VALUE)
    HelpRequestRes reopen(@PathVariable("projectId") String projectId, @PathVariable("id") Integer id) {
        return helpService.reopen(projectId, id, false)
    }

    private PageRequest createPagingRequestWithValidation(String projectId, int limit, int page, String orderBy, Boolean ascending) {
        SkillsValidator.isNotBlank(projectId, "Project Id")
        SkillsValidator.isTrue(limit <= 200, "Cannot ask for more than 200 items, provided=[${limit}]", projectId)
        SkillsValidator.isTrue(limit > 0, "Limit must be greater than 0, provided=[${limit}]", projectId)
        SkillsValidator.isTrue(page >= 1, "Page must be 1 or greater (pages are 1-based), provided=[${page}]", projectId)
        SkillsValidator.isTrue(HelpService.ORDER_BY_OPTIONS.contains(orderBy), "orderBy must be one of ${HelpService.ORDER_BY_OPTIONS}, provided=[${orderBy}]", projectId)
        return PageRequest.of(page - 1, limit, ascending ? ASC : DESC, orderBy)
    }
}
