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

import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService
import skills.storage.model.auth.RoleName

class HelpHubNotificationsSpecs extends DefaultIntSpec {

    String projId
    SkillsService owner
    SkillsService admin2
    SkillsService approver
    SkillsService student

    def setup() {
        def proj = SkillsFactory.createProject(1)
        projId = proj.projectId
        skillsService.createProject(proj)
        List<String> users = getRandomUsers(3)
        admin2 = createService(users[0])
        approver = createService(users[1])
        student = createService(users[2])
        owner = skillsService
        owner.addUserRole(admin2.userName, projId, RoleName.ROLE_PROJECT_ADMIN.toString())
        owner.addUserRole(approver.userName, projId, RoleName.ROLE_PROJECT_APPROVER.toString())
        // ignore notifications that exist for reasons unrelated to this test
        [owner, admin2, approver].each { it.dismissAllWebNotifications() }
    }

    def "admins are notified when a question is asked, approvers and the asker are not"() {
        when:
        def q = student.createHelpRequest(projId, [scopeType: 'PROJECT', title: 'Need help', body: 'please'])
        def ownerNotifs = owner.getWebNotifications()
        def admin2Notifs = admin2.getWebNotifications()

        then:
        ownerNotifs.size() == 1
        admin2Notifs.size() == 1
        ownerNotifs[0].link == "/administrator/projects/${projId}/help/${q.id}".toString()
        ownerNotifs[0].title
        ownerNotifs[0].notification.contains('Need help')
        approver.getWebNotifications().isEmpty()
        student.getWebNotifications().isEmpty()
    }

    def "an admin asking a question does not notify themselves but notifies other admins"() {
        when:
        owner.createHelpRequest(projId, [scopeType: 'PROJECT', title: 'admin q', body: 'b'])

        then:
        owner.getWebNotifications().isEmpty()
        admin2.getWebNotifications().size() == 1
    }

    def "resolving auto-dismisses pending notifications for every admin"() {
        def q = student.createHelpRequest(projId, [scopeType: 'PROJECT', title: 'Need help', body: 'please'])
        assert owner.getWebNotifications().size() == 1
        assert admin2.getWebNotifications().size() == 1

        when: 'resolved by the student'
        student.resolveHelpRequest(projId, q.id)

        then:
        owner.getWebNotifications().isEmpty()
        admin2.getWebNotifications().isEmpty()
    }

    def "resolving by staff also dismisses for all admins and one admin dismissing does not affect another"() {
        def q = student.createHelpRequest(projId, [scopeType: 'PROJECT', title: 'Need help', body: 'please'])
        owner.dismissWebNotification(owner.getWebNotifications()[0].id)
        assert owner.getWebNotifications().isEmpty()
        assert admin2.getWebNotifications().size() == 1

        when:
        approver.resolveHelpRequestAsStaff(projId, q.id)

        then:
        admin2.getWebNotifications().isEmpty()
    }

    def "replies and reopen do not create notifications in the MVP"() {
        def q = student.createHelpRequest(projId, [scopeType: 'PROJECT', title: 'Need help', body: 'please'])
        [owner, admin2].each { it.dismissAllWebNotifications() }

        when:
        approver.replyToHelpRequestAsStaff(projId, q.id, 'answer')
        student.replyToHelpRequest(projId, q.id, 'thanks')
        student.resolveHelpRequest(projId, q.id)
        student.reopenHelpRequest(projId, q.id)

        then:
        owner.getWebNotifications().isEmpty()
        student.getWebNotifications().isEmpty()
    }

    def "web notifications still return only the current user's and existing notifications have no link"() {
        when:
        student.createHelpRequest(projId, [scopeType: 'PROJECT', title: 'Need help', body: 'please'])
        def other = createService(getRandomUsers(5)[4])

        then:
        other.getWebNotifications().every { it.link == null }
        !other.getWebNotifications().any { it.title?.contains('help request') }
    }
}
