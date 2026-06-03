import apiClient from './client'
import { mockApplications, mockApplicationDetail, mockResult, delay } from './mock'

// ─── Öğrenci uçları (backend hazır — her zaman gerçek) ───────────────────────

const studentApi = {
  // GET /applications Spring Page döndürüyor; düz array için content'i alıyoruz.
  getMyApplications: () =>
    apiClient.get('/applications').then((res) => res.data.content),

  getApplicationById: (id) =>
    apiClient.get(`/applications/${id}`).then((res) => res.data),

  // Akademik bilgiler YÖKSİS'ten gelir (read-only). Formda görünen = kaydedilen;
  // backend create de aynı kaynaktan türetir.
  getMyYoksisData: () =>
    apiClient.get('/yoksis/me').then((res) => res.data),

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
}

// ─── ÖİDB: statü & alan eşlemesi ─────────────────────────────────────────────
// Backend ApplicationStatus enum'ı ile panelin beklediği statü adları farklı;
// tek noktada eşliyoruz (krş. ydyo.js mapApplication deseni).
//   SUBMITTED              → OIDB_REVIEW   (OİDB'nin ilk incelemesini bekl
//   REVISION_REQUESTED     → REQUEST_UPDATE
//   YDYO_ACCEPTED          → YDYO_APPROVED
//   DEAN_OFFICE_REVIEW +   → FACULTY_REVIEW (fakülte/YGK hattındaki tüm ara statüler)
const OIDB_STATUS_MAP = {
  SUBMITTED: 'OIDB_REVIEW',
  OIDB_REVIEW: 'OIDB_REVIEW',
  REVISION_REQUESTED: 'REQUEST_UPDATE',
  OIDB_REJECTED: 'REJECTED',
  YDYO_REVIEW: 'YDYO_REVIEW',
  YDYO_EXAM_PENDING: 'YDYO_REVIEW',
  YDYO_ACCEPTED: 'YDYO_APPROVED',
  YDYO_REJECTED: 'YDYO_REJECTED',
  DEAN_OFFICE_REVIEW: 'FACULTY_REVIEW',
  YGK_REVIEW: 'FACULTY_REVIEW',
  YGK_REVIEW_DONE: 'FACULTY_REVIEW',
  FACULTY_BOARD_REVIEW: 'FACULTY_REVIEW',
  FACULTY_BOARD_REJECTED: 'FACULTY_REVIEW',
  FACULTY_BOARD_ACCEPTED: 'FACULTY_REVIEW',
  OIDB_FINAL_REVIEW: 'FACULTY_REVIEW',
  APPROVED: 'ACCEPTED',
  REJECTED: 'REJECTED',
}

const mapStatus = (status) => OIDB_STATUS_MAP[status] ?? status

// Henüz gönderilmemiş / geri çekilmiş başvurular OİDB panelinde gösterilmez.
const HIDDEN_STATUSES = ['DRAFT', 'WITHDRAWN']

// Backend ApplicationResponse → ÖİDB panelinin beklediği şekil.
const mapOidbApplication = (dto) => {
  if (!dto) return dto
  return {
    applicationId: dto.id,
    studentName: dto.studentName,
    studentEmail: dto.email,
    studentPhone: dto.phoneNumber,
    currentUniversity: dto.currentUniversity,
    currentDepartment: dto.currentDepartment,
    semester: dto.academicYear,
    targetDepartment: dto.targetDepartment,
    targetFaculty: dto.targetFaculty,
    status: mapStatus(dto.status),
    submittedAt: dto.submissionDate,
    createdAt: dto.submissionDate,
    documents: (dto.documents ?? []).map((d) => ({
      documentId: d.documentId,
      docType: d.documentType,
      fileName: d.fileName,
    })),
  }
}

// ─── ÖİDB: mock implementasyonu (VITE_USE_MOCK !== 'false' iken aktif) ────────
const oidbMock = {
  getOidbApplications: async () => {
    await delay(500)
    return mockApplications
  },

  getOidbApplicationById: async () => {
    await delay(300)
    return mockApplicationDetail
  },

  requestApplicationUpdate: async () => {
    await delay(500)
    return { success: true }
  },

  sendToYdyo: async () => {
    await delay(500)
    return { success: true }
  },

  forwardToFaculty: async () => {
    await delay(500)
    return { success: true }
  },

  rejectApplication: async () => {
    await delay(500)
    return { success: true }
  },
}

// ─── ÖİDB: gerçek backend adaptörü ───────────────────────────────────────────
// Panelin 4 aksiyonu da tek dinamik uca (PATCH /{id}/oidb-review) eşlenir; backend
// başvurunun güncel statüsüne göre doğru iş akışına yönlendirir (processDynamicOidbReview).
// DTO alanı `isApproved` ama Jackson JSON adı `approved` (krş. ydyo-initial-review).
const oidbReal = {
  // Board tüm statüleri ister; filtresiz çekip DRAFT/WITHDRAWN dışındakileri eşliyoruz.
  getOidbApplications: async () => {
    const res = await apiClient.get('/applications', { params: { size: 500 } })
    return (res.data.content ?? [])
      .filter((dto) => !HIDDEN_STATUSES.includes(dto.status))
      .map(mapOidbApplication)
  },

  getOidbApplicationById: async (id) =>
    apiClient.get(`/applications/${id}`).then((r) => mapOidbApplication(r.data)),

  // Güncelleme İste → REVISION_REQUESTED
  requestApplicationUpdate: (id) =>
    apiClient
      .patch(`/applications/${id}/oidb-review`, { approved: false, requestRevision: true })
      .then((r) => r.data),

  // YDYO'ya Gönder (1. aşama onayı) → YDYO_REVIEW
  sendToYdyo: (id) =>
    apiClient
      .patch(`/applications/${id}/oidb-review`, { approved: true, requestRevision: false })
      .then((r) => r.data),

  // Fakülteye Gönder (YDYO sonrası onay) → DEAN_OFFICE_REVIEW
  forwardToFaculty: (id) =>
    apiClient
      .patch(`/applications/${id}/oidb-review`, { approved: true, requestRevision: false })
      .then((r) => r.data),

  // Reddet (1. aşamada OIDB_REJECTED, YDYO sonrasında REJECTED)
  rejectApplication: (id) =>
    apiClient
      .patch(`/applications/${id}/oidb-review`, { approved: false, requestRevision: false })
      .then((r) => r.data),
}

// Varsayılan: MOCK. Gerçek backend'e geçmek için .env'de VITE_USE_MOCK=false.
const USE_MOCK = import.meta.env.VITE_USE_MOCK !== 'false'

const oidbApi = USE_MOCK ? oidbMock : oidbReal

export const applicationsApi = {
  ...studentApi,
  ...oidbApi,

  // Sonucu Paylaş: backend ucu henüz yok → bayraktan bağımsız mock no-op.
  // TODO: sonuç ilan/paylaşım endpoint'i geldiğinde gerçek çağrıya bağla.
  shareResults: async () => {
    await delay(500)
    return { success: true }
  },

  // --- Sonuç (gerçek uç + fallback) ---
  getMyResult: async () => {
    try {
      const response = await apiClient.get('/applications/result')
      return response.data
    } catch {
      await delay(500)
      return mockResult
    }
  },
}
