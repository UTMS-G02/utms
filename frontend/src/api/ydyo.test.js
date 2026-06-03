import { describe, it, expect } from 'vitest'
import {
  ydyoApi,
  YDYO_STATUS,
  initialReviewFields,
  examResultFields,
  wasDecided,
  deriveDocumentApproval,
  deriveExamStatus,
  deriveExemptionStatus,
} from './ydyo'

// Bir kaydın üç rozetinin (Belge Onayı / Sınav Sonucu / Muafiyet) mantıken
// tutarlı olup olmadığını tek yerde doğrular. Kullanıcının bildirdiği hata tam da
// bu üçlünün çelişmesiydi (Onaylandı + Sınav Gerekli Değil + Muaf Değil).
const badges = (app) => ({
  doc: deriveDocumentApproval(app),
  exam: deriveExamStatus(app),
  exemption: deriveExemptionStatus(app),
})

describe('initialReviewFields — karar → tüm alanlar tutarlı', () => {
  it('Onaylandı (muaf): ACCEPTED, sınav gerekmez, muaf; eski sınav sonucu temizlenir', () => {
    const f = initialReviewFields(true, false)
    expect(f).toMatchObject({
      status: YDYO_STATUS.ACCEPTED,
      requiresExam: false,
      exemptionApproved: true,
      examScore: null,
      examPassed: null,
    })
    expect(badges(f)).toEqual({ doc: 'APPROVED', exam: 'NOT_REQUIRED', exemption: 'EXEMPT' })
  })

  it('Onaylanmadı: EXAM_PENDING, sınava yönlendirilir', () => {
    const f = initialReviewFields(false, true)
    expect(f).toMatchObject({ status: YDYO_STATUS.EXAM_PENDING, requiresExam: true, exemptionApproved: false })
  })
})

describe('examResultFields — sınav sonucu → tüm alanlar tutarlı', () => {
  it('eşik altı (50): REJECTED, muaf değil, başarısız', () => {
    const f = examResultFields(50, false)
    expect(f).toMatchObject({
      status: YDYO_STATUS.REJECTED,
      requiresExam: true,
      exemptionApproved: false,
      examScore: 50,
      examPassed: false,
    })
    expect(badges(f)).toEqual({ doc: 'NOT_APPROVED', exam: 'FAILED', exemption: 'NOT_EXEMPT' })
  })

  it('eşik üstü (72): ACCEPTED, sınavla muaf, başarılı', () => {
    const f = examResultFields(72, true)
    expect(badges(f)).toEqual({ doc: 'NOT_APPROVED', exam: 'PASSED', exemption: 'EXEMPT' })
  })
})

describe('REGRESYON: Onaylandı iken sonradan 50 girilince çelişkili durum oluşmamalı', () => {
  // Hata senaryosu: kayıt önce "Onaylandı" (muaf) yapılır; sonra aynı kayda
  // "Onaylanmadı + 50 puan" girilir. Eskiden requiresExam/exemptionApproved eski
  // kalıp rozetler çelişiyordu. Artık alanlar BİRLİKTE güncellendiği için
  // çelişki imkansız.
  it('önce muaf, sonra exam 50 → kayıt tek tip REJECTED, rozetler tutarlı', () => {
    // 1) Onaylandı → muaf
    const app = { status: YDYO_STATUS.REVIEW }
    Object.assign(app, initialReviewFields(true, false))
    expect(badges(app)).toEqual({ doc: 'APPROVED', exam: 'NOT_REQUIRED', exemption: 'EXEMPT' })

    // 2) Aynı kayda sınav 50 girildi → tüm alanlar yeniden yazılır
    Object.assign(app, examResultFields(50, false))

    // Çelişkili kombinasyon (Onaylandı + Sınav Gerekli Değil + Muaf Değil) OLUŞMAMALI:
    expect(badges(app)).toEqual({ doc: 'NOT_APPROVED', exam: 'FAILED', exemption: 'NOT_EXEMPT' })
    expect(app.requiresExam).toBe(true)        // eski 'false' eski kalmadı
    expect(app.exemptionApproved).toBe(false)  // eski 'true' eski kalmadı
  })
})

describe('wasDecided — yeniden değerlendirme tespiti', () => {
  it('ACCEPTED/REJECTED kesin karardır → true', () => {
    expect(wasDecided({ status: YDYO_STATUS.ACCEPTED })).toBe(true)
    expect(wasDecided({ status: YDYO_STATUS.REJECTED })).toBe(true)
  })
  it('REVIEW/EXAM_PENDING henüz karar değil → false', () => {
    expect(wasDecided({ status: YDYO_STATUS.REVIEW })).toBe(false)
    expect(wasDecided({ status: YDYO_STATUS.EXAM_PENDING })).toBe(false)
  })
})

describe('mock API — "değişiklik yapılmıştır" damgası (audit)', () => {
  it('İLK karar değişiklik sayılmaz (REVIEW → ACCEPTED): modified yok', async () => {
    // app 71 seed'i REVIEW durumunda.
    const res = await ydyoApi.submitInitialReview(71, {
      approved: true, requiresExam: false, notes: 'belge yeterli', reviewerId: 2, reviewerEmail: 'ydyo@iyte.edu.tr',
    })
    expect(res.status).toBe(YDYO_STATUS.ACCEPTED)
    expect(res.modified).toBeFalsy()
  })

  it('KESİN karara bağlı kaydı değiştirmek damgalanır (ACCEPTED → exam 50)', async () => {
    // app 73 seed'i ACCEPTED (muaf). Üstüne sınav sonucu girmek = yeniden değerlendirme.
    const res = await ydyoApi.submitExamResult(73, {
      examScore: 50, passed: false, notes: 'yanlışlıkla değişti', reviewerId: 2, reviewerEmail: 'ydyo@iyte.edu.tr',
    })
    expect(res.modified).toBe(true)
    expect(res.modifiedBy).toBe('ydyo@iyte.edu.tr')
    expect(res.modifiedAt).toBeTruthy()
    // Damga konsa da kayıt tutarlı kalır:
    expect(badges(res)).toEqual({ doc: 'NOT_APPROVED', exam: 'FAILED', exemption: 'NOT_EXEMPT' })
  })
})
