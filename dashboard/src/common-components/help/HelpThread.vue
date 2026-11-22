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
import { computed, ref } from 'vue'
import Tag from 'primevue/tag'
import MarkdownText from '@/common-components/utilities/markdown/MarkdownText.vue'
import { useTimeUtils } from '@/common-components/utilities/UseTimeUtils.js'
import { HELP_MAX_BODY_LENGTH, awaitingLabel, roleLabel, scopeLabel } from '@/common-components/help/HelpUtils.js'

const props = defineProps({
  thread: { type: Object, required: true },
  // true when rendered on the admin side
  staffView: { type: Boolean, default: false },
  busy: { type: Boolean, default: false },
})
const emit = defineEmits(['reply', 'resolve', 'reopen'])

const timeUtils = useTimeUtils()
const replyText = ref('')
const isResolved = computed(() => props.thread.status === 'RESOLVED')
const canSend = computed(() => replyText.value.trim().length > 0 && replyText.value.length <= HELP_MAX_BODY_LENGTH && !props.busy)
const lastReplyText = computed(() => {
  if (!props.thread.lastReplyByUserIdForDisplay) return 'No replies yet'
  return `Last reply by ${props.thread.lastReplyByUserIdForDisplay} (${roleLabel(props.thread.lastReplyByRole)})`
})
const resolvedText = computed(() => {
  const by = props.thread.resolvedByRole === 'STUDENT' ? 'the student' : 'staff'
  const who = props.thread.resolvedByUserIdForDisplay ? ` (${props.thread.resolvedByUserIdForDisplay})` : ''
  return `Resolved by ${by}${who} ${timeUtils.formatDate(props.thread.resolvedOn)}`
})

const sendReply = () => {
  if (!canSend.value) return
  emit('reply', replyText.value.trim())
  replyText.value = ''
}
</script>

<template>
  <div data-cy="helpThread">
    <Card>
      <template #title>
        <div class="flex gap-2 items-center flex-wrap">
          <div class="flex-1" data-cy="helpThreadTitle">{{ thread.title }}</div>
          <Tag v-if="isResolved" severity="success" value="Resolved" data-cy="helpThreadStatus" />
          <Tag v-else severity="warn" value="Open" data-cy="helpThreadStatus" />
          <Tag v-if="!isResolved" severity="info" :value="awaitingLabel(thread)" data-cy="helpThreadAwaiting" />
        </div>
      </template>
      <template #subtitle>
        <div class="flex gap-4 flex-wrap text-sm" data-cy="helpThreadMeta">
          <span data-cy="helpThreadScope"><i class="fas fa-bullseye mr-1" aria-hidden="true"></i>{{ scopeLabel(thread) }}</span>
          <span v-if="staffView"><i class="fas fa-user mr-1" aria-hidden="true"></i>Asked by {{ thread.userIdForDisplay }}</span>
          <span>Asked {{ timeUtils.formatDate(thread.created) }}</span>
          <span data-cy="helpThreadLastReply">{{ lastReplyText }}</span>
        </div>
      </template>
      <template #content>
        <markdown-text :text="thread.body" data-cy="helpThreadBody" />
      </template>
    </Card>

    <div class="mt-4" data-cy="helpReplies">
      <div v-for="reply in thread.replies" :key="reply.id"
           class="mb-3 pl-3 border-l-2"
           :class="reply.authorRole === 'STUDENT' ? 'border-l-blue-400' : 'border-l-green-500'"
           :data-cy="`helpReply-${reply.id}`">
        <div class="text-sm text-muted-color">
          <span class="font-semibold">{{ reply.authorUserIdForDisplay }}</span>
          <Tag class="ml-2" :severity="reply.authorRole === 'STUDENT' ? 'info' : 'success'" :value="roleLabel(reply.authorRole)" />
          <span class="ml-2">{{ timeUtils.formatDate(reply.created) }}</span>
        </div>
        <markdown-text :text="reply.body" />
      </div>
    </div>

    <div v-if="isResolved" class="mt-4 flex gap-2 items-center flex-wrap" data-cy="helpResolvedBanner" role="status">
      <i class="fas fa-check-circle text-green-600" aria-hidden="true"></i>
      <span class="flex-1">{{ resolvedText }}. This conversation is closed.</span>
      <Button label="Reopen" icon="fas fa-undo" outlined size="small" :loading="busy"
              data-cy="helpReopenBtn" @click="emit('reopen')" />
    </div>

    <div v-else class="mt-4" data-cy="helpReplyBox">
      <label for="helpReplyInput" class="sr-only">Reply</label>
      <Textarea id="helpReplyInput" v-model="replyText" rows="4" class="w-full" auto-resize
                placeholder="Write a reply..." data-cy="helpReplyInput" :maxlength="HELP_MAX_BODY_LENGTH" />
      <div class="flex gap-2 mt-2 items-center">
        <div class="flex-1 text-sm text-muted-color">{{ replyText.length }}/{{ HELP_MAX_BODY_LENGTH }}</div>
        <Button label="Resolve" icon="fas fa-check" severity="success" outlined :loading="busy"
                data-cy="helpResolveBtn" @click="emit('resolve')" />
        <Button label="Reply" icon="fas fa-paper-plane" :disabled="!canSend" :loading="busy"
                data-cy="helpReplyBtn" @click="sendReply" />
      </div>
    </div>
  </div>
</template>
