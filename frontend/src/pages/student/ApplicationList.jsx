import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { Button, Typography, Tag, Spin, Table, Empty, App } from 'antd'
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

const formatDate = (iso) => {
  if (!iso) return 'Gönderilmedi'
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
  },
  footer: {
    marginTop: 24,
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

export default function ApplicationList() {
  const [loading, setLoading] = useState(true)
  const [applications, setApplications] = useState([])
  const navigate = useNavigate()
  const { message } = App.useApp()

  useEffect(() => {
    applicationsApi.getMyApplications()
      .then((list) => {
        setApplications(list ?? [])
      })
      .catch(() => {
        message.error('Başvurular yüklenirken bir hata oluştu.')
      })
      .finally(() => {
        setLoading(false)
      })
  }, [])

  const columns = [
    {
      title: 'Başvuru No',
      dataIndex: 'applicationId',
      key: 'applicationId',
      render: (id) => <Text style={{ fontWeight: 500 }}>#YG-{id}</Text>,
    },
    {
      title: 'Hedef Bölüm',
      dataIndex: 'targetDepartment',
      key: 'targetDepartment',
    },
    {
      title: 'Başvuru Tarihi',
      dataIndex: 'submittedAt',
      key: 'submittedAt',
      render: (val) => (
        <Text type={val ? undefined : 'secondary'}>{formatDate(val)}</Text>
      ),
    },
    {
      title: 'Durum',
      dataIndex: 'status',
      key: 'status',
      render: (status) => {
        const info = STATUS_MAP[status] ?? { label: status, color: 'default' }
        return <Tag color={info.color}>{info.label}</Tag>
      },
    },
    {
      title: 'İşlemler',
      key: 'actions',
      render: (_, record) => (
        <Button onClick={() => navigate(`/student/applications/${record.applicationId}`)}>
          Detayları Görüntüle
        </Button>
      ),
    },
  ]

  if (loading) {
    return (
      <div style={styles.loadingWrap}>
        <Spin size="large" />
        <Text type="secondary">Yükleniyor...</Text>
      </div>
    )
  }

  return (
    <div style={styles.page}>
      <div style={styles.pageHeader}>
        <Title level={2} style={{ margin: 0, color: '#1f212b', fontWeight: 700 }}>
          Başvurularım
        </Title>
        <Text type="secondary" style={{ fontSize: 14 }}>
          Yatay geçiş başvurunuzu görüntüleyin ve yönetin
        </Text>
      </div>

      <div style={styles.card}>
        <Title level={5} style={{ marginTop: 0, marginBottom: 16 }}>Başvuru Geçmişi</Title>

        {applications.length === 0 ? (
          <Empty
            image={Empty.PRESENTED_IMAGE_SIMPLE}
            description="Henüz başvuru oluşturmadınız."
          >
            <Button
              type="primary"
              style={{ background: '#8B1A2B', borderColor: '#8B1A2B' }}
              onClick={() => navigate('/student/applications/new')}
            >
              Yeni Başvuru Oluştur
            </Button>
          </Empty>
        ) : (
          <Table
            columns={columns}
            dataSource={applications}
            rowKey="applicationId"
            pagination={false}
            size="middle"
            bordered={false}
          />
        )}
      </div>

      <div style={styles.footer}>
        <Button onClick={() => navigate('/student/dashboard')}>
          Ana Sayfaya Dön
        </Button>
      </div>
    </div>
  )
}
