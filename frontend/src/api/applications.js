import apiClient from './client'
import { mockResult, delay } from './mock'

export const applicationsApi = {
  // GET /api/applications artık Spring Page döndürüyor; sayfalar düz array
  // beklediği için içindeki content dizisini çıkarıyoruz.
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

  // TODO: replace with real API call (backend'de net karşılığı yok)
  getMyResult: async () => {
    await delay(500)
    return mockResult
  },
}
