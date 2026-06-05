import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { Row, Col, Button, Typography, Tag, Spin, App } from 'antd'
import { FileTextOutlined, ClockCircleOutlined } from '@ant-design/icons'
import { applicationsApi } from '../../api/applications'
import { getStudentStatusMeta } from '../../constants/applicationStatus'

const { Title, Text } = Typography

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

  // Sol kart yalnızca AKSİYON kartı: yeni başvuru oluştur veya taslağı tamamla.
  // İkisi de başvuru formuna gider. Gönderilmiş başvuruyu "görüntüleme" işi sağ
  // karta (Başvuru Durumu) bırakıldı; burada tekrar buton yok.
  const handleApplyClick = () => {
    navigate('/student/applications/new')
  }

  // Sol kartta aksiyon var mı? (yalnızca hiç başvuru yokken ya da taslakken)
  const hasAction = !application || application.status === 'DRAFT'

  if (loading) {
    return (
      <div style={styles.loadingWrap}>
        <Spin size="large" />
        <Text type="secondary">Yükleniyor...</Text>
      </div>
    )
  }

  // Durum etiketi/rengi, Başvurularım sayfasıyla aynı ortak kaynaktan gelir
  // (constants/applicationStatus) — böylece her statü (ör. REVISION_REQUESTED →
  // "Düzeltme Bekliyor") tutarlı ve eksiksiz gösterilir.
  const statusInfo = application ? getStudentStatusMeta(application) : null

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
                {!application
                  ? 'Yatay geçiş başvurunuzu başlatmak için tıklayın.'
                  : application.status === 'DRAFT'
                    ? 'Taslak halindeki başvurunuzu tamamlayın ve gönderin.'
                    : 'Bu dönem için başvurunuz alındı. Durumunu sağdaki "Başvuru Durumu" kartından takip edebilirsiniz.'}
              </Text>
            </div>

            {hasAction && (
              <div style={styles.cardFooter}>
                <Button
                  type="primary"
                  block
                  style={{ background: '#8B1A2B', borderColor: '#8B1A2B', height: 40, fontWeight: 600 }}
                  onClick={handleApplyClick}
                >
                  {!application ? 'Yeni Başvuru Oluştur' : 'Başvurumu Tamamla'}
                </Button>
              </div>
            )}
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
                        Başvuru No: #YG-{application.id}
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
                  onClick={() => navigate(`/student/applications/${application.id}`)}
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
