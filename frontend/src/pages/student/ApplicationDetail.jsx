import { useState, useEffect } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { Button, Typography, Tag, Spin, Steps, Alert, Descriptions, Divider, Space, Upload, Tooltip, App } from 'antd'
import { FileTextOutlined, UploadOutlined, LockOutlined } from '@ant-design/icons'
import { applicationsApi } from '../../api/applications'
import { getStatusMeta, isRevisionRequested } from '../../constants/applicationStatus'

const { Title, Text } = Typography

const ALERT_MAP = {
  DRAFT:                'Başvurunuz henüz gönderilmemiştir. Tamamlayarak gönderin.',
  SUBMITTED:            'Başvurunuz başarıyla gönderilmiştir. İnceleme sürecini bekliyorsunuz.',
  OIDB_REVIEW:          'Başvurunuz şu anda akademik kurul tarafından incelenmektedir. Karar verildiğinde e-posta ile bilgilendirileceksiniz.',
  REVISION_REQUESTED:   'Başvurunuz düzeltme için iade edilmiştir. Aşağıdaki gerekçeyi inceleyip ilgili belgeleri güncelleyin.',
  OIDB_REJECTED:        'Başvurunuz reddedilmiştir.',
  APPROVED:             'Başvurunuz kabul edilmiştir. Tebrikler!',
  YDYO_REVIEW:          'Başvurunuz YDYO birimi tarafından incelenmektedir.',
  EVALUATION_QUEUE:     'Başvurunuz değerlendirme kuyruğuna alınmıştır.',
  YGK_SCORED:           'Başvurunuz puanlandı, üst birim onayı bekleniyor.',
  DEAN_REVIEW:          'Başvurunuz dekan tarafından incelenmektedir.',
  FACULTY_BOARD_REVIEW: 'Başvurunuz fakülte kurulunda değerlendirilmektedir.',
  FINAL_DEAN_REVIEW:    'Başvurunuz son dekan incelemesindedir.',
  RESULT_PUBLISHED:     'Başvurunuzun sonucu açıklanmıştır.',
  ACCEPTED:             'Başvurunuz kabul edilmiştir. Tebrikler!',
  REJECTED:             'Başvurunuz reddedilmiştir.',
}

const TERMINAL_STATUSES = ['RESULT_PUBLISHED', 'ACCEPTED', 'REJECTED']

const DOCUMENT_TYPE_LABEL = {
  STUDENT_CERTIFICATE: 'Öğrenci Belgesi',
  TRANSCRIPT: 'Transkript Belgesi',
  YKS_RESULT: 'YKS Sonuç Belgesi',
  LANGUAGE_CERT: 'Yabancı Dil Belgesi',
  ID_CARD: 'Kimlik Belgesi',
  OTHER: 'Diğer Belge',
}

const getStepCurrent = (status) => {
  if (!status || status === 'DRAFT') return 0
  if (TERMINAL_STATUSES.includes(status)) return 3
  return 1
}

const formatDate = (iso) => {
  if (!iso) return '—'
  // GG.AA.YYYY (tr-TR locale '.' ayırıcı kullanır → örn. 03.06.2026)
  return new Intl.DateTimeFormat('tr-TR', { day: '2-digit', month: '2-digit', year: 'numeric' }).format(new Date(iso))
}

const styles = {
  page: {
    fontFamily: "'DM Sans', sans-serif",
  },
  pageHeader: {
    marginBottom: 32,
  },
  card: {
    background: '#ffffff',
    borderRadius: 10,
    padding: 24,
    boxShadow: '0 4px 16px rgba(0,0,0,0.06)',
    border: '1px solid #f0f0f0',
    marginBottom: 24,
  },
  cardSubtitle: {
    display: 'block',
    marginBottom: 24,
  },
  documentRow: {
    display: 'flex',
    alignItems: 'center',
    gap: 10,
    padding: '10px 12px',
    background: '#fafafa',
    border: '1px solid #f0f0f0',
    borderRadius: 8,
    marginBottom: 8,
  },
  footer: {
    display: 'flex',
    justifyContent: 'flex-end',
    gap: 12,
    marginTop: 8,
  },
  loadingWrap: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    minHeight: 300,
    gap: 12,
  },
}

const stepItems = [
  { title: 'Gönderildi' },
  { title: 'İnceleniyor' },
  { title: 'Sonuçlandı' },
]

export default function ApplicationDetail() {
  const [loading, setLoading] = useState(true)
  const [application, setApplication] = useState(null)
  const [reuploading, setReuploading] = useState(false)
  const [pendingFiles, setPendingFiles] = useState({}) // { [documentType]: File }
  const navigate = useNavigate()
  const { id } = useParams()
  const { message } = App.useApp()

  useEffect(() => {
    applicationsApi.getApplicationById(id)
      .then((data) => {
        setApplication(data ?? null)
      })
      .catch(() => {
        message.error('Başvuru detayı yüklenirken bir hata oluştu.')
      })
      .finally(() => {
        setLoading(false)
      })
  }, [id])

  if (loading) {
    return (
      <div style={styles.loadingWrap}>
        <Spin size="large" />
        <Text type="secondary">Yükleniyor...</Text>
      </div>
    )
  }

  if (!application) {
    return (
      <div style={styles.page}>
        <Text type="secondary">Başvuru bulunamadı.</Text>
        <div style={{ marginTop: 16 }}>
          <Button onClick={() => navigate('/student/applications')}>
            Başvurularıma Dön
          </Button>
        </div>
      </div>
    )
  }

  const {
    id: applicationId,            // DTO alanı 'id' (eski kodda 'applicationId' yoktu → undefined idi)
    status,
    submissionDate,               // DTO alanı 'submissionDate' (eski 'submittedAt' yanlıştı)
    studentName,
    email,                        // gerçek öğrenci e-postası (backend ApplicationResponse)
    targetDepartment,
    currentUniversity,            // YÖKSİS'ten (mock) — başvuru anında kaydedildi
    currentDepartment,
    gpa,
    oidbNotes,                    // eski genel ÖİDB notu (geriye dönük yedek)
    ydyoNotes,
    requestedDocumentType,        // OİDB'nin düzeltme istediği belge tipi (örn: TRANSCRIPT)
    revisionNotes,                // OİDB'nin düzeltme notu (öncelikli gerekçe)
    documents = [],
  } = application
  const alertText = ALERT_MAP[status] ?? 'Başvurunuzun durumu güncellenmektedir.'
  const stepCurrent = getStepCurrent(status)
  const statusMeta = getStatusMeta(status)

  const revisionMode = isRevisionRequested(status)
  const revisionNote = revisionNotes || oidbNotes || ydyoNotes || 'Düzeltme gerekçesi belirtilmemiştir. Lütfen birimle iletişime geçin.'
  const requestedDocLabel = requestedDocumentType
    ? (DOCUMENT_TYPE_LABEL[requestedDocumentType] ?? requestedDocumentType)
    : null
  // OİDB belirli bir belge seçtiyse yalnızca o belge düzenlenebilir; seçmediyse (geriye dönük) hepsi açık.
  const isDocEditable = (docType) => !requestedDocumentType || docType === requestedDocumentType

  const beforeReupload = (documentType, file) => {
    if (file.type !== 'application/pdf') { message.error('Sadece PDF yüklenebilir.'); return Upload.LIST_IGNORE }
    if (file.size > 10 * 1024 * 1024) { message.error("Dosya 10MB'dan büyük olamaz."); return Upload.LIST_IGNORE }
    setPendingFiles((prev) => ({ ...prev, [documentType]: file }))
    return false // otomatik upload'ı engelle; gönderimi biz yapacağız
  }

  const handleReupload = async () => {
    const entries = Object.entries(pendingFiles).filter(([, f]) => f)
    if (entries.length === 0) {
      message.warning('Lütfen güncellemek istediğiniz en az bir belgeyi seçin.')
      return
    }
    setReuploading(true)
    try {
      for (const [documentType, file] of entries) {
        await applicationsApi.uploadDocument(applicationId, file, documentType)
      }
      await applicationsApi.submitApplication(applicationId) // düzeltmeyi tekrar gönder
      message.success('Belgeleriniz güncellendi ve başvurunuz yeniden gönderildi.')
      const fresh = await applicationsApi.getApplicationById(applicationId)
      setApplication(fresh)
      setPendingFiles({})
    } catch (err) {
      message.error(err?.response?.data?.message || 'Belgeler güncellenirken bir hata oluştu.')
    } finally {
      setReuploading(false)
    }
  }

  const descriptionItems = [
    {
      key: 'applicationId',
      label: 'Başvuru Numarası',
      children: <Text style={{ fontWeight: 500 }}>#YG-{applicationId}</Text>,
    },
    {
      key: 'currentDept',
      label: 'Mevcut Bölüm',
      children: currentDepartment ? <Tag>{currentDepartment}</Tag> : '—',
    },
    {
      key: 'studentName',
      label: 'Ad Soyad',
      children: studentName ?? '—',
    },
    {
      key: 'targetDept',
      label: 'Hedef Bölüm',
      children: (
        <Tag style={{ background: '#8B1A2B', color: '#fff', border: 'none' }}>
          {targetDepartment}
        </Tag>
      ),
    },
    {
      key: 'gpa',
      label: 'Genel Not Ortalaması',
      children: gpa != null ? Number(gpa).toFixed(2) : '—',
    },
    {
      key: 'email',
      label: 'E-posta',
      children: email ?? '—',
    },
    {
      key: 'university',
      label: 'Üniversite',
      children: currentUniversity ?? '—',
    },
    {
      key: 'submittedAt',
      label: 'Başvuru Tarihi',
      children: `📅 ${formatDate(submissionDate)}`,
    },
  ]

  return (
    <div style={styles.page}>
      <div style={styles.pageHeader}>
        <Title level={2} style={{ margin: 0, color: '#1f212b', fontWeight: 700 }}>
          Başvuru Takibi
        </Title>
        <Text type="secondary" style={{ fontSize: 14 }}>
          Yatay geçiş başvurunuzun detaylı durumunu ve zaman çizelgesini görüntüleyin
        </Text>
      </div>

      {/* Düzeltme / Evrak İadesi banner'ı — yalnızca REVISION_REQUESTED iken */}
      {revisionMode && (
        <Alert
          type="warning"
          showIcon
          banner
          style={{ marginBottom: 24, borderRadius: 8 }}
          message={<strong>Düzeltme / Evrak İadesi Gerekiyor</strong>}
          description={
            <span>
              {requestedDocLabel ? (
                <>
                  Başvurunuzda <Text strong style={{ color: '#ad6800' }}>{requestedDocLabel}</Text> belgesinin
                  düzeltilmesi isteniyor. Yalnızca bu belgeyi yeniden yükleyip başvurunuzu tekrar gönderin:
                </>
              ) : (
                <>Başvurunuz aşağıdaki gerekçeyle düzeltmeye gönderildi. İlgili belgeleri güncelleyip yeniden gönderin:</>
              )}
              <br />
              <Text strong style={{ color: '#ad6800' }}>"{revisionNote}"</Text>
            </span>
          }
        />
      )}

      {/* Durum kartı */}
      <div style={styles.card}>
        <Space align="center" style={{ marginBottom: 4 }}>
          <Title level={5} style={{ margin: 0 }}>Başvuru Durumu</Title>
          <Tag color={statusMeta.color}>{statusMeta.label}</Tag>
        </Space>
        <Text type="secondary" style={styles.cardSubtitle}>
          Geçiş talebinizin sürecini takip edin
        </Text>

        <Steps
          current={stepCurrent}
          items={stepItems}
          style={{ marginBottom: 24 }}
        />

        <Alert
          type="info"
          showIcon
          message={
            <span>
              <strong>Güncel Durum: </strong>{alertText}
            </span>
          }
        />
      </div>

      {/* Detay kartı */}
      <div style={styles.card}>
        <Title level={5} style={{ marginTop: 0, marginBottom: 4 }}>Başvuru Detayları</Title>
        <Text type="secondary" style={styles.cardSubtitle}>
          Bu geçiş talebi hakkında tam bilgi
        </Text>

        <Descriptions
          column={2}
          layout="vertical"
          colon={false}
          items={descriptionItems}
        />

        <Divider />

        <Text type="secondary" style={{ fontSize: 12, display: 'block', marginBottom: 12 }}>
          Yüklenen Belgeler
        </Text>

        {documents.map((doc) => {
          const editable = isDocEditable(doc.documentType)
          return (
            <div
              key={doc.documentId}
              style={{
                ...styles.documentRow,
                // İstenen belgeyi görsel olarak öne çıkar
                ...(revisionMode && editable
                  ? { borderColor: '#ad6800', background: '#fffbe6' }
                  : {}),
              }}
            >
              <FileTextOutlined style={{ color: '#8B1A2B', fontSize: 16 }} />
              <Text style={{ flex: 1 }}>
                {pendingFiles[doc.documentType]?.name ?? doc.fileName}
                <Text type="secondary" style={{ marginLeft: 8, fontSize: 12 }}>
                  ({DOCUMENT_TYPE_LABEL[doc.documentType] ?? 'Belge'})
                </Text>
              </Text>

              {/* Yalnızca REVISION_REQUESTED iken ve OİDB'nin seçtiği belge için kilit açılır */}
              {revisionMode && (
                editable ? (
                  <Upload
                    accept=".pdf"
                    maxCount={1}
                    showUploadList={false}
                    beforeUpload={(file) => beforeReupload(doc.documentType, file)}
                  >
                    <Button size="small" icon={<UploadOutlined />}>
                      {pendingFiles[doc.documentType] ? 'Değiştir' : 'Yeniden Yükle'}
                    </Button>
                  </Upload>
                ) : (
                  <Tooltip title="Bu belge için düzeltme istenmedi, yeniden yüklenemez.">
                    <Button size="small" icon={<LockOutlined />} disabled>
                      Kilitli
                    </Button>
                  </Tooltip>
                )
              )}
            </div>
          )
        })}
      </div>

      <div style={styles.footer}>
        <Space>
          <Button onClick={() => navigate('/student/dashboard')}>
            Ana Sayfaya Dön
          </Button>
          <Button onClick={() => navigate('/student/applications')}>
            Başvurularımı Görüntüle
          </Button>
          {revisionMode && (
            <Button
              type="primary"
              loading={reuploading}
              onClick={handleReupload}
              style={{ background: '#8B1A2B', borderColor: '#8B1A2B', fontWeight: 600 }}
            >
              Belgeleri Güncelle ve Yeniden Gönder
            </Button>
          )}
        </Space>
      </div>
    </div>
  )
}
