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
import { object, string } from 'yup'
import SkillsInputFormDialog from '@/components/utils/inputForm/SkillsInputFormDialog.vue'
import SkillsTextInput from '@/components/utils/inputForm/SkillsTextInput.vue'
import MarkdownEditor from '@/common-components/utilities/markdown/MarkdownEditor.vue'
import { useHelpService } from '@/skills-display/services/UseHelpService.js'
import { HELP_MAX_BODY_LENGTH, HELP_MAX_TITLE_LENGTH } from '@/common-components/help/HelpUtils.js'

const model = defineModel()
const props = defineProps({
  scopeType: { type: String, default: 'PROJECT' },
  skillId: { type: String, default: null },
  scopeName: { type: String, default: null },
})
const emit = defineEmits(['created', 'done'])
const helpService = useHelpService()

const initialData = { title: '', body: '' }
const schema = object({
  title: string().trim().required().max(HELP_MAX_TITLE_LENGTH).customDescriptionValidator('Title').label('Title'),
  body: string().trim().required().max(HELP_MAX_BODY_LENGTH).customDescriptionValidator('Question').label('Question'),
})

const save = (values) => {
  return helpService.createQuestion({
    scopeType: props.scopeType,
    skillId: props.scopeType === 'PROJECT' ? null : props.skillId,
    title: values.title,
    body: values.body,
  }).then((created) => {
    emit('created', created)
  })
}
const scopeText = () => {
  if (props.scopeType === 'PROJECT') return 'this project'
  return `${props.scopeType === 'SUBJECT' ? 'subject' : 'skill'} ${props.scopeName || props.skillId}`
}
</script>

<template>
  <SkillsInputFormDialog v-model="model"
                         id="askForHelpDialog"
                         header="Ask for Help"
                         :maximizable="false"
                         :enable-return-focus="true"
                         :initial-values="initialData"
                         :validation-schema="schema"
                         :save-data-function="save"
                         save-button-label="Submit Question"
                         save-button-icon="fas fa-paper-plane"
                         :style="{ width: '45rem !important' }"
                         @on-cancel="emit('done')">
    <p class="mb-3" data-cy="askForHelpScope">Your question about <strong>{{ scopeText() }}</strong> is only visible to you and the project's staff.</p>
    <SkillsTextInput name="title" label="Title" data-cy="helpTitleInput" :max-length="HELP_MAX_TITLE_LENGTH" />
    <MarkdownEditor class="mt-4" name="body" label="Question" data-cy="helpBodyInput" :allow-attachments="false" :allow-community-elevation="false" />
  </SkillsInputFormDialog>
</template>
