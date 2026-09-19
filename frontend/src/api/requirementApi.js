import apiClient from './client'
import { downloadFile } from './download'

export async function fetchRequirements() {
  return (await apiClient.get('/requirements')).data
}

export async function createRequirement(payload) {
  return (await apiClient.post('/requirements', payload)).data
}

export async function deleteRequirement(id) {
  await apiClient.delete(`/requirements/${id}`)
}

export async function analyzeRequirement(id) {
  return (await apiClient.post(`/requirements/${id}/analyze`)).data
}

export async function getAnalysis(id) {
  return (await apiClient.get(`/requirements/${id}/analysis`)).data
}

// Xuat SRS dang HTML (mo bang trinh duyet -> Print -> Save as PDF)
export async function exportSrs(id) {
  await downloadFile(`/export/requirements/${id}/srs`, `REQ-${id}-SRS.html`)
}

// Xuat SRS dang Excel that (.xlsx), gom ca sheet Ma tran truy vet
export async function exportSrsExcel(id) {
  await downloadFile(`/export/requirements/${id}/excel`, `REQ-${id}-SRS.xlsx`)
}
