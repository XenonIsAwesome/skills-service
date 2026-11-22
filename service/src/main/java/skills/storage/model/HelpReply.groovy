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
import org.springframework.data.jpa.domain.support.AuditingEntityListener

@Entity
@Table(name = "help_reply")
@ToString(includeNames = true)
@EntityListeners(AuditingEntityListener)
class HelpReply {

    static enum AuthorRole {
        STUDENT, ADMIN, APPROVER
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id

    Integer helpRequestId

    String authorUserId

    @Enumerated(EnumType.STRING)
    AuthorRole authorRole

    String body

    // reserved for publishing Q&As (not used in the MVP)
    Boolean published = false

    @CreatedDate
    Date created

    @Temporal(TemporalType.TIMESTAMP)
    Date editedOn
}
