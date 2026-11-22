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
import axios from 'axios'
import { useSkillsDisplayAttributesState } from '@/skills-display/stores/UseSkillsDisplayAttributesState.js'

export const useHelpService = () => {
  const attributes = useSkillsDisplayAttributesState()
  const base = () => `${attributes.serviceUrl}/api/projects/${encodeURIComponent(attributes.projectId)}/help`

  const createQuestion = (question) => axios.post(base(), question).then((res) => res.data)
  const getMyQuestions = () => axios.get(`${base()}/mine`).then((res) => res.data)
  const getQuestion = (id) => axios.get(`${base()}/${id}`).then((res) => res.data)
  const reply = (id, body) => axios.post(`${base()}/${id}/replies`, { body }).then((res) => res.data)
  const resolve = (id) => axios.post(`${base()}/${id}/resolve`).then((res) => res.data)
  const reopen = (id) => axios.post(`${base()}/${id}/reopen`).then((res) => res.data)

  return { createQuestion, getMyQuestions, getQuestion, reply, resolve, reopen }
}
