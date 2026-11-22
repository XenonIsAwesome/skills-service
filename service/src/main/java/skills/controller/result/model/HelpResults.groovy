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
package skills.controller.result.model

import groovy.transform.Canonical

@Canonical
class HelpReplyRes {
    Integer id
    String authorUserId
    String authorUserIdForDisplay
    String authorRole
    String body
    Date created
}

@Canonical
class HelpRequestRes {
    Integer id
    String projectId
    String userId
    String userIdForDisplay
    String scopeType
    String skillId
    String skillName
    // true when the scope is SKILL/SUBJECT but the referenced skill/subject has since been deleted
    boolean skillDeleted
    String title
    String body
    String status
    String resolvedByUserIdForDisplay
    String resolvedByRole
    Date resolvedOn
    Date created
    Date updated
    int replyCount
    String lastReplyByRole
    String lastReplyByUserIdForDisplay
    Date lastReplyOn
    // who needs to act next: STAFF or STUDENT, null when resolved
    String awaiting
    // only populated by the single-item endpoints
    List<HelpReplyRes> replies
}

@Canonical
class HelpStatsRes {
    long openCount
}
