import { delay } from './mock'

// ─── YDYO status flow (state machine, per UC-7) ───────────────────────────────
// YDYO_REVIEW
//   ├─ Belge Onaylandı   → YDYO_ACCEPTED        (Muaf — no exam)
//   ├─ Belge Onaylanmadı → YDYO_EXAM_PENDING ─(bulk exam)─┬─ ≥ threshold → YDYO_ACCEPTED
//   └─ Belge Reddedildi  → YDYO_REJECTED                  └─ < threshold → YDYO_REJECTED
export const YDYO_STATUS = {
  REVIEW: 'YDYO_REVIEW',
  EXAM_PENDING: 'YDYO_EXAM_PENDING',
  ACCEPTED: 'YDYO_ACCEPTED',
  REJECTED: 'YDYO_REJECTED',
}

// Statuses still in the YDYO pipeline (not finalized). Used by the forward guard.
export const PENDING_YDYO_STATUSES = [YDYO_STATUS.REVIEW, YDYO_STATUS.EXAM_PENDING]

// ─── "Belge Onay Durumu" decisions → ydyo-initial-review request body ─────────
// Two booleans encode three outcomes. Centralised so a real-binding change only
// touches one place.
// TODO: verify approved/requiresExam semantics on real binding
export const DOC_DECISIONS = {
  APPROVED:     { value: 'APPROVED',     label: 'Onaylandı',   approved: true,  requiresExam: false },
  NOT_APPROVED: { value: 'NOT_APPROVED', label: 'Onaylanmadı', approved: false, requiresExam: true  },
  REJECTED:     { value: 'REJECTED',     label: 'Reddedildi',  approved: false, requiresExam: false },
}

// Predefined proficiency-exam pass mark (UC-7 §12 — pass/fail is threshold-based,
// not a manual toggle). Score >= threshold ⇒ "Sınav Başarılı".
// TODO: confirm the real threshold / per-exam-type thresholds with YDYO.
export const PASS_THRESHOLD = 60

// ─── Badge derivation helpers (read-only, auto-calculated per UC-7) ───────────
// The board & detail render three independent badges. Derived from canonical
// record fields so mock and (later) real data stay consistent.
// TODO: map to real ApplicationResponse fields

// "Belge Onay Durumu" — the student-level document-review decision (not per file).
export const deriveDocumentApproval = (app) => {
  if (app.status === YDYO_STATUS.REVIEW) return 'PENDING'
  if (app.exemptionApproved === true) return 'APPROVED'   // docs approved ⇒ exempt
  return 'NOT_APPROVED'                                    // sent to exam, or rejected
}

// "Sınav Sonucu" — dynamically follows the document decision (UC-7 §6 / §5a).
export const deriveExamStatus = (app) => {
  if (app.status === YDYO_STATUS.REVIEW) return 'NOT_DETERMINED'
  if (app.requiresExam === false) return 'NOT_REQUIRED'    // exempted ⇒ no exam
  // examPassed önceliklidir: detayda puansız "Başarılı/Başarısız" kararı da doğru görünsün.
  if (app.examPassed === true) return 'PASSED'
  if (app.examPassed === false) return 'FAILED'
  return 'PENDING'                                         // YDYO_EXAM_PENDING, henüz sonuç yok
}

// "Muafiyet Sonucu" — strictly read-only, computed from status (UC-7 special req).
export const deriveExemptionStatus = (app) => {
  if (app.status === YDYO_STATUS.ACCEPTED) return 'EXEMPT'
  if (app.status === YDYO_STATUS.REJECTED) return 'NOT_EXEMPT'
  return 'PENDING'                                         // REVIEW or EXAM_PENDING
}

// ─── Mock store ───────────────────────────────────────────────────────────────
// Mutable in-memory list so review/exam actions are reflected on re-fetch.
// Mixed statuses on purpose so the board exercises every badge.
// YDYO only views documents — it never uploads them (UC-7 assumptions).
let mockApplications = [
  {
    applicationId: 71,
    studentName: 'Ayşe Demir',
    tcKimlikNo: '12345678901',
    email: 'ayse.demir@std.iyte.edu.tr',
    phone: '0532 111 22 33',
    currentUniversity: 'Ege Üniversitesi',
    currentDepartment: 'Bilgisayar Mühendisliği',
    targetDepartment: 'Bilgisayar Mühendisliği',
    targetFaculty: 'Mühendislik Fakültesi',
    academicYear: '2025-2026',
    status: YDYO_STATUS.REVIEW,
    requiresExam: null,
    exemptionApproved: null,
    examScore: null,
    examPassed: null,
    submittedAt: '2026-05-10T09:20:00Z',
    notes: '',
    documents: [
      { documentId: 201, docType: 'TRANSCRIPT', fileName: 'Transkript.pdf' },
      { documentId: 202, docType: 'APPROVAL',   fileName: 'Onay_Belgesi.pdf' },
    ],
    // English proficiency cert. examType/score may be absent — not every cert is
    // machine-readable, so the system can't always extract the score.
    englishCertificate: { documentId: 205, fileName: 'TOEFL_Belgesi.pdf', examType: 'TOEFL', score: 65 },
  },
  {
    applicationId: 72,
    studentName: 'Mehmet Kaya',
    tcKimlikNo: '23456789012',
    email: 'mehmet.kaya@std.iyte.edu.tr',
    phone: '0533 222 33 44',
    currentUniversity: 'Dokuz Eylül Üniversitesi',
    currentDepartment: 'Elektrik-Elektronik Mühendisliği',
    targetDepartment: 'Elektrik-Elektronik Mühendisliği',
    targetFaculty: 'Mühendislik Fakültesi',
    academicYear: '2025-2026',
    status: YDYO_STATUS.EXAM_PENDING,
    requiresExam: true,
    exemptionApproved: false,
    examScore: null,
    examPassed: null,
    submittedAt: '2026-05-08T14:05:00Z',
    notes: 'İngilizce yeterlilik belgesi eksik, sınava yönlendirildi.',
    documents: [
      { documentId: 203, docType: 'TRANSCRIPT', fileName: 'Transkript.pdf' },
    ],
    englishCertificate: null,
  },
  {
    applicationId: 73,
    studentName: 'Zeynep Şahin',
    tcKimlikNo: '34567890123',
    email: 'zeynep.sahin@std.iyte.edu.tr',
    phone: '0534 333 44 55',
    currentUniversity: 'Boğaziçi Üniversitesi',
    currentDepartment: 'Makine Mühendisliği',
    targetDepartment: 'Makine Mühendisliği',
    targetFaculty: 'Mühendislik Fakültesi',
    academicYear: '2025-2026',
    status: YDYO_STATUS.ACCEPTED,
    requiresExam: false,
    exemptionApproved: true,
    examScore: null,
    examPassed: null,
    submittedAt: '2026-05-02T11:40:00Z',
    notes: 'Cambridge C1 ile muaf.',
    documents: [
      { documentId: 204, docType: 'TRANSCRIPT', fileName: 'Transkript.pdf' },
    ],
    englishCertificate: { documentId: 206, fileName: 'Cambridge_C1.pdf', examType: 'Cambridge', score: null },
  },
  {
    applicationId: 74,
    studentName: 'Can Yıldız',
    tcKimlikNo: '45678901234',
    email: 'can.yildiz@std.iyte.edu.tr',
    phone: '0535 444 55 66',
    currentUniversity: 'Orta Doğu Teknik Üniversitesi',
    currentDepartment: 'İnşaat Mühendisliği',
    targetDepartment: 'İnşaat Mühendisliği',
    targetFaculty: 'Mühendislik Fakültesi',
    academicYear: '2025-2026',
    status: YDYO_STATUS.REJECTED,
    requiresExam: true,
    exemptionApproved: false,
    examScore: 42,
    examPassed: false,
    submittedAt: '2026-04-28T08:15:00Z',
    notes: 'Yeterlilik sınavı notu eşiğin altında.',
    documents: [
      { documentId: 207, docType: 'TRANSCRIPT', fileName: 'Transkript.pdf' },
    ],
    englishCertificate: null,
  },
]

// Clone so callers can't mutate the store directly.
const clone = (obj) => JSON.parse(JSON.stringify(obj))

const findOrThrow = (id) => {
  const app = mockApplications.find((a) => a.applicationId === Number(id))
  if (!app) throw new Error('Başvuru bulunamadı.')
  return app
}

export const ydyoApi = {
  // GET /api/applications?status=YDYO_REVIEW (Spring Page → .content)
  // The real list endpoint filters by a single status; the board needs every YDYO
  // stage. Mock returns all YDYO records when no status is passed.
  // TODO: replace with real API call →
  //   apiClient.get('/applications', { params: { status } }).then((r) => r.data.content)
  // TODO: board needs all YDYO_* stages but the endpoint takes one status — either
  //   call per status and merge, or fetch unfiltered and filter YDYO_* client-side.
  getApplications: async (status) => {
    await delay(400)
    const list = status
      ? mockApplications.filter((a) => a.status === status)
      : mockApplications
    return { content: clone(list) }
  },

  // GET /api/applications/{id} (ApplicationResponse)
  // TODO: replace with real API call →
  //   apiClient.get(`/applications/${id}`).then((r) => r.data)
  getApplicationById: async (id) => {
    await delay(300)
    return clone(findOrThrow(id))
  },

  // GET /api/applications/{applicationId}/documents
  // TODO: replace with real API call →
  //   apiClient.get(`/applications/${applicationId}/documents`).then((r) => r.data)
  getDocuments: async (applicationId) => {
    await delay(200)
    return clone(findOrThrow(applicationId).documents ?? [])
  },

  // GET /api/documents/{documentId}/download → file blob.
  // YDYO only views documents; the link opens the proxied download URL.
  // TODO: replace with real API call →
  //   apiClient.get(`/documents/${documentId}/download`, { responseType: 'blob' })
  getDocumentDownloadUrl: (documentId) => `/api/documents/${documentId}/download`,

  // PATCH /api/applications/{id}/ydyo-initial-review  (Phase 1 — document review)
  // Body: { approved, requiresExam, notes, reviewerId }.
  // ⚠️ Only send reviewerId — never the full reviewer entity (passwordHash etc.).
  // The single "Belge Onay Durumu" decision drives Sınav Sonucu & Muafiyet Sonucu.
  // TODO: replace with real API call →
  //   apiClient.patch(`/applications/${id}/ydyo-initial-review`,
  //     { approved, requiresExam, notes, reviewerId }).then((r) => r.data)
  submitInitialReview: async (id, { approved, requiresExam, notes, reviewerId }) => {
    await delay(400)
    const app = findOrThrow(id)
    app.approved = approved
    app.requiresExam = requiresExam
    app.exemptionApproved = approved && !requiresExam
    app.notes = notes
    app.reviewerId = reviewerId
    if (approved && !requiresExam) {
      app.status = YDYO_STATUS.ACCEPTED        // Belge Onaylandı → Muaf
    } else if (!approved && requiresExam) {
      app.status = YDYO_STATUS.EXAM_PENDING    // Belge Onaylanmadı → sınava
    } else {
      app.status = YDYO_STATUS.REJECTED        // Belge Reddedildi → eleme
    }
    return clone(app)
  },

  // PATCH /api/applications/{id}/ydyo-exam-result  (Phase 2 — exam result)
  // Body: { examScore, passed, notes, reviewerId }. `passed` is computed from the
  // score threshold (UC-7 §12); the bulk screen lets staff override before saving.
  // TODO: replace with real API call →
  //   apiClient.patch(`/applications/${id}/ydyo-exam-result`,
  //     { examScore, passed, notes, reviewerId }).then((r) => r.data)
  submitExamResult: async (id, { examScore, passed, notes, reviewerId }) => {
    await delay(300)
    const app = findOrThrow(id)
    app.examScore = examScore
    app.examPassed = passed
    app.notes = notes
    app.reviewerId = reviewerId
    app.status = passed ? YDYO_STATUS.ACCEPTED : YDYO_STATUS.REJECTED
    return clone(app)
  },
}
