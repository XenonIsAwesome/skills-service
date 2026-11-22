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
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useSkillsDisplayInfo } from '@/skills-display/UseSkillsDisplayInfo.js'
import { useSkillsDisplayAttributesState } from '@/skills-display/stores/UseSkillsDisplayAttributesState.js'
import AskForHelpModal from '@/skills-display/components/help/AskForHelpModal.vue'

const props = defineProps({
  scopeType: { type: String, default: 'PROJECT' },
  skillId: { type: String, default: null },
  scopeName: { type: String, default: null },
  showMyQuestions: { type: Boolean, default: false },
})
const router = useRouter()
const skillsDisplayInfo = useSkillsDisplayInfo()
const attributes = useSkillsDisplayAttributesState()
const show = ref(false)

const goToThread = (created) => {
  show.value = false
  router.push({
    name: skillsDisplayInfo.getContextSpecificRouteName('myHelpQuestion'),
    params: { projectId: attributes.projectId, id: created.id },
  })
}
const goToMine = () => {
  router.push({ name: skillsDisplayInfo.getContextSpecificRouteName('myHelpQuestions'), params: { projectId: attributes.projectId } })
}
</script>

<template>
  <div class="flex gap-2 flex-wrap" data-cy="askForHelp">
    <Button label="Ask for help" icon="fas fa-question-circle" outlined size="small"
            data-cy="askForHelpBtn" @click="show = true" />
    <Button v-if="showMyQuestions" label="My questions" icon="fas fa-comments" text size="small"
            data-cy="myQuestionsBtn" @click="goToMine" />
    <AskForHelpModal v-if="show" v-model="show" :scope-type="scopeType" :skill-id="skillId" :scope-name="scopeName"
                     @created="goToThread" @done="show = false" />
  </div>
</template>
