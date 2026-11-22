/*
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
describe('Help Hub Tests', () => {
    const studentUser = 'helpstudent@skills.org'
    const approverUser = 'helpapprover@skills.org'
    const password = 'password'

    const loginAsDefaultUser = () => {
        cy.logout()
        cy.fixture('vars.json').then((vars) => {
            if (!Cypress.env('oauthMode')) {
                cy.login(vars.defaultUser, vars.defaultPass)
            } else {
                cy.loginBySingleSignOn()
            }
        })
    }

    beforeEach(() => {
        // register the other users first so they exist, then set everything up as the default (admin) user
        cy.logout()
        cy.register(studentUser, password)
        cy.register(approverUser, password)
        loginAsDefaultUser()
        cy.createProject(1)
        cy.createSubject(1, 1)
        cy.createSkill(1, 1, 1)
        cy.request('POST', `/admin/projects/proj1/users/${approverUser}/roles/ROLE_PROJECT_APPROVER`)
        cy.logout()
    })

    const askQuestion = (title, body) => {
        cy.get('[data-cy="askForHelpBtn"]').click()
        cy.get('[data-cy="helpTitleInput"]').type(title)
        cy.get('[data-cy="helpBodyInput"] [contenteditable="true"]').first().type(body)
        cy.get('[data-cy="saveDialogBtn"]').click()
    }

    it('student asks, approver replies, student resolves; admin is notified and notification is dismissed on resolve', () => {
        cy.login(studentUser, password)
        cy.visit('/progress-and-rankings/projects/proj1')
        askQuestion('How do I start?', 'I am lost')
        cy.get('[data-cy="helpThreadTitle"]').should('have.text', 'How do I start?')
        cy.get('[data-cy="helpThreadStatus"]').should('contain.text', 'Open')
        cy.get('[data-cy="helpThreadAwaiting"]').should('contain.text', 'Awaiting staff')
        cy.logout()

        // admin gets an in-app notification with a deep link
        loginAsDefaultUser()
        cy.visit('/administrator/')
        cy.get('[data-cy="notifBtn"]').click()
        cy.get('[data-cy="notifPanel"]').contains('New help request')
        cy.get('[data-cy="notifLink"]').first().should('have.attr', 'href').and('contain', '/administrator/projects/proj1/help/')
        cy.logout()

        // approver can see the nav entry with an open count and reply
        cy.login(approverUser, password)
        cy.visit('/administrator/projects/proj1/')
        cy.get('[data-cy="navBadge-Help"]').should('contain.text', '1')
        cy.get('[data-cy="nav-Help"]').click()
        cy.get('[data-cy="helpTable"]').contains('How do I start?').click()
        cy.get('[data-cy="helpThreadMeta"]').should('contain.text', 'No replies yet')
        cy.get('[data-cy="helpReplyInput"]').type('Start with the first skill')
        cy.get('[data-cy="helpReplyBtn"]').click()
        cy.get('[data-cy="helpReplies"]').contains('Start with the first skill')
        cy.get('[data-cy="helpThreadLastReply"]').should('contain.text', 'Last reply by')
        cy.get('[data-cy="helpThreadAwaiting"]').should('contain.text', 'Awaiting student')
        cy.logout()

        // student sees the reply, replies and resolves
        cy.login(studentUser, password)
        cy.visit('/progress-and-rankings/projects/proj1/help')
        cy.get('[data-cy="myQuestions"]').contains('How do I start?').click()
        cy.get('[data-cy="helpReplies"]').contains('Start with the first skill')
        cy.get('[data-cy="helpReplyInput"]').type('Thanks!')
        cy.get('[data-cy="helpReplyBtn"]').click()
        cy.get('[data-cy="helpResolveBtn"]').click()
        cy.get('[data-cy="helpResolvedBanner"]').should('contain.text', 'Resolved by the student')
        cy.get('[data-cy="helpReplyBox"]').should('not.exist')
        cy.logout()

        // the admin's notification was auto-dismissed
        loginAsDefaultUser()
        cy.visit('/administrator/')
        cy.get('[data-cy="notifBtn"]').click()
        cy.get('[data-cy="notifPanel"]').should('not.contain.text', 'New help request')
    })

    it('resolved thread can be reopened and replied to', () => {
        cy.login(studentUser, password)
        cy.request('POST', '/api/projects/proj1/help', { scopeType: 'PROJECT', title: 'Q1', body: 'body' }).then((res) => {
            cy.request('POST', `/api/projects/proj1/help/${res.body.id}/resolve`)
            cy.visit(`/progress-and-rankings/projects/proj1/help/${res.body.id}`)
        })
        cy.get('[data-cy="helpResolvedBanner"]').should('exist')
        cy.get('[data-cy="helpReopenBtn"]').click()
        cy.get('[data-cy="helpReplyBox"]').should('exist')
        cy.get('[data-cy="helpThreadStatus"]').should('contain.text', 'Open')
    })

    it('admin inbox filters by status and scope', () => {
        cy.login(studentUser, password)
        cy.request('POST', '/api/projects/proj1/help', { scopeType: 'PROJECT', title: 'Project question', body: 'b' })
        cy.request('POST', '/api/projects/proj1/help', { scopeType: 'SKILL', skillId: 'skill1', title: 'Skill question', body: 'b' })
        cy.request('POST', '/api/projects/proj1/help', { scopeType: 'PROJECT', title: 'Done question', body: 'b' }).then((res) => {
            cy.request('POST', `/api/projects/proj1/help/${res.body.id}/resolve`)
        })
        cy.logout()

        loginAsDefaultUser()
        cy.visit('/administrator/projects/proj1/help')
        cy.get('[data-cy="helpTable"]').should('contain.text', 'Project question').and('contain.text', 'Skill question')
        cy.get('[data-cy="helpTable"]').should('not.contain.text', 'Done question')

        cy.get('[data-cy="helpScopeFilter"]').click()
        cy.get('[role="option"]').contains('Skill').click()
        cy.get('[data-cy="helpTable"]').should('contain.text', 'Skill question').and('not.contain.text', 'Project question')

        cy.get('[data-cy="helpScopeFilter"]').click()
        cy.get('[role="option"]').contains('All').click()
        cy.get('[data-cy="helpStatusFilter"]').click()
        cy.get('[role="option"]').contains('Resolved').click()
        cy.get('[data-cy="helpTable"]').should('contain.text', 'Done question').and('not.contain.text', 'Project question')
    })

    it('a student cannot see another student\'s question or use staff endpoints', () => {
        cy.login(studentUser, password)
        cy.request('POST', '/api/projects/proj1/help', { scopeType: 'PROJECT', title: 'Mine', body: 'b' }).then((res) => {
            cy.logout()
            cy.register('otherhelpstudent@skills.org', password)
            cy.login('otherhelpstudent@skills.org', password)
            cy.request({ url: `/api/projects/proj1/help/${res.body.id}`, failOnStatusCode: false }).its('status').should('eq', 400)
            cy.request({ url: '/admin/projects/proj1/help?limit=10&page=1&orderBy=created&ascending=false', failOnStatusCode: false })
                .its('status').should('be.oneOf', [401, 403])
        })
    })
})
