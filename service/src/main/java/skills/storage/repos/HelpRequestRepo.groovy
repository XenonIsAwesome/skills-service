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
package skills.storage.repos

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import skills.storage.model.HelpRequest

interface HelpRequestRepo extends JpaRepository<HelpRequest, Integer>, JpaSpecificationExecutor<HelpRequest> {

    List<HelpRequest> findAllByProjectIdAndUserIdOrderByCreatedDesc(String projectId, String userId)

    long countByProjectIdAndStatus(String projectId, HelpRequest.Status status)

    long countByProjectIdAndUserIdAndStatus(String projectId, String userId, HelpRequest.Status status)

    // row lock so that concurrent reply/resolve/reopen on the same thread are serialized and the status check is reliable
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query('select h from HelpRequest h where h.id = ?1')
    HelpRequest findByIdForUpdate(Integer id)
}
