import apiClient from './client'
import { mockApplications, mockApplicationDetail, mockResult, delay } from './mock'

export const applicationsApi = {
  getMyApplications: async () => {
    try {
      const response = await apiClient.get('/applications')
      return response.data
    } catch (error) {
      await delay(500)
      return mockApplications
    }
  },

  getOidbApplications: async () => {
    try {
      const response = await apiClient.get('/oidb/applications')
      return response.data
    } catch (error) {
      await delay(500)
      return mockApplications
    }
  },

  getApplicationById: async (_id) => {
    try {
      const response = await apiClient.get(`/applications/${_id}`)
      return response.data
    } catch (error) {
      await delay(500)
      return mockApplicationDetail
    }
  },

  createApplication: async (_payload) => {
    try {
      const response = await apiClient.post('/applications', _payload)
      return response.data
    } catch (error) {
      await delay(500)
      return {
        applicationId: Date.now(),
        status: 'DRAFT',
        createdAt: new Date().toISOString(),
      }
    }
  },

  submitApplication: async (_id) => {
    try {
      const response = await apiClient.post(`/applications/${_id}/submit`)
      return response.data
    } catch (error) {
      await delay(500)
      return { ...mockApplicationDetail, status: 'SUBMITTED' }
    }
  },

  uploadDocument: async (_id, _formData) => {
    try {
      const response = await apiClient.post(`/applications/${_id}/documents`, _formData)
      return response.data
    } catch (error) {
      await delay(500)
      return {
        documentId: Date.now(),
        docType: 'OTHER',
        fileName: 'document.pdf',
        uploadedAt: new Date().toISOString(),
      }
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
