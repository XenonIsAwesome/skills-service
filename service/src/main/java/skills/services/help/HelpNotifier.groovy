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
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import skills.services.AccessSettingsStorageService
import skills.storage.model.HelpRequest
import skills.storage.model.WebNotification
import skills.storage.model.WebNotificationAck
import skills.storage.repos.WebNotificationsAckRepo
import skills.storage.repos.WebNotificationsRepo

/**
 * Single place for everything that notifies people about help requests. The MVP only creates in-app
 * notifications for project admins on question creation; email and reply notifications can be added here later.
 */
@Component
@Slf4j
class HelpNotifier {

    @Autowired
    AccessSettingsStorageService accessSettingsStorageService

    @Autowired
    WebNotificationsRepo webNotificationsRepo

    @Autowired
    WebNotificationsAckRepo webNotificationsAckRepo

    static String lookupId(Integer helpRequestId) {
        return "help:${helpRequestId}"
    }

    static String adminLink(String projectId, Integer helpRequestId) {
        return "/administrator/projects/${URLEncoder.encode(projectId, 'UTF-8')}/help/${helpRequestId}"
    }

    /** Notifies all project admins (not approvers) in-app. Joins the caller's transaction. */
    @Transactional
    void questionAsked(HelpRequest helpRequest, String askedByDisplay, String projectName) {
        List<String> admins = accessSettingsStorageService.getProjectAdminIds(helpRequest.projectId)
        // do not notify the asker about their own question
        admins.findAll { it != helpRequest.userId }.unique().each { String adminUserId ->
            webNotificationsRepo.save(new WebNotification(
                    userId: adminUserId,
                    lookupId: lookupId(helpRequest.id),
                    notifiedOn: new Date(),
                    showUntil: null,
                    title: "New help request in ${projectName}",
                    notification: "${askedByDisplay} asked: ${helpRequest.title}",
                    link: adminLink(helpRequest.projectId, helpRequest.id),
            ))
        }
    }

    /** Dismisses the pending notifications of this question for every admin that has one. */
    @Transactional
    void questionResolved(Integer helpRequestId) {
        webNotificationsRepo.findAllByLookupId(lookupId(helpRequestId)).each { WebNotification notif ->
            if (notif.userId && !webNotificationsAckRepo.findAllByUserIdAndWebNotificationsRefId(notif.userId, notif.id)) {
                webNotificationsAckRepo.save(new WebNotificationAck(userId: notif.userId, webNotificationsRefId: notif.id))
            }
        }
    }
}
