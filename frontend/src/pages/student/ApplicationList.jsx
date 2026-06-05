import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { Button, Typography, Tag, Spin, Table, Empty, App } from 'antd'
import { applicationsApi } from '../../api/applications'
import { getStudentStatusMeta } from '../../constants/applicationStatus'

const { Title, Text } = Typography

const formatDate = (iso) => {
  if (!iso) return 'Gönderilmedi'
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
      dataIndex: 'id',
      key: 'id',
      render: (id) => <Text style={{ fontWeight: 500 }}>#YG-{id}</Text>,
    },
    {
      title: 'Akademik Yıl',
      dataIndex: 'academicYear',
      key: 'academicYear',
      render: (val) => val ?? '—',
    },
    {
      title: 'Hedef Bölüm',
      dataIndex: 'targetDepartment',
      key: 'targetDepartment',
    },
    {
      title: 'Başvuru Tarihi',
      dataIndex: 'submissionDate',
      key: 'submissionDate',
      render: (val) => (
        <Text type={val ? undefined : 'secondary'}>{formatDate(val)}</Text>
      ),
    },
    {
      title: 'Durum',
      dataIndex: 'status',
      key: 'status',
      render: (_, record) => {
        const info = getStudentStatusMeta(record)
        return <Tag color={info.color}>{info.label}</Tag>
      },
    },
    {
      title: 'İşlemler',
      key: 'actions',
      render: (_, record) => (
        <Button onClick={() => navigate(`/student/applications/${record.id}`)}>
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
            rowKey="id"
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
