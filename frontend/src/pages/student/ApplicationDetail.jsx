import { useState, useEffect } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { Button, Typography, Tag, Spin, Steps, Alert, Descriptions, Divider, Space, App } from 'antd'
import { FileTextOutlined } from '@ant-design/icons'
import { applicationsApi } from '../../api/applications'

const { Title, Text } = Typography

const STATUS_MAP = {
  DRAFT:                { label: 'Taslak',                        color: 'default'  },
  SUBMITTED:            { label: 'Gönderildi',                    color: 'blue'     },
  OIDB_REVIEW:          { label: 'OIDB İncelemesinde',            color: 'orange'   },
  YDYO_REVIEW:          { label: 'YDYO İncelemesinde',            color: 'orange'   },
  EVALUATION_QUEUE:     { label: 'Değerlendirme Kuyruğunda',      color: 'cyan'     },
  YGK_SCORED:           { label: 'YGK Puanlandı',                 color: 'geekblue' },
  DEAN_REVIEW:          { label: 'Dekan İncelemesinde',           color: 'orange'   },
  FACULTY_BOARD_REVIEW: { label: 'Fakülte Kurulu İncelemesinde',  color: 'orange'   },
  FINAL_DEAN_REVIEW:    { label: 'Final Dekan İncelemesinde',     color: 'orange'   },
  RESULT_PUBLISHED:     { label: 'Sonuç Yayınlandı',              color: 'green'    },
  ACCEPTED:             { label: 'Kabul Edildi',                  color: 'success'  },
  REJECTED:             { label: 'Reddedildi',                    color: 'error'    },
}

const ALERT_MAP = {
  DRAFT:                'Başvurunuz henüz gönderilmemiştir. Tamamlayarak gönderin.',
  SUBMITTED:            'Başvurunuz başarıyla gönderilmiştir. İnceleme sürecini bekliyorsunuz.',
  OIDB_REVIEW:          'Başvurunuz şu anda akademik kurul tarafından incelenmektedir. Karar verildiğinde e-posta ile bilgilendirileceksiniz.',
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

const getStepCurrent = (status) => {
  if (!status || status === 'DRAFT') return 0
  if (TERMINAL_STATUSES.includes(status)) return 3
  return 1
}

const formatDate = (iso) => {
  if (!iso) return '—'
  return new Intl.DateTimeFormat('tr-TR', { day: 'numeric', month: 'long', year: 'numeric' }).format(new Date(iso))
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

  const { applicationId, status, submittedAt, studentName, targetDepartment, documents = [], statusHistory = [] } = application
  const lastHistory = statusHistory[statusHistory.length - 1]
  const alertText = ALERT_MAP[status] ?? 'Başvurunuzun durumu güncellenmektedir.'
  const stepCurrent = getStepCurrent(status)

  // TODO: backend'e bu alanlar eklenince güncellenecek
  const HARDCODED_CURRENT_DEPT = 'Fen Bilimleri'
  const HARDCODED_STUDENT_NO = 'STU-2024-12345'
  const HARDCODED_GPA = '3.85'
  const HARDCODED_EMAIL = 'ahmet.yilmaz@ogrenci.iyte.edu.tr'
  const HARDCODED_UNIVERSITY = 'İzmir Yüksek Teknoloji Enstitüsü'
  const HARDCODED_GRADE = '2. Sınıf'

  const descriptionItems = [
    {
      key: 'applicationId',
      label: 'Başvuru Numarası',
      children: <Text style={{ fontWeight: 500 }}>#YG-{applicationId}</Text>,
    },
    {
      key: 'currentDept',
      label: 'Mevcut Bölüm',
      children: <Tag>{HARDCODED_CURRENT_DEPT}</Tag>,
    },
    {
      key: 'studentNo',
      label: 'Öğrenci Numarası',
      children: HARDCODED_STUDENT_NO,
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
      key: 'studentName',
      label: 'Ad Soyad',
      children: studentName,
    },
    {
      key: 'gpa',
      label: 'Genel Not Ortalaması',
      children: HARDCODED_GPA,
    },
    {
      key: 'email',
      label: 'E-posta',
      children: HARDCODED_EMAIL,
    },
    {
      key: 'submittedAt',
      label: 'Başvuru Tarihi',
      children: `📅 ${formatDate(submittedAt)}`,
    },
    {
      key: 'university',
      label: 'Üniversite',
      children: HARDCODED_UNIVERSITY,
    },
    {
      key: 'lastUpdate',
      label: 'Son Güncelleme',
      children: `📅 ${formatDate(lastHistory?.changedAt)}`,
    },
    {
      key: 'grade',
      label: 'Sınıf',
      children: HARDCODED_GRADE,
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

      {/* Durum kartı */}
      <div style={styles.card}>
        <Title level={5} style={{ marginTop: 0, marginBottom: 4 }}>Başvuru Durumu</Title>
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

        {documents.map((doc) => (
          <div key={doc.documentId} style={styles.documentRow}>
            <FileTextOutlined style={{ color: '#8B1A2B', fontSize: 16 }} />
            <Text>{doc.fileName}</Text>
          </div>
        ))}
      </div>

      <div style={styles.footer}>
        <Space>
          <Button onClick={() => navigate('/student/dashboard')}>
            Ana Sayfaya Dön
          </Button>
          <Button onClick={() => navigate('/student/applications')}>
            Başvurularımı Görüntüle
          </Button>
        </Space>
      </div>
    </div>
  )
}
