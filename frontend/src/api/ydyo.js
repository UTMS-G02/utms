import apiClient from './client'
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

// ─── Karar → türetilmiş alanların TAMAMI (tek kaynak) ─────────────────────────
// Bug: karar değişince (örn. "Onaylandı" iken sonradan "Onaylanmadı + puan")
// yalnız bir kısım alan güncellenip diğerleri eski kalıyordu → rozetler birbiriyle
// çelişiyordu (Onaylandı + Sınav Gerekli Değil + Muaf Değil gibi). Çözüm: her karar
// için status / requiresExam / exemptionApproved / examScore / examPassed alanlarının
// HEPSİ birlikte hesaplanır; hiçbir alan eski (stale) kalmaz.

// "Belge Onay Durumu" kararı → tutarlı alan kümesi.
export const initialReviewFields = (approved, requiresExam) => {
  if (approved && !requiresExam) {
    // Belge yeterli → muaf, sınav gerekmez. Eski sınav sonucu varsa temizlenir.
    return { status: YDYO_STATUS.ACCEPTED, requiresExam: false, exemptionApproved: true,  examScore: null, examPassed: null }
  }
  if (!approved && requiresExam) {
    // Belge yetersiz → sınava yönlendir. Önceki sonuç (varsa) sıfırlanır.
    return { status: YDYO_STATUS.EXAM_PENDING, requiresExam: true, exemptionApproved: false, examScore: null, examPassed: null }
  }
  // Belge reddedildi → eleme.
  return { status: YDYO_STATUS.REJECTED, requiresExam: false, exemptionApproved: false, examScore: null, examPassed: null }
}

// Sınav sonucu girişi → tutarlı alan kümesi (sınav yolu ⇒ belge muafiyeti yok).
export const examResultFields = (examScore, passed) => ({
  status: passed ? YDYO_STATUS.ACCEPTED : YDYO_STATUS.REJECTED,
  requiresExam: true,
  exemptionApproved: false,
  examScore,
  examPassed: passed,
})

// Kayıt zaten KESİN bir karara bağlanmış mı? (ACCEPTED/REJECTED). Bu durumdaki bir
// kaydı yeniden değerlendirmek "değişiklik" sayılır → uyarı işareti konur.
export const wasDecided = (app) => app.status === YDYO_STATUS.ACCEPTED || app.status === YDYO_STATUS.REJECTED

// Yeniden değerlendirme olduysa "değişiklik yapılmıştır" damgasını ekle (kim/ne zaman).
const stampModification = (app, reEvaluation, reviewerId, reviewerEmail) => {
  if (!reEvaluation) return
  app.modified = true
  app.modifiedBy = reviewerEmail ?? (reviewerId != null ? `#${reviewerId}` : 'bilinmiyor')
  app.modifiedAt = new Date().toISOString()
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
  {
    // Belge onaylanmadı (yetersiz) → sınava girdi → geçti → otomatik muaf.
    applicationId: 75,
    studentName: 'Elif Aydın',
    tcKimlikNo: '56789012345',
    email: 'elif.aydin@std.iyte.edu.tr',
    phone: '0536 555 66 77',
    currentUniversity: 'İstanbul Teknik Üniversitesi',
    currentDepartment: 'Endüstri Mühendisliği',
    targetDepartment: 'Endüstri Mühendisliği',
    targetFaculty: 'Mühendislik Fakültesi',
    academicYear: '2025-2026',
    status: YDYO_STATUS.ACCEPTED,
    requiresExam: true,
    exemptionApproved: false,
    examScore: 72,
    examPassed: true,
    submittedAt: '2026-05-05T10:30:00Z',
    notes: 'Belgesi yetersiz, sınavdan geçti — muaf.',
    documents: [
      { documentId: 208, docType: 'TRANSCRIPT', fileName: 'Transkript.pdf' },
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

const mockApi = {
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
  submitInitialReview: async (id, { approved, requiresExam, notes, reviewerId, reviewerEmail }) => {
    await delay(400)
    const app = findOrThrow(id)
    const reEvaluation = wasDecided(app)     // damga: zaten karara bağlıyken mi değişiyor?
    app.approved = approved
    app.notes = notes
    app.reviewerId = reviewerId
    // Türetilen alanların TAMAMINI birlikte yaz → hiçbiri eski (stale) kalmaz.
    Object.assign(app, initialReviewFields(approved, requiresExam))
    stampModification(app, reEvaluation, reviewerId, reviewerEmail)
    return clone(app)
  },

  // PATCH /api/applications/{id}/ydyo-exam-result  (Phase 2 — exam result)
  // Body: { examScore, passed, notes, reviewerId }. The exam result is entered per
  // student from the application detail (Onaylanmadı → Sınav Sonucu dropdown).
  // TODO: replace with real API call →
  //   apiClient.patch(`/applications/${id}/ydyo-exam-result`,
  //     { examScore, passed, notes, reviewerId }).then((r) => r.data)
  submitExamResult: async (id, { examScore, passed, notes, reviewerId, reviewerEmail }) => {
    await delay(300)
    const app = findOrThrow(id)
    const reEvaluation = wasDecided(app)     // damga: zaten karara bağlıyken mi değişiyor?
    app.notes = notes
    app.reviewerId = reviewerId
    // Sınav yolu → status + requiresExam + exemptionApproved + puan tutarlı kurulur.
    Object.assign(app, examResultFields(examScore, passed))
    stampModification(app, reEvaluation, reviewerId, reviewerEmail)
    return clone(app)
  },
}

// ─── Real backend adapter ─────────────────────────────────────────────────────
// Backend `ApplicationResponse` alan adları UI'nin beklediğinden farklı; tek
// noktada eşliyoruz. (Bkz. docs/ydyo-frontend-backend-uyum.md)
const LANG_CERT_TYPES = ['LANGUAGE_CERT', 'LANGUAGE_CERTIFICATE', 'ENGLISH_CERT', 'YDS', 'TOEFL']

const mapApplication = (dto) => {
  if (!dto) return dto
  const allDocs = (dto.documents ?? []).map((d) => ({
    documentId: d.documentId,
    docType: d.documentType,
    fileName: d.fileName,
  }))
  // İngilizce yeterlilik belgesi backend'de ayrı bir kavram değil → dil belgesi
  // tipindeki ilk belgeden türetilir; kalanı "Başvuru Belgeleri" altında kalır.
  const isLangCert = (d) => LANG_CERT_TYPES.includes((d.docType ?? '').toUpperCase())
  const langDoc = allDocs.find(isLangCert)
  return {
    applicationId: dto.id,
    studentName: dto.studentName,
    tcKimlikNo: dto.tckn,
    email: dto.email,
    phone: dto.phoneNumber,
    currentUniversity: dto.currentUniversity,
    currentDepartment: dto.currentDepartment,
    targetDepartment: dto.targetDepartment,
    targetFaculty: dto.targetFaculty,
    academicYear: dto.academicYear,
    status: dto.status,
    requiresExam: dto.requiresExam,
    exemptionApproved: dto.ydyoApproved,   // backend "ydyoApproved" → UI "exemptionApproved"
    examScore: dto.examScore,
    examPassed: dto.examPassed,
    submittedAt: dto.submissionDate,
    notes: dto.ydyoNotes ?? '',
    documents: allDocs.filter((d) => !isLangCert(d)),
    englishCertificate: langDoc
      ? { documentId: langDoc.documentId, fileName: langDoc.fileName, examType: null, score: null }
      : null,
    // Karar sonradan değiştirildiyse "değişiklik yapılmıştır" işareti. Backend bu
    // alanları henüz döndürmüyor → şimdilik undefined (işaret gizli) kalır.
    // TODO: backend ApplicationResponse'a modified/modifiedBy/modifiedAt eklenince aktif olur.
    modified: dto.modified,
    modifiedBy: dto.modifiedBy,
    modifiedAt: dto.modifiedAt,
  }
}

const realApi = {
  // Board tüm YDYO_* aşamalarını ister ama endpoint tek status alır → filtresiz
  // çekip client-side YDYO_* filtreliyoruz (status verilirse doğrudan ona filtre).
  getApplications: async (status) => {
    const params = status ? { status, size: 500 } : { size: 500 }
    const res = await apiClient.get('/applications', { params })
    const mapped = (res.data.content ?? []).map(mapApplication)
    const list = status ? mapped : mapped.filter((a) => String(a.status).startsWith('YDYO_'))
    return { content: list }
  },

  getApplicationById: async (id) =>
    apiClient.get(`/applications/${id}`).then((r) => mapApplication(r.data)),

  getDocuments: async (applicationId) =>
    apiClient.get(`/applications/${applicationId}/documents`).then((r) =>
      (r.data ?? []).map((d) => ({
        documentId: d.documentId,
        docType: d.documentType,
        fileName: d.fileName,
      }))
    ),

  // Plain href (yeni sekmede açılır); dev proxy /api → backend.
  getDocumentDownloadUrl: (documentId) => `/api/documents/${documentId}/download`,

  // ⚠️ Sadece reviewerId gönderilir — asla tam reviewer entity'si (passwordHash vb.).
  // Backend DTO alanı `isApproved` (Jackson JSON adı `approved`).
  submitInitialReview: async (id, { approved, requiresExam, notes, reviewerId }) =>
    apiClient
      .patch(`/applications/${id}/ydyo-initial-review`, { approved, requiresExam, notes, reviewerId })
      .then((r) => mapApplication(r.data)),

  // Backend exam-result'ı yalnız YDYO_EXAM_PENDING statüsünde kabul eder. Detay
  // ekranı "Onaylanmadı + puan"ı tek adımda gönderir; kayıt henüz YDYO_REVIEW ise
  // önce sınava yönlendirip (initial-review) sonra sonucu gireriz (iki PATCH zinciri).
  submitExamResult: async (id, { examScore, passed, notes, reviewerId }) => {
    const body = { examScore, passed, notes, reviewerId }
    try {
      const r = await apiClient.patch(`/applications/${id}/ydyo-exam-result`, body)
      return mapApplication(r.data)
    } catch (err) {
      // 400 = "bekleyen sınav yok" (statü YDYO_REVIEW). Önce EXAM_PENDING'e taşı, tekrar dene.
      if (err?.response?.status !== 400) throw err
      await apiClient.patch(`/applications/${id}/ydyo-initial-review`, {
        approved: false, requiresExam: true, notes, reviewerId,
      })
      const r2 = await apiClient.patch(`/applications/${id}/ydyo-exam-result`, body)
      return mapApplication(r2.data)
    }
  },
}

// Varsayılan: MOCK. Gerçek backend'e geçmek için .env'de VITE_USE_MOCK=false.
// (Test/demo mock üzerinden geçtiği için bayrak açıkça kapatılana dek mock kalır.)
const USE_MOCK = import.meta.env.VITE_USE_MOCK !== 'false'

export const ydyoApi = USE_MOCK ? mockApi : realApi
