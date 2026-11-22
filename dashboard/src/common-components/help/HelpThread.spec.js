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
import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import HelpThread from '@/common-components/help/HelpThread.vue'

const slotStub = (name) => ({ name, template: '<div><slot name="title"/><slot name="subtitle"/><slot name="content"/><slot/></div>' })
const buttonStub = {
  name: 'Button',
  props: ['label', 'disabled'],
  emits: ['click'],
  template: '<button :disabled="disabled" @click="$emit(\'click\')">{{ label }}</button>',
}
const textareaStub = {
  name: 'Textarea',
  props: ['modelValue'],
  emits: ['update:modelValue'],
  template: '<textarea :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)"></textarea>',
}
const mountThread = (thread, props = {}) => mount(HelpThread, {
  props: { thread, ...props },
  global: {
    stubs: {
      Card: slotStub('Card'),
      Tag: { props: ['value'], template: '<span>{{ value }}</span>' },
      MarkdownText: { props: ['text'], template: '<div>{{ text }}</div>' },
      Button: buttonStub,
      Textarea: textareaStub,
    },
  },
})

const openThread = {
  id: 1, title: 'How?', body: 'I am stuck', status: 'OPEN', awaiting: 'STAFF', scopeType: 'PROJECT',
  userIdForDisplay: 'student1', created: new Date().toISOString(), replies: [],
}

describe('HelpThread', () => {
  it('open thread shows reply box and resolve button, no reopen', () => {
    const wrapper = mountThread(openThread)
    expect(wrapper.find('[data-cy="helpReplyBox"]').exists()).toBe(true)
    expect(wrapper.find('[data-cy="helpResolveBtn"]').exists()).toBe(true)
    expect(wrapper.find('[data-cy="helpReopenBtn"]').exists()).toBe(false)
    expect(wrapper.text()).toContain('Awaiting staff')
  })

  it('resolved thread shows a clear state, no reply box, and a reopen button', () => {
    const wrapper = mountThread({ ...openThread, status: 'RESOLVED', awaiting: null, resolvedByRole: 'STUDENT', resolvedByUserIdForDisplay: 'student1', resolvedOn: new Date().toISOString() })
    expect(wrapper.find('[data-cy="helpReplyBox"]').exists()).toBe(false)
    expect(wrapper.find('[data-cy="helpResolvedBanner"]').text()).toContain('Resolved by the student')
    expect(wrapper.find('[data-cy="helpReopenBtn"]').exists()).toBe(true)
  })

  it('emits reply with trimmed text and clears the box; blank reply is not sent', async () => {
    const wrapper = mountThread(openThread)
    await wrapper.find('textarea').setValue('   ')
    await wrapper.find('[data-cy="helpReplyBtn"]').trigger('click')
    expect(wrapper.emitted('reply')).toBeUndefined()

    await wrapper.find('textarea').setValue('  thanks  ')
    await wrapper.find('[data-cy="helpReplyBtn"]').trigger('click')
    expect(wrapper.emitted('reply')[0]).toEqual(['thanks'])
    expect(wrapper.find('textarea').element.value).toBe('')
  })

  it('emits resolve and reopen', async () => {
    const open = mountThread(openThread)
    await open.find('[data-cy="helpResolveBtn"]').trigger('click')
    expect(open.emitted('resolve')).toHaveLength(1)

    const resolved = mountThread({ ...openThread, status: 'RESOLVED', awaiting: null })
    await resolved.find('[data-cy="helpReopenBtn"]').trigger('click')
    expect(resolved.emitted('reopen')).toHaveLength(1)
  })

  it('shows last reply info and who asked only in staff view', () => {
    const thread = { ...openThread, awaiting: 'STUDENT', lastReplyByUserIdForDisplay: 'approver1', lastReplyByRole: 'APPROVER', replyCount: 1,
      replies: [{ id: 5, authorUserIdForDisplay: 'approver1', authorRole: 'APPROVER', body: 'try this', created: new Date().toISOString() }] }
    const staff = mountThread(thread, { staffView: true })
    expect(staff.find('[data-cy="helpThreadLastReply"]').text()).toContain('Last reply by approver1 (Approver)')
    expect(staff.text()).toContain('Asked by student1')
    const student = mountThread(thread)
    expect(student.text()).not.toContain('Asked by student1')
    expect(student.find('[data-cy="helpReply-5"]').text()).toContain('try this')
  })
})
