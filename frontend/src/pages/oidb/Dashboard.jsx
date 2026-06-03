import { useEffect, useMemo, useState } from 'react'
import { useLocation } from 'react-router-dom'
import {
  App,
  Button,
  Col,
  Divider,
  Empty,
  Input,
  Row,
  Select,
  Space,
  Spin,
  Tag,
  Timeline,
  Tabs,
  Typography,
} from 'antd'
import {
  DownOutlined,
  DownloadOutlined,
  LogoutOutlined,
  UpOutlined,
  ShareAltOutlined,
} from '@ant-design/icons'
import { applicationsApi } from '../../api/applications'
import { useAuth } from '../../contexts/AuthContext'

const { Title, Text } = Typography
const { Search } = Input
const { Option } = Select

const STATUS_MAP = {
  ALL: { label: 'Tümü' },
  OIDB_REVIEW: { label: 'İnceleniyor', color: 'blue' },
  REQUEST_UPDATE: { label: 'Güncelleme Gerekli', color: 'volcano' },
  YDYO_REVIEW: { label: "YDYO'da", color: 'purple' },
  YDYO_APPROVED: { label: "YDYO Onaylı", color: 'green' },
  YDYO_REJECTED: { label: "YDYO Reddetti", color: 'magenta' },
  FACULTY_REVIEW: { label: 'Fakültede', color: 'gold' },
  ACCEPTED: { label: 'Onaylandı', color: 'success' },
  REJECTED: { label: 'Reddedildi', color: 'error' },
}

const STATUS_OPTIONS = [
  { value: 'ALL', label: 'Tümü' },
  { value: 'OIDB_REVIEW', label: 'İnceleniyor' },
  { value: 'REQUEST_UPDATE', label: 'Güncelleme Gerekli' },
  { value: 'YDYO_REVIEW', label: "YDYO'da" },
  { value: 'YDYO_APPROVED', label: "YDYO Onaylı" },
  { value: 'YDYO_REJECTED', label: "YDYO Reddetti" },
  { value: 'FACULTY_REVIEW', label: 'Fakültede' },
  { value: 'ACCEPTED', label: 'Onaylandı' },
  { value: 'REJECTED', label: 'Reddedildi' },
]

const FACULTY_OPTIONS = [
  { value: 'ALL', label: 'Tüm Fakülteler' },
  { value: 'Mühendislik Fakültesi', label: 'Mühendislik Fakültesi' },
  { value: 'Fen Fakültesi', label: 'Fen Fakültesi' },
  { value: 'Mimarlık Fakültesi', label: 'Mimarlık Fakültesi' },
]

const ANNOUNCEABLE_STATUSES = ['ACCEPTED', 'REJECTED']

const DOCUMENT_TYPE_LABEL = {
  TRANSCRIPT: 'Transkript Belgesi',
  ID_CARD: 'Kimlik Belgesi',
  LANGUAGE_CERT: 'Yabancı Dil Belgesi',
  OTHER: 'Diğer Belge',
}

const ACTION_CONFIG = {
  OIDB_REVIEW: [
    { key: 'request_update', label: 'Güncelleme İste', type: 'default' },
    { key: 'send_ydyo', label: "YDYO'ya Gönder", type: 'primary' },
  ],
  YDYO_APPROVED: [
    { key: 'forward_to_faculty', label: 'Fakülteye Gönder', type: 'primary' },
  ],
  YDYO_REJECTED: [
    { key: 'reject_application', label: 'Başvuruyu Reddet', type: 'default', danger: true },
  ],
  ACCEPTED: [
    { key: 'share_result', label: 'Sonucu Paylaş', type: 'primary' },
  ],
  REJECTED: [
    { key: 'share_result', label: 'Sonucu Paylaş', type: 'primary' },
  ],
}

const styles = {
  page: {
    fontFamily: "'DM Sans', sans-serif",
  },
  headerBar: {
    background: '#ffffff',
    borderRadius: 20,
    padding: 24,
    boxShadow: '0 10px 18px rgba(0, 0, 0, 0.05)',
    marginBottom: 24,
  },
  filterBar: {
    background: '#ffffff',
    borderRadius: 20,
    padding: 24,
    boxShadow: '0 10px 18px rgba(0, 0, 0, 0.05)',
    marginBottom: 24,
  },
  selectBox: {
    width: '100%',
    maxWidth: 320,
  },
  card: {
    background: '#ffffff',
    borderRadius: 20,
    padding: 24,
    marginBottom: 16,
    boxShadow: '0 8px 18px rgba(0, 0, 0, 0.04)',
    border: '1px solid #f0f0f0',
  },
  sectionRow: {
    display: 'grid',
    gridTemplateColumns: 'repeat(3, minmax(0, 1fr))',
    gap: 16,
    marginTop: 24,
  },
  summaryDetail: {
    color: 'var(--color-text-secondary, #6b7280)',
    fontSize: 12,
    marginBottom: 6,
    display: 'flex',
    alignItems: 'center',
  },
  summaryValue: {
    fontWeight: 600,
    fontSize: 14,
  },
  actionBox: {
    border: '1px dashed #d9d9d9',
    borderRadius: 16,
    padding: 18,
    marginTop: 16,
  },
  documentRow: {
    display: 'grid',
    gridTemplateColumns: '1fr auto',
    gap: 12,
    alignItems: 'center',
    padding: '16px 0',
    borderBottom: '1px solid #f0f0f0',
  },
  documentMeta: {
    display: 'flex',
    flexDirection: 'column',
    gap: 4,
  },
}

const formatDate = (iso) => {
  if (!iso) return '-'
  return new Intl.DateTimeFormat('tr-TR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  }).format(new Date(iso))
}

const formatSearchMatch = (application, searchQuery) => {
  if (!searchQuery) return true
  const normalized = searchQuery.trim().toLowerCase()
  return [
    application.studentName,
    application.studentEmail,
    application.studentPhone,
    application.currentDepartment,
    application.currentUniversity,
    application.targetDepartment,
    application.targetFaculty,
  ]
    .filter(Boolean)
    .some((value) => value.toLowerCase().includes(normalized))
    || String(application.applicationId).includes(normalized)
}

export default function OidbDashboard() {
  const { message: antdMessage } = App.useApp()
  const { logout } = useAuth()
  const location = useLocation()
  const isPendingRoute = location.pathname === '/oidb/pending'

  const [loading, setLoading] = useState(true)
  const [applications, setApplications] = useState([])
  const [expandedIds, setExpandedIds] = useState([])
  const [details, setDetails] = useState({})
  const [selectedIds, setSelectedIds] = useState([])
  const [statusFilter, setStatusFilter] = useState(isPendingRoute ? 'OIDB_REVIEW' : 'ALL')
  const [facultyFilter, setFacultyFilter] = useState('ALL')
  const [searchTerm, setSearchTerm] = useState('')
  const [activeTabs, setActiveTabs] = useState({})

  useEffect(() => {
    setStatusFilter(isPendingRoute ? 'OIDB_REVIEW' : 'ALL')
  }, [isPendingRoute])

  useEffect(() => {
    setLoading(true)
    applicationsApi.getOidbApplications()
      .then((list) => setApplications(list ?? []))
      .catch(() => {
        antdMessage.error('Başvurular yüklenirken bir hata oluştu.')
      })
      .finally(() => setLoading(false))
  }, [])

  const filteredApplications = useMemo(() => {
    return applications.filter((application) => {
      const matchesStatus = statusFilter === 'ALL' || application.status === statusFilter
      const matchesFaculty = facultyFilter === 'ALL' || application.targetFaculty === facultyFilter
      const matchesSearch = formatSearchMatch(application, searchTerm)
      return matchesStatus && matchesFaculty && matchesSearch
    })
  }, [applications, facultyFilter, searchTerm, statusFilter])

  const chooseableApplications = useMemo(
    () => filteredApplications.filter((item) => ANNOUNCEABLE_STATUSES.includes(item.status)),
    [filteredApplications],
  )

  const isAllSelected = chooseableApplications.length > 0 && chooseableApplications.every((item) => selectedIds.includes(item.applicationId))

  const handleSelectAll = () => {
    if (isAllSelected) {
      setSelectedIds([])
      return
    }
    setSelectedIds(chooseableApplications.map((item) => item.applicationId))
  }

  const handleToggleExpand = async (applicationId) => {
    const isExpanded = expandedIds.includes(applicationId)
    if (isExpanded) {
      setExpandedIds(expandedIds.filter((id) => id !== applicationId))
      return
    }

    if (!details[applicationId]) {
      antdMessage.loading({ content: 'Detaylar yükleniyor...', key: `loading-${applicationId}` })
      try {
        const detail = await applicationsApi.getApplicationById(applicationId)
        setDetails((prev) => ({ ...prev, [applicationId]: detail }))
        antdMessage.destroy(`loading-${applicationId}`)
      } catch (error) {
        antdMessage.error('Başvuru detayları alınamadı.')
      }
    }

    setExpandedIds([...expandedIds, applicationId])
  }

  const handleAction = async (applicationId, actionKey) => {
    try {
      let result = null
      if (actionKey === 'request_update') result = await applicationsApi.requestApplicationUpdate(applicationId)
      if (actionKey === 'send_ydyo') result = await applicationsApi.sendToYdyo(applicationId)
      if (actionKey === 'forward_to_faculty') result = await applicationsApi.forwardToFaculty(applicationId)
      if (actionKey === 'reject_application') result = await applicationsApi.rejectApplication(applicationId)
      if (actionKey === 'share_result') result = await applicationsApi.shareResults([applicationId])

      if (result?.success !== false) {
        antdMessage.success('İşlem başarılı.')
      } else {
        throw new Error('Action failed')
      }
    } catch (error) {
      antdMessage.error('İşlem gerçekleştirilirken bir hata oluştu.')
    }
  }

  const handleBulkShare = async () => {
    if (selectedIds.length === 0) return
    try {
      await applicationsApi.shareResults(selectedIds)
      antdMessage.success('Sonuç paylaşımı başarılı.')
      setSelectedIds([])
    } catch {
      antdMessage.error('Sonuç paylaşımı sırasında bir hata oluştu.')
    }
  }

  const handleDownloadDocument = (documentId) => {
    const downloadUrl = `/api/documents/${documentId}/download`
    window.open(downloadUrl, '_blank')
  }

  const renderActionButtons = (status, applicationId) => {
    const actions = ACTION_CONFIG[status] ?? []
    if (actions.length === 0) {
      return <Text type="secondary">Bu durumda yapılacak işlem yok.</Text>
    }

    return (
      <Space wrap>
        {actions.map((action) => (
          <Button
            key={action.key}
            type={action.type}
            danger={action.danger}
            onClick={() => handleAction(applicationId, action.key)}
          >
            {action.label}
          </Button>
        ))}
      </Space>
    )
  }

  if (loading) {
    return (
      <div style={{ minHeight: 360, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
        <Spin size="large" />
      </div>
    )
  }

  return (
    <div style={styles.page}>
      <div style={styles.filterBar}>
        <Row gutter={[16, 16]} align="middle">
          <Col xs={24} lg={10}>
            <Space direction="vertical" style={{ width: '100%' }}>
              <Text strong>Öğrenci Ara:</Text>
              <Search
                placeholder="Ad, Soyad veya Numara"
                allowClear
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                enterButton="Ara"
              />
            </Space>
          </Col>
          <Col xs={24} sm={12} lg={6}>
            <Text strong>Başvuru Durumu:</Text>
            <Select
              value={statusFilter}
              onChange={setStatusFilter}
              style={styles.selectBox}
            >
              {STATUS_OPTIONS.map((option) => (
                <Option key={option.value} value={option.value}>{option.label}</Option>
              ))}
            </Select>
          </Col>
          <Col xs={24} sm={12} lg={6}>
            <Text strong>Fakülte:</Text>
            <Select
              value={facultyFilter}
              onChange={setFacultyFilter}
              style={styles.selectBox}
            >
              {FACULTY_OPTIONS.map((option) => (
                <Option key={option.value} value={option.value}>{option.label}</Option>
              ))}
            </Select>
          </Col>
          <Row xs={24} sm={24} lg={2} style={{ display: 'flex', alignItems: 'flex-end' }}>
            <Text style={{ display: 'block', fontWeight: 600 }}>{filteredApplications.length} başvuru gösteriliyor</Text>
          </Row>
        </Row>
      </div>

      <div style={styles.card}>
        <Row align="middle" justify="space-between" wrap={false} style={{ gap: 16 }}>
          <Col>
            <Button
              type={isAllSelected ? 'default' : 'primary'}
              onClick={handleSelectAll}
            >
              {isAllSelected
                ? 'Seçimi Temizle'
                : `Tümünü seç (${chooseableApplications.length})`}
            </Button>
          </Col>
          <Col>
            <Text type="secondary">
              {selectedIds.length > 0
                ? `${selectedIds.length} başvuru seçildi`
                : `${chooseableApplications.length} başvuru seçilebilir`}
            </Text>
          </Col>
          <Col>
            <Button
              type="primary"
              disabled={selectedIds.length === 0}
              onClick={handleBulkShare}
              icon={<ShareAltOutlined />}
            >
              Sonuçları Paylaş ({selectedIds.length})
            </Button>
          </Col>
        </Row>
      </div>

      {filteredApplications.length === 0 ? (
        <Empty description="Herhangi bir başvuru bulunamadı." style={{ marginTop: 64 }} />
      ) : (
        filteredApplications.map((application) => {
          const isExpanded = expandedIds.includes(application.applicationId)
          const detail = details[application.applicationId]
          const activeKey = activeTabs[application.applicationId] ?? 'general'
          const statusInfo = STATUS_MAP[application.status] ?? { label: application.status, color: 'default' }
          const documentCount = detail?.documents?.length ?? 0

          return (
            <div key={application.applicationId} style={styles.card}>
              <Row align="middle" justify="space-between" gutter={[16, 16]}>
                <Col xs={24} lg={20}>
                  <Row gutter={[16, 16]}>
                    <Col xs={24} sm={12} md={8}>
                      <Text style={styles.summaryDetail}>Ad Soyad</Text>
                      <Text style={styles.summaryValue}>{application.studentName}</Text>
                    </Col>
                    <Col xs={24} sm={12} md={8}>
                      <Text style={styles.summaryDetail}>E-posta</Text>
                      <Text style={styles.summaryValue}>{application.studentEmail}</Text>
                    </Col>
                    <Col xs={24} sm={12} md={8}>
                      <Text style={styles.summaryDetail}>Telefon Numarası</Text>
                      <Text style={styles.summaryValue}>{application.studentPhone}</Text>
                    </Col>
                    <Col xs={24} sm={12} md={8}>
                      <Text style={styles.summaryDetail}>Fakülte</Text>
                      <Text style={styles.summaryValue}>{application.targetFaculty}</Text>
                    </Col>
                    <Col xs={24} sm={12} md={8}>
                      <Text style={styles.summaryDetail}>Durum</Text>
                      <Tag color={statusInfo.color}>{statusInfo.label}</Tag>
                    </Col>
                    <Col xs={24} sm={12} md={8}>
                      <Text style={styles.summaryDetail}>Başvuru Tarihi</Text>
                      <Text style={styles.summaryValue}>{formatDate(application.submittedAt)}</Text>
                    </Col>
                  </Row>
                </Col>
                <Col>
                  <Button type="text" onClick={() => handleToggleExpand(application.applicationId)} icon={isExpanded ? <UpOutlined /> : <DownOutlined />}>
                    {isExpanded ? 'Küçült' : 'Detay' }
                  </Button>
                </Col>
              </Row>

              {isExpanded && (
                <div style={{ marginTop: 24 }}>
                  <Divider />
                  <Row gutter={[16, 16]}>
                    <Col xs={24} sm={12} lg={6}>
                      <Text style={styles.summaryDetail}>Mevcut Üniversite</Text>
                      <Text style={styles.summaryValue}>{detail?.currentUniversity ?? application.currentUniversity}</Text>
                    </Col>
                    <Col xs={24} sm={12} lg={6}>
                      <Text style={styles.summaryDetail}>Mevcut Bölüm</Text>
                      <Text style={styles.summaryValue}>{detail?.currentDepartment ?? application.currentDepartment}</Text>
                    </Col>
                    <Col xs={24} sm={12} lg={6}>
                      <Text style={styles.summaryDetail}>Hedef Bölüm</Text>
                      <Text style={styles.summaryValue}>{application.targetDepartment}</Text>
                    </Col>
                    <Col xs={24} sm={12} lg={6}>
                      <Text style={styles.summaryDetail}>Dönem</Text>
                      <Text style={styles.summaryValue}>{detail?.semester ?? application.semester}</Text>
                    </Col>
                  </Row>

                  <div style={styles.sectionRow}>
                    <div>
                      <Tabs
                        activeKey={activeKey}
                        onChange={(key) => setActiveTabs((prev) => ({ ...prev, [application.applicationId]: key }))}
                        items={[
                          {
                            key: 'general',
                            label: 'Genel Bakış',
                            children: (
                              <div style={styles.actionBox}>
                                <Title level={5}>İşlemler</Title>
                                {renderActionButtons(application.status, application.applicationId)}
                              </div>
                            ),
                          },
                          {
                            key: 'documents',
                            label: `Dökümanlar (${documentCount})`,
                            children: (
                              <div style={styles.actionBox}>
                                <Row align="middle" justify="space-between">
                                  <Col>
                                    <Title level={5} style={{ marginBottom: 0 }}>Dökümanlar</Title>
                                  </Col>
                                  <Col>
                                    <Button icon={<DownloadOutlined />}>Tümünü İndir</Button>
                                  </Col>
                                </Row>

                                {detail?.documents?.length ? (
                                  detail.documents.map((doc) => (
                                    <div key={doc.documentId} style={styles.documentRow}>
                                      <div style={styles.documentMeta}>
                                        <Text strong>{doc.fileName}</Text>
                                        <Text type="secondary">{DOCUMENT_TYPE_LABEL[doc.docType] ?? 'Belge'}</Text>
                                        <Text type="secondary" style={{ fontSize: 12 }}>
                                          {doc.size} • Yüklenme: {formatDate(doc.uploadedAt)}
                                        </Text>
                                      </div>
                                      <Button icon={<DownloadOutlined />} onClick={() => handleDownloadDocument(doc.documentId)}>
                                        İndir
                                      </Button>
                                    </div>
                                  ))
                                ) : (
                                  <Empty description="Döküman bulunmuyor." />
                                )}
                              </div>
                            ),
                          },
                          {
                            key: 'history',
                            label: 'Geçmiş',
                            children: (
                              <div style={styles.actionBox}>
                                <Title level={5}>Geçmiş</Title>
                                {detail?.statusHistory?.length ? (
                                  <Timeline mode="left">
                                    {detail.statusHistory.map((entry) => {
                                      const info = STATUS_MAP[entry.status] ?? { label: entry.status, color: 'default' }
                                      return (
                                        <Timeline.Item key={entry.status + entry.changedAt} color={info.color}>
                                          <Text strong>{info.label}</Text>
                                          <div>{formatDate(entry.changedAt)}</div>
                                        </Timeline.Item>
                                      )
                                    })}
                                  </Timeline>
                                ) : (
                                  <Empty description="Henüz geçmiş kaydı yok." />
                                )}
                              </div>
                            ),
                          },
                        ]}
                      />
                    </div>
                  </div>
                </div>
              )}
            </div>
          )
        })
      )}
    </div>
  )
}
