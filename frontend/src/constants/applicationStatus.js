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
