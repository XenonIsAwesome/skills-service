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

const base = (projectId) => `/admin/projects/${encodeURIComponent(projectId)}/help`

export default {
  getHelpRequests(projectId, params) {
    return axios.get(base(projectId), { params }).then((res) => res.data)
  },
  getStats(projectId) {
    return axios.get(`${base(projectId)}/stats`).then((res) => res.data)
  },
  getHelpRequest(projectId, id) {
    return axios.get(`${base(projectId)}/${id}`).then((res) => res.data)
  },
  reply(projectId, id, body) {
    return axios.post(`${base(projectId)}/${id}/replies`, { body }).then((res) => res.data)
  },
  resolve(projectId, id) {
    return axios.post(`${base(projectId)}/${id}/resolve`).then((res) => res.data)
  },
  reopen(projectId, id) {
    return axios.post(`${base(projectId)}/${id}/reopen`).then((res) => res.data)
  },
}
