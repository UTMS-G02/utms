import apiClient from './client'

const intibakApi = {
  getIntibak: (applicationId) =>
    apiClient.get(`/applications/${applicationId}/intibak`).then((r) => r.data),

  addRow: (applicationId, row) =>
    apiClient.post(`/applications/${applicationId}/intibak/rows`, row).then((r) => r.data),

  updateRow: (applicationId, rowId, row) =>
    apiClient.patch(`/applications/${applicationId}/intibak/rows/${rowId}`, row).then((r) => r.data),

  deleteRow: (applicationId, rowId) =>
    apiClient.delete(`/applications/${applicationId}/intibak/rows/${rowId}`).then((r) => r.data),

  saveTable: (applicationId, payload) =>
    apiClient.put(`/applications/${applicationId}/intibak`, payload).then((r) => r.data),
}

export default intibakApi
