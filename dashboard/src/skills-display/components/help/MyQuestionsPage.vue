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
import { useRouter } from 'vue-router'
import Tag from 'primevue/tag'
import SkillsTitle from '@/skills-display/components/utilities/SkillsTitle.vue'
import SkillsSpinner from '@/components/utils/SkillsSpinner.vue'
import AskForHelpButton from '@/skills-display/components/help/AskForHelpButton.vue'
import { useHelpService } from '@/skills-display/services/UseHelpService.js'
import { useSkillsDisplayInfo } from '@/skills-display/UseSkillsDisplayInfo.js'
import { useSkillsDisplayAttributesState } from '@/skills-display/stores/UseSkillsDisplayAttributesState.js'
import { useTimeUtils } from '@/common-components/utilities/UseTimeUtils.js'
import { awaitingLabel, scopeLabel } from '@/common-components/help/HelpUtils.js'

const helpService = useHelpService()
const router = useRouter()
const skillsDisplayInfo = useSkillsDisplayInfo()
const attributes = useSkillsDisplayAttributesState()
const timeUtils = useTimeUtils()
const questions = ref([])
const loading = ref(true)

onMounted(() => {
  helpService.getMyQuestions().then((res) => { questions.value = res }).finally(() => { loading.value = false })
})
const open = (q) => router.push({
  name: skillsDisplayInfo.getContextSpecificRouteName('myHelpQuestion'),
  params: { projectId: attributes.projectId, id: q.id },
})
</script>

<template>
  <div>
    <SkillsTitle>My Questions</SkillsTitle>
    <div class="mt-4 flex">
      <AskForHelpButton />
    </div>
    <SkillsSpinner :is-loading="loading" />
    <div v-if="!loading" class="mt-4" data-cy="myQuestions">
      <Card v-if="questions.length === 0">
        <template #content><div data-cy="noQuestions">You have not asked any questions yet.</div></template>
      </Card>
      <Card v-for="q in questions" :key="q.id" class="mb-2 cursor-pointer" :data-cy="`question-${q.id}`">
        <template #content>
          <div class="flex gap-2 items-center flex-wrap">
            <a href="#" class="flex-1 font-semibold" @click.prevent="open(q)" data-cy="questionLink">{{ q.title }}</a>
            <Tag v-if="q.status === 'RESOLVED'" severity="success" value="Resolved" />
            <Tag v-else severity="warn" :value="awaitingLabel(q)" />
          </div>
          <div class="text-sm text-muted-color mt-1">{{ scopeLabel(q) }} &middot; {{ timeUtils.formatDate(q.created) }} &middot; {{ q.replyCount }} replies</div>
        </template>
      </Card>
    </div>
  </div>
</template>
