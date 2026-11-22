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
import SkillsTitle from '@/skills-display/components/utilities/SkillsTitle.vue'
import SkillsSpinner from '@/components/utils/SkillsSpinner.vue'
import HelpThread from '@/common-components/help/HelpThread.vue'
import { useHelpService } from '@/skills-display/services/UseHelpService.js'

const route = useRoute()
const helpService = useHelpService()
const thread = ref(null)
const loading = ref(true)
const busy = ref(false)

onMounted(() => {
  helpService.getQuestion(route.params.id).then((res) => { thread.value = res }).finally(() => { loading.value = false })
})
const run = (fn) => {
  busy.value = true
  return fn().then((res) => { thread.value = res }).finally(() => { busy.value = false })
}
</script>

<template>
  <div>
    <SkillsTitle>My Question</SkillsTitle>
    <SkillsSpinner :is-loading="loading" />
    <div v-if="thread" class="mt-4">
      <HelpThread :thread="thread" :busy="busy"
                  @reply="(body) => run(() => helpService.reply(thread.id, body))"
                  @resolve="run(() => helpService.resolve(thread.id))"
                  @reopen="run(() => helpService.reopen(thread.id))" />
    </div>
  </div>
</template>
