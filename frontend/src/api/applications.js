import { mockApplications, mockApplicationDetail, mockResult, delay } from './mock'

export const applicationsApi = {
  // TODO: replace with real API call
  getMyApplications: async () => {
    await delay(500)
    return mockApplications
  },

  // TODO: replace with real API call
  getApplicationById: async (_id) => {
    await delay(500)
    return mockApplicationDetail
  },

  // TODO: replace with real API call
  createApplication: async (_payload) => {
    await delay(500)
    return {
      applicationId: Date.now(),
      status: 'DRAFT',
      createdAt: new Date().toISOString(),
    }
  },

  // TODO: replace with real API call
  submitApplication: async (_id) => {
    await delay(500)
    return { ...mockApplicationDetail, status: 'SUBMITTED' }
  },

  // TODO: replace with real API call
  uploadDocument: async (_id, _formData) => {
    await delay(500)
    return {
      documentId: Date.now(),
      docType: 'OTHER',
      fileName: 'document.pdf',
      uploadedAt: new Date().toISOString(),
    }
  },

  // TODO: replace with real API call
  getMyResult: async () => {
    await delay(500)
    return mockResult
  },
}
