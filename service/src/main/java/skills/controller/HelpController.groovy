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
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import skills.controller.request.model.HelpReplyRequest
import skills.controller.request.model.HelpRequestCreateRequest
import skills.controller.result.model.HelpRequestRes
import skills.profile.EnableCallStackProf
import skills.services.help.HelpService

/** Student-facing help hub endpoints. A student can only ever see and modify their own questions. */
@RestController
@RequestMapping("/api/projects/{projectId}/help")
@Slf4j
@EnableCallStackProf
class HelpController {

    @Autowired
    HelpService helpService

    @PostMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE)
    HelpRequestRes create(@PathVariable("projectId") String projectId, @RequestBody HelpRequestCreateRequest request) {
        return helpService.createQuestion(projectId, request)
    }

    @GetMapping(value = "/mine", produces = MediaType.APPLICATION_JSON_VALUE)
    List<HelpRequestRes> mine(@PathVariable("projectId") String projectId) {
        return helpService.getMyQuestions(projectId)
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    HelpRequestRes get(@PathVariable("projectId") String projectId, @PathVariable("id") Integer id) {
        return helpService.getForStudent(projectId, id)
    }

    @PostMapping(value = "/{id}/replies", produces = MediaType.APPLICATION_JSON_VALUE)
    HelpRequestRes reply(@PathVariable("projectId") String projectId, @PathVariable("id") Integer id, @RequestBody HelpReplyRequest request) {
        return helpService.reply(projectId, id, request, true)
    }

    @PostMapping(value = "/{id}/resolve", produces = MediaType.APPLICATION_JSON_VALUE)
    HelpRequestRes resolve(@PathVariable("projectId") String projectId, @PathVariable("id") Integer id) {
        return helpService.resolve(projectId, id, true)
    }

    @PostMapping(value = "/{id}/reopen", produces = MediaType.APPLICATION_JSON_VALUE)
    HelpRequestRes reopen(@PathVariable("projectId") String projectId, @PathVariable("id") Integer id) {
        return helpService.reopen(projectId, id, true)
    }
}
