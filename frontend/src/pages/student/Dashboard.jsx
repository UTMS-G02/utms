import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { Row, Col, Button, Typography, Tag, Spin, App } from 'antd'
import { FileTextOutlined, ClockCircleOutlined } from '@ant-design/icons'
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
    height: '100%',
    display: 'flex',
    flexDirection: 'column',
  },
  cardHeader: {
    display: 'flex',
    alignItems: 'center',
    gap: 10,
    marginBottom: 4,
  },
  cardIcon: {
    fontSize: 20,
    color: '#8B1A2B',
  },
  cardBody: {
    flex: 1,
    marginTop: 16,
  },
  cardFooter: {
    marginTop: 24,
  },
  applicationRow: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    padding: '12px 14px',
    background: '#fafafa',
    borderRadius: 8,
    border: '1px solid #f0f0f0',
  },
  dot: {
    width: 8,
    height: 8,
    borderRadius: '50%',
    background: '#8B1A2B',
    flexShrink: 0,
    marginTop: 4,
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

export default function StudentDashboard() {
  const [loading, setLoading] = useState(true)
  const [application, setApplication] = useState(null)
  const navigate = useNavigate()
  const { message } = App.useApp()

  useEffect(() => {
    applicationsApi.getMyApplications()
      .then((list) => {
        setApplication(list?.[0] ?? null)
      })
      .catch(() => {
        message.error('Başvuru bilgileri yüklenirken bir hata oluştu.')
      })
      .finally(() => {
        setLoading(false)
      })
  }, [])

  const handleApplyClick = () => {
    if (application) {
      navigate(`/student/applications/${application.applicationId}/edit`)
    } else {
      navigate('/student/applications/new')
    }
  }

  if (loading) {
    return (
      <div style={styles.loadingWrap}>
        <Spin size="large" />
        <Text type="secondary">Yükleniyor...</Text>
      </div>
    )
  }

  const statusInfo = application
    ? (STATUS_MAP[application.status] ?? { label: application.status, color: 'default' })
    : null

  return (
    <div style={styles.page}>
      <div style={styles.pageHeader}>
        <Title level={2} style={{ margin: 0, color: '#1f212b', fontWeight: 700 }}>
          Öğrenci Paneli
        </Title>
        <Text type="secondary" style={{ fontSize: 14 }}>
          Yatay geçiş başvurunuzu yönetin ve durumunu takip edin
        </Text>
      </div>

      <Row gutter={[24, 24]}>
        {/* Sol kart — Başvuru Yap */}
        <Col xs={24} md={12}>
          <div style={styles.card}>
            <div>
              <div style={styles.cardHeader}>
                <FileTextOutlined style={styles.cardIcon} />
                <Title level={5} style={{ margin: 0 }}>Başvuru Yap</Title>
              </div>
              <Text type="secondary" style={{ fontSize: 13 }}>
                Mevcut başvurunuzu görüntüleyin veya güncelleyin
              </Text>
            </div>

            <div style={styles.cardBody}>
              <Text style={{ fontSize: 14, color: '#555', lineHeight: 1.6 }}>
                Başvurunuzda değişiklik yapmak için güncelleme hakkınızı kullanabilirsiniz.
                Bu hakkı sadece bir kez kullanabilirsiniz.
              </Text>
            </div>

            <div style={styles.cardFooter}>
              <Button
                type="primary"
                block
                style={{ background: '#8B1A2B', borderColor: '#8B1A2B', height: 40, fontWeight: 600 }}
                onClick={handleApplyClick}
              >
                {application ? 'Başvurumu Güncelle' : 'Yeni Başvuru Oluştur'}
              </Button>
            </div>
          </div>
        </Col>

        {/* Sağ kart — Başvuru Durumu */}
        <Col xs={24} md={12}>
          <div style={styles.card}>
            <div>
              <div style={styles.cardHeader}>
                <ClockCircleOutlined style={styles.cardIcon} />
                <Title level={5} style={{ margin: 0 }}>Başvuru Durumu</Title>
              </div>
              <Text type="secondary" style={{ fontSize: 13 }}>
                Başvurunuzun güncel durumunu görüntüleyin
              </Text>
            </div>

            <div style={styles.cardBody}>
              {application ? (
                <div style={styles.applicationRow}>
                  <div style={{ display: 'flex', alignItems: 'flex-start', gap: 10 }}>
                    <div style={styles.dot} />
                    <div>
                      <Text style={{ fontSize: 14, fontWeight: 500 }}>Yatay Geçiş Başvurusu</Text>
                      <br />
                      <Text type="secondary" style={{ fontSize: 12 }}>
                        Başvuru No: #YG-{application.applicationId}
                      </Text>
                    </div>
                  </div>
                  <Tag color={statusInfo.color} style={{ marginLeft: 8, flexShrink: 0 }}>
                    {statusInfo.label}
                  </Tag>
                </div>
              ) : (
                <Text type="secondary" style={{ fontSize: 14 }}>
                  Henüz başvuru oluşturmadınız.
                </Text>
              )}
            </div>

            {application && (
              <div style={styles.cardFooter}>
                <Button
                  block
                  style={{ height: 40, fontWeight: 600 }}
                  onClick={() => navigate(`/student/applications/${application.applicationId}`)}
                >
                  Başvuru Detaylarını Görüntüle
                </Button>
              </div>
            )}
          </div>
        </Col>
      </Row>
    </div>
  )
}
