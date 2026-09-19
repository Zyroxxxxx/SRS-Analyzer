import apiClient from './client'
import { downloadFile } from './download'

export async function fetchAdminDashboard() {
  return (await apiClient.get('/admin/dashboard')).data
}

export async function fetchAdminUsers() {
  return (await apiClient.get('/admin/users')).data
}

export async function fetchAdminUserDetail(userId) {
  return (await apiClient.get(`/admin/users/${userId}`)).data
}

// Admin tao tai khoan moi cho nguoi dung (thay vi thao tac truc tiep vao database)
export async function createAdminUser(payload) {
  // payload: { username, email, password, fullName, role }
  return (await apiClient.post('/admin/users', payload)).data
}

// Cap nhat 1 user - chi truyen truong can doi (fullName / role / active / tokenQuota)
export async function updateAdminUser(userId, payload) {
  return (await apiClient.put(`/admin/users/${userId}`, payload)).data
}

export async function exportAdminUsageReport() {
  await downloadFile('/export/admin/usage-excel', 'bao-cao-su-dung-AI.xlsx')
}

export async function deleteAdminUser(userId) {
  await apiClient.delete(`/admin/users/${userId}`)
}

export async function fetchAdminRequirements() {
  return (await apiClient.get('/admin/requirements')).data
}

export async function fetchAdminAudit() {
  return (await apiClient.get('/admin/audit')).data
}

export async function deleteAdminAudit(auditId) {
  await apiClient.delete(`/admin/audit/${auditId}`)
}

export async function deleteAllAdminAudit() {
  await apiClient.delete('/admin/audit')
}
