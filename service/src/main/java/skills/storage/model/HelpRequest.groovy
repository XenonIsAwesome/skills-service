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
package skills.storage.model

import groovy.transform.ToString
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener

@Entity
@Table(name = "help_request")
@ToString(includeNames = true)
@EntityListeners(AuditingEntityListener)
class HelpRequest {

    static enum ScopeType {
        PROJECT, SUBJECT, SKILL
    }

    static enum Status {
        OPEN, RESOLVED
    }

    static enum ResolvedByRole {
        STUDENT, STAFF
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id

    String projectId

    String userId

    @Enumerated(EnumType.STRING)
    ScopeType scopeType

    // null when scopeType is PROJECT or when the referenced skill/subject was deleted
    Integer skillRefId

    String title

    String body

    @Enumerated(EnumType.STRING)
    Status status = Status.OPEN

    String resolvedByUserId

    @Enumerated(EnumType.STRING)
    ResolvedByRole resolvedByRole

    @Temporal(TemporalType.TIMESTAMP)
    Date resolvedOn

    // reserved for publishing Q&As (not used in the MVP)
    @Temporal(TemporalType.TIMESTAMP)
    Date publishedOn
    String publishedBy
    String publishedTitle
    String publishedBody

    @CreatedDate
    Date created

    @LastModifiedDate
    Date updated
}
