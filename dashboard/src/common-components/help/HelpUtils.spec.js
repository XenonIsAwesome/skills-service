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
import { awaitingLabel, roleLabel, scopeLabel } from '@/common-components/help/HelpUtils.js'

describe('HelpUtils', () => {
  it('scopeLabel', () => {
    expect(scopeLabel({ scopeType: 'PROJECT' })).toBe('Project')
    expect(scopeLabel({ scopeType: 'SKILL', skillName: 'My Skill' })).toBe('Skill: My Skill')
    expect(scopeLabel({ scopeType: 'SUBJECT', skillName: 'My Subject' })).toBe('Subject: My Subject')
    expect(scopeLabel({ scopeType: 'SKILL', skillDeleted: true })).toBe('Skill: (deleted skill)')
    expect(scopeLabel(null)).toBe('')
  })

  it('roleLabel', () => {
    expect(roleLabel('ADMIN')).toBe('Admin')
    expect(roleLabel('APPROVER')).toBe('Approver')
    expect(roleLabel('STUDENT')).toBe('Student')
  })

  it('awaitingLabel is derived from the thread and empty when resolved', () => {
    expect(awaitingLabel({ status: 'OPEN', awaiting: 'STAFF' })).toBe('Awaiting staff')
    expect(awaitingLabel({ status: 'OPEN', awaiting: 'STUDENT' })).toBe('Awaiting student')
    expect(awaitingLabel({ status: 'RESOLVED', awaiting: null })).toBe('')
  })
})
