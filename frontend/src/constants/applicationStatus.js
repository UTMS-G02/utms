// Backend ApplicationStatus enum'unun BİREBİR karşılığı (krş. ApplicationStatus.java).
// Tag renkleri AntD preset'leri: 'success' | 'error' | 'warning' | 'processing' | 'default'
// ve adlandırılmış renkler ('blue','cyan','gold','geekblue'...).
export const STATUS_META = {
  DRAFT:                  { label: 'Taslak',                         color: 'default'    },
  SUBMITTED:              { label: 'Gönderildi',                     color: 'blue'       },
  WITHDRAWN:              { label: 'Geri Çekildi',                   color: 'default'    },

  OIDB_REVIEW:            { label: 'ÖİDB İncelemesinde',             color: 'processing' },
  REVISION_REQUESTED:     { label: 'Düzeltme Bekliyor',             color: 'warning'    }, // sarı/turuncu
  OIDB_REJECTED:          { label: 'Reddedildi',                     color: 'error'      },

  YDYO_REVIEW:            { label: 'YDYO İncelemesinde',             color: 'processing' },
  YDYO_EXAM_PENDING:      { label: 'Sınav Bekleniyor',               color: 'gold'       },
  YDYO_REJECTED:          { label: 'Reddedildi',                     color: 'error'      },
  YDYO_ACCEPTED:          { label: 'YDYO Onayladı',                  color: 'cyan'       },

  DEAN_OFFICE_REVIEW:     { label: 'Dekanlık İncelemesinde',         color: 'processing' },
  YGK_REVIEW:             { label: 'YGK Değerlendirmesinde',         color: 'processing' },
  YGK_REVIEW_DONE:        { label: 'YGK Değerlendirmesi Tamamlandı', color: 'geekblue'   },
  FACULTY_BOARD_REVIEW:   { label: 'Fakülte Kurulu İncelemesinde',   color: 'processing' },
  FACULTY_BOARD_REJECTED: { label: 'Reddedildi',                     color: 'error'      },
  FACULTY_BOARD_ACCEPTED: { label: 'Fakülte Kurulu Onayladı',        color: 'cyan'       },
  OIDB_FINAL_REVIEW:      { label: 'ÖİDB Son İncelemesinde',         color: 'processing' },

  APPROVED:               { label: 'Onaylandı',                      color: 'success'    },
  ACCEPTED:               { label: 'Onaylandı',                      color: 'success'    }, // güvenlik amaçlı alias
  REJECTED:               { label: 'Reddedildi',                     color: 'error'      },
}

// Bilinmeyen/eksik statüde ham enum'u göstermek yerine güvenli bir varsayılan döndürür.
export const getStatusMeta = (status) =>
  STATUS_META[status] ?? { label: status ?? 'Bilinmiyor', color: 'default' }

export const isRevisionRequested = (status) => status === 'REVISION_REQUESTED'

// Öğrenci, istenen düzeltmeyi yapıp başvuruyu YENİDEN gönderdi mi?
// (bir kez düzeltme istendi VE başvuru tekrar OİDB incelemesine döndü)
export const isResubmittedAfterRevision = (app) =>
  Boolean(app?.revisionRequestedBefore) &&
  (app?.status === 'SUBMITTED' || app?.status === 'OIDB_REVIEW')

// ─── Öğrenci milestone modeli ─────────────────────────────────────────────────
// Öğrenciye gösterilen durum YALNIZCA ÖİDB aksiyonlarını yansıtır. YDYO / YGK /
// Dekanlık / Fakülte gibi iç aşamalar jenerik "Değerlendiriliyor" altında gizlenir.
// YDYO sonucu (yabancı dilden muaf / sınavı geçti), ancak ÖİDB başvuruyu
// değerlendirmeye ilerlettikten (EVALUATION_QUEUE ve sonrası) sonra yüzeye çıkar.

const STUDENT_UNDER_OIDB = ['SUBMITTED', 'OIDB_REVIEW']
// ÖİDB onayladı ama post-YDYO işlemini henüz yapmadı → sonuç gizli, jenerik değerlendiriliyor
const STUDENT_PRE_OIDB_FORWARD = ['YDYO_REVIEW', 'YDYO_EXAM_PENDING', 'YDYO_ACCEPTED', 'YDYO_REJECTED']
// ÖİDB post-YDYO işlemini yaptı → değerlendirme sürecinde (muaf/geçti yüzeye çıktı)
const STUDENT_IN_EVALUATION = [
  'EVALUATION_QUEUE', 'YGK_REVIEW', 'YGK_REVIEW_DONE', 'YGK_SCORED',
  'DEAN_OFFICE_REVIEW', 'DEAN_REVIEW', 'DEAN_REJECTED',
  'FACULTY_BOARD_REVIEW', 'FACULTY_BOARD_ACCEPTED', 'FACULTY_BOARD_REJECTED',
  'OIDB_FINAL_REVIEW', 'FINAL_DEAN_REVIEW',
]
const STUDENT_APPROVED = ['APPROVED', 'ACCEPTED', 'RESULT_PUBLISHED']
const STUDENT_REJECTED = ['OIDB_REJECTED', 'REJECTED'] // yalnızca ÖİDB'nin verdiği red

// Öğrenci için tam milestone: etiket + renk + Güncel Durum alert tipi/metni.
export const getStudentMilestone = (app) => {
  const fallback = { label: 'Bilinmiyor', color: 'default', alertType: 'info', alertText: 'Başvurunuzun durumu güncellenmektedir.' }
  if (!app) return fallback
  const status = app.status

  // Düzeltme yapıp yeniden gönderilen başvuru → sonucu belli değil, "İncelemede"
  if (isResubmittedAfterRevision(app)) {
    return { label: 'İncelemede', color: 'processing', alertType: 'info',
      alertText: 'Başvurunuz Öğrenci İşleri (ÖİDB) tarafından yeniden inceleniyor. Sonuç belirlendiğinde bilgilendirileceksiniz.' }
  }

  if (status === 'DRAFT')
    return { label: 'Taslak', color: 'default', alertType: 'info', alertText: 'Başvurunuz henüz gönderilmemiştir. Tamamlayarak gönderin.' }
  if (status === 'WITHDRAWN')
    return { label: 'Geri Çekildi', color: 'default', alertType: 'info', alertText: 'Başvurunuzu geri çektiniz.' }
  if (status === 'REVISION_REQUESTED')
    return { label: 'Düzeltme Bekliyor', color: 'warning', alertType: 'warning',
      alertText: 'Başvurunuz düzeltme için iade edilmiştir. Yukarıdaki gerekçeyi inceleyip ilgili belgeleri güncelleyin.' }
  if (STUDENT_UNDER_OIDB.includes(status))
    return { label: 'İncelemede', color: 'processing', alertType: 'info',
      alertText: 'Başvurunuz Öğrenci İşleri (ÖİDB) tarafından inceleniyor. Sonuç belirlendiğinde bilgilendirileceksiniz.' }
  if (STUDENT_REJECTED.includes(status))
    return { label: 'Reddedildi', color: 'error', alertType: 'error',
      alertText: app.oidbNotes ? `Başvurunuz reddedilmiştir. Gerekçe: ${app.oidbNotes}` : 'Başvurunuz reddedilmiştir.' }
  if (STUDENT_APPROVED.includes(status))
    return { label: 'Onaylandı', color: 'success', alertType: 'success', alertText: 'Başvurunuz kabul edilmiştir. Tebrikler!' }
  if (STUDENT_PRE_OIDB_FORWARD.includes(status))
    return { label: 'Değerlendiriliyor', color: 'processing', alertType: 'info', alertText: 'Başvurunuz değerlendirme sürecindedir.' }
  if (STUDENT_IN_EVALUATION.includes(status)) {
    const langText =
      app.ydyoApproved === true ? 'Yabancı dil şartından muaf tutuldunuz. '
      : app.ydyoApproved === false ? 'Yabancı dil sınavını başarıyla geçtiniz. '
      : ''
    return { label: 'Değerlendiriliyor', color: 'processing', alertType: 'info',
      alertText: `${langText}Başvurunuz değerlendirme sürecindedir.` }
  }

  // Bilinmeyen statü → güvenli varsayılan
  const meta = getStatusMeta(status)
  return { ...fallback, label: meta.label, color: meta.color }
}

// Öğrenci panelleri için durum etiketi (Tag). Detay + Başvurularım + Ana Sayfa
// hepsi bunu kullanır → tek noktadan ÖİDB-bazlı tutarlı görünüm.
export const getStudentStatusMeta = (app) => {
  const m = getStudentMilestone(app)
  return { label: m.label, color: m.color }
}
