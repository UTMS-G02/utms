import apiClient from './client'
import { mockApplications, mockResult, delay } from './mock'

export const applicationsApi = {
  // --- Öğrenci uçları (backend hazır) ---

  // GET /applications Spring Page döndürüyor; düz array için content'i alıyoruz.
  getMyApplications: () =>
    apiClient.get('/applications').then((res) => res.data.content),

  getApplicationById: (id) =>
    apiClient.get(`/applications/${id}`).then((res) => res.data),

  createApplication: (payload) =>
    apiClient.post('/applications', payload).then((res) => res.data),

  submitApplication: (id) =>
    apiClient.patch(`/applications/${id}/submit`).then((res) => res.data),

  withdrawApplication: (id) =>
    apiClient.patch(`/applications/${id}/withdraw`).then((res) => res.data),

  uploadDocument: (id, file, documentType) => {
    const formData = new FormData()
    formData.append('file', file)
    return apiClient
      .post(`/applications/${id}/documents`, formData, {
        params: { documentType },
        headers: { 'Content-Type': 'multipart/form-data' },
      })
      .then((res) => res.data)
  },

  // --- ÖİDB uçları (backend hazır değilse mock fallback'li) ---

  getOidbApplications: async () => {
    try {
      const response = await apiClient.get('/oidb/applications')
      return response.data
    } catch (error) {
      await delay(500)
      return mockApplications
    }
  },

  requestApplicationUpdate: async (applicationId) => {
    try {
      const response = await apiClient.post(`/oidb/applications/${applicationId}/request-update`)
      return response.data
    } catch {
      await delay(500)
      return { success: true }
    }
  },

  sendToYdyo: async (applicationId) => {
    try {
      const response = await apiClient.post(`/oidb/applications/${applicationId}/send-to-ydyo`)
      return response.data
    } catch {
      await delay(500)
      return { success: true }
    }
  },

  forwardToFaculty: async (applicationId) => {
    try {
      const response = await apiClient.post(`/oidb/applications/${applicationId}/forward-to-faculty`)
      return response.data
    } catch {
      await delay(500)
      return { success: true }
    }
  },

  rejectApplication: async (applicationId) => {
    try {
      const response = await apiClient.post(`/oidb/applications/${applicationId}/reject`)
      return response.data
    } catch {
      await delay(500)
      return { success: true }
    }
  },

  shareResults: async (applicationIds) => {
    try {
      const response = await apiClient.post('/oidb/share-results', { applicationIds })
      return response.data
    } catch {
      await delay(500)
      return { success: true }
    }
  },

  // --- Sonuç (gerçek uç + fallback) ---

  getMyResult: async () => {
    try {
      const response = await apiClient.get('/applications/result')
      return response.data
    } catch (error) {
      await delay(500)
      return mockResult
    }
  },
}
