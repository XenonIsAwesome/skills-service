/*
Copyright 2026 SkillTree

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Tag from 'primevue/tag'
import SubPageHeader from '@/components/utils/pages/SubPageHeader.vue'
import SkillsDataTable from '@/components/utils/table/SkillsDataTable.vue'
import DateCell from '@/components/utils/table/DateCell.vue'
import AdminHelpService from '@/components/help/AdminHelpService.js'
import { useHelpStatsState } from '@/stores/UseHelpStatsState.js'
import { awaitingLabel, roleLabel } from '@/common-components/help/HelpUtils.js'

const route = useRoute()
const router = useRouter()
const stats = useHelpStatsState()
const projectId = route.params.projectId

const items = ref([])
const loading = ref(true)
const totalRows = ref(0)
const pageSize = ref(10)
const currentPage = ref(1)
const sortBy = ref('created')
const sortOrder = ref(-1)
const statusFilter = ref('OPEN')
const scopeFilter = ref(null)
const userFilter = ref('')

const statusOptions = [{ label: 'All', value: null }, { label: 'Open', value: 'OPEN' }, { label: 'Resolved', value: 'RESOLVED' }]
const scopeOptions = [{ label: 'All', value: null }, { label: 'Project', value: 'PROJECT' }, { label: 'Subject', value: 'SUBJECT' }, { label: 'Skill', value: 'SKILL' }]

const load = () => {
  loading.value = true
  const params = {
    limit: pageSize.value,
    page: currentPage.value,
    orderBy: sortBy.value,
    ascending: sortOrder.value === 1,
    userFilter: userFilter.value,
  }
  if (statusFilter.value) params.status = statusFilter.value
  if (scopeFilter.value) params.scopeType = scopeFilter.value
  AdminHelpService.getHelpRequests(projectId, params)
    .then((res) => { items.value = res.data; totalRows.value = res.count })
    .finally(() => { loading.value = false })
  stats.loadStats(projectId)
}
onMounted(load)

const pageChanged = (e) => { currentPage.value = e.page + 1; pageSize.value = e.rows; load() }
const sortTable = (e) => { sortBy.value = e.sortField; sortOrder.value = e.sortOrder; currentPage.value = 1; load() }
const filterChanged = () => { currentPage.value = 1; load() }
const open = (item) => router.push({ name: 'ProjectHelpThread', params: { projectId, helpId: item.id } })
const skillLink = (item) => {
  if (item.scopeType === 'SKILL') return { name: 'SkillOverview', params: { projectId, subjectId: undefined, skillId: item.skillId } }
  return { name: 'SubjectSkills', params: { projectId, subjectId: item.skillId } }
}
</script>

<template>
  <div>
    <SubPageHeader title="Help" />
    <Card>
      <template #content>
        <div class="flex gap-3 flex-wrap mb-3 items-end">
          <div class="flex flex-col gap-1">
            <label for="helpStatusFilter" class="text-sm">Status</label>
            <Select input-id="helpStatusFilter" v-model="statusFilter" :options="statusOptions" option-label="label" option-value="value"
                    data-cy="helpStatusFilter" @change="filterChanged" />
          </div>
          <div class="flex flex-col gap-1">
            <label for="helpScopeFilter" class="text-sm">Scope</label>
            <Select input-id="helpScopeFilter" v-model="scopeFilter" :options="scopeOptions" option-label="label" option-value="value"
                    data-cy="helpScopeFilter" @change="filterChanged" />
          </div>
          <div class="flex flex-col gap-1">
            <label for="helpUserFilter" class="text-sm">User</label>
            <InputText id="helpUserFilter" v-model="userFilter" placeholder="Filter by user" data-cy="helpUserFilter" @keyup.enter="filterChanged" @blur="filterChanged" />
          </div>
        </div>
        <SkillsDataTable :value="items" :loading="loading" data-cy="helpTable" paginator lazy
                         :total-records="totalRows" :rows="pageSize" :rows-per-page-options="[10, 25, 50]"
                         :sort-field="sortBy" :sort-order="sortOrder" @page="pageChanged" @sort="sortTable">
          <template #empty><div data-cy="noHelpRequests">No help requests found.</div></template>
          <Column field="title" header="Question">
            <template #body="{ data }">
              <a href="#" class="font-semibold" data-cy="helpTitleLink" @click.prevent="open(data)">{{ data.title }}</a>
              <div class="text-sm text-muted-color">
                <span v-if="data.lastReplyByUserIdForDisplay">Last reply by {{ data.lastReplyByUserIdForDisplay }} ({{ roleLabel(data.lastReplyByRole) }})</span>
                <span v-else>No replies yet</span>
              </div>
            </template>
          </Column>
          <Column field="userId" header="Asked By" sortable>
            <template #body="{ data }">{{ data.userIdForDisplay }}</template>
          </Column>
          <Column header="Scope">
            <template #body="{ data }">
              <span v-if="data.scopeType === 'PROJECT'">Project</span>
              <span v-else-if="data.skillDeleted">{{ data.scopeType === 'SKILL' ? 'Skill' : 'Subject' }}: (deleted)</span>
              <router-link v-else :to="skillLink(data)">{{ data.scopeType === 'SKILL' ? 'Skill' : 'Subject' }}: {{ data.skillName }}</router-link>
            </template>
          </Column>
          <Column field="status" header="Status" sortable>
            <template #body="{ data }">
              <Tag v-if="data.status === 'RESOLVED'" severity="success" value="Resolved" />
              <Tag v-else severity="warn" :value="awaitingLabel(data)" />
            </template>
          </Column>
          <Column field="created" header="Asked" sortable>
            <template #body="{ data }"><DateCell :value="data.created" /></template>
          </Column>
        </SkillsDataTable>
      </template>
    </Card>
  </div>
</template>
