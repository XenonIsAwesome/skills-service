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
import { useRoute } from 'vue-router'
import SubPageHeader from '@/components/utils/pages/SubPageHeader.vue'
import SkillsSpinner from '@/components/utils/SkillsSpinner.vue'
import HelpThread from '@/common-components/help/HelpThread.vue'
import AdminHelpService from '@/components/help/AdminHelpService.js'
import { useHelpStatsState } from '@/stores/UseHelpStatsState.js'

const route = useRoute()
const stats = useHelpStatsState()
const projectId = route.params.projectId
const thread = ref(null)
const loading = ref(true)
const busy = ref(false)

onMounted(() => {
  AdminHelpService.getHelpRequest(projectId, route.params.helpId).then((res) => { thread.value = res }).finally(() => { loading.value = false })
})
const run = (fn) => {
  busy.value = true
  return fn().then((res) => { thread.value = res }).then(() => stats.loadStats(projectId)).finally(() => { busy.value = false })
}
</script>

<template>
  <div>
    <SubPageHeader title="Help Request" />
    <SkillsSpinner :is-loading="loading" />
    <div v-if="thread" class="mt-4">
      <HelpThread :thread="thread" :busy="busy" staff-view
                  @reply="(body) => run(() => AdminHelpService.reply(projectId, thread.id, body))"
                  @resolve="run(() => AdminHelpService.resolve(projectId, thread.id))"
                  @reopen="run(() => AdminHelpService.reopen(projectId, thread.id))" />
    </div>
  </div>
</template>
