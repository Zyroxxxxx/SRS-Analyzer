import apiClient from './client'

const LONG = { timeout: 300000 } // AI / upload co the mat > 30 giay
const d = p => p.then(r => r.data)

// Phan tich co timeout dai (tai lieu Word lon). Backend: POST /requirements/{id}/analyze
export const analyze = id => d(apiClient.post(`/requirements/${id}/analyze`, null, LONG))

export const fetchVersions = id => d(apiClient.get(`/workspace/requirements/${id}/versions`))
export const generateAB = id => d(apiClient.post(`/workspace/requirements/${id}/generate-ab`, null, LONG))
export const applyVersion = (id, source, content) => d(apiClient.post(`/workspace/requirements/${id}/versions`, { source, content }))
export const restoreVersion = (id, versionId) => d(apiClient.post(`/workspace/requirements/${id}/versions/${versionId}/restore`))

export const fetchStories = id => d(apiClient.get(`/workspace/requirements/${id}/stories`))
export const addStory = (id, content, priority) => d(apiClient.post(`/workspace/requirements/${id}/stories`, { content, priority }))
export const updateStory = (sid, content, priority) => d(apiClient.put(`/workspace/stories/${sid}`, { content, priority }))
export const deleteStory = sid => apiClient.delete(`/workspace/stories/${sid}`)
export const rewriteStory = (sid, instruction) => d(apiClient.post(`/workspace/stories/${sid}/rewrite`, { instruction }, LONG))
export const confirmStory = (sid, confirmed) => d(apiClient.post(`/workspace/stories/${sid}/confirm`, { confirmed }))
export const addCriteria = (sid, content) => d(apiClient.post(`/workspace/stories/${sid}/criteria`, { content }))
export const updateCriteria = (cid, content) => d(apiClient.put(`/workspace/criteria/${cid}`, { content }))
export const deleteCriteria = cid => d(apiClient.delete(`/workspace/criteria/${cid}`))

export function importDocx(file) {
  const fd = new FormData()
  fd.append('file', file)
  return d(apiClient.post('/workspace/requirements/import-docx', fd, { ...LONG, headers: { 'Content-Type': 'multipart/form-data' } }))
}

// Tra ve object URL cua anh PNG (nho revoke khi khong dung nua)
export async function swimlaneUrl(id) {
  try {
    const r = await apiClient.get(`/workspace/requirements/${id}/swimlane.png`, { responseType: 'blob', timeout: 60000 })
    return URL.createObjectURL(r.data)
  } catch (e) {
    if (e.response?.data instanceof Blob) {
      try { e.response.data = JSON.parse(await e.response.data.text()) } catch (_) { /* giu loi goc */ }
    }
    throw e
  }
}

export const sendEmail = (id, payload) => d(apiClient.post(`/workspace/requirements/${id}/email`, payload, { timeout: 60000 }))
