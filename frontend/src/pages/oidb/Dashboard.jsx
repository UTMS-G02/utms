import { useCallback, useEffect, useMemo, useState } from 'react'
import { useLocation } from 'react-router-dom'
import {
  App,
  Button,
  Col,
  Divider,
  Empty,
  Input,
  Modal,
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

// Yetkili (Bearer) bir GET ile alınan blob'u tarayıcıda indirmeye zorlar:
// geçici obje URL'i → gizli <a download> → programatik click → URL'i temizle (revoke).
const triggerBlobDownload = (blob, fileName) => {
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  link.remove()
  window.URL.revokeObjectURL(url)
}

const STATUS_MAP = {
  ALL: { label: 'Tümü' },
  OIDB_REVIEW: { label: 'İnceleniyor', color: 'blue' },
  REQUEST_UPDATE: { label: 'Güncelleme Gerekli', color: 'volcano' },
  YDYO_REVIEW: { label: "YDYO'da", color: 'purple' },
  YDYO_APPROVED: { label: "YDYO Onaylı", color: 'green' },
  YDYO_REJECTED: { label: "YDYO Reddetti", color: 'magenta' },
  FACULTY_REVIEW: { label: 'Fakültede', color: 'gold' },
  OIDB_FINAL: { label: 'Yayın Bekliyor', color: 'cyan' },
  ACCEPTED: { label: 'Onaylandı', color: 'success' },
  REJECTED: { label: 'Reddedildi', color: 'error' },
}

// Color tinting system matching YDYO design
const TINT = {
  amber: { background: '#FEF3C7', color: '#B45309' },
  green: { background: '#D1FAE5', color: '#047857' },
  red: { background: '#FEE2E2', color: '#B91C1C' },
  blue: { background: '#DBEAFE', color: '#1E40AF' },
  purple: { background: '#E9D5FF', color: '#7E22CE' },
  gray: { background: '#F3F4F6', color: '#6B7280' },
}

const getStatusTint = (status) => {
  switch (status) {
    case 'OIDB_REVIEW':
      return TINT.blue
    case 'REQUEST_UPDATE':
      return TINT.amber
    case 'YDYO_REVIEW':
      return TINT.purple
    case 'YDYO_APPROVED':
      return TINT.green
    case 'YDYO_REJECTED':
      return TINT.red
    case 'FACULTY_REVIEW':
      return TINT.purple
    case 'OIDB_FINAL':
      return TINT.blue
    case 'ACCEPTED':
      return TINT.green
    case 'REJECTED':
      return TINT.red
    default:
      return TINT.gray
  }
}

// Öğrenci, istenen düzeltmeyi yapıp başvuruyu yeniden gönderdiyse memura ayırt edici bir
// rozet gösterilir ("Öğrenci Güncelledi") — aksi halde standart STATUS_MAP kullanılır.
const getOidbStatusInfo = (application) => {
  const resubmitted = application.revisionRequestedBefore &&
    (application.rawStatus === 'SUBMITTED' || application.rawStatus === 'OIDB_REVIEW')
  if (resubmitted) return { label: 'Öğrenci Güncelledi', color: 'gold' }
  return STATUS_MAP[application.status] ?? { label: application.status, color: 'default' }
}

const STATUS_OPTIONS = [
  { value: 'ALL', label: 'Tümü' },
  { value: 'OIDB_REVIEW', label: 'İnceleniyor' },
  { value: 'REQUEST_UPDATE', label: 'Güncelleme Gerekli' },
  { value: 'YDYO_REVIEW', label: "YDYO'da" },
  { value: 'YDYO_APPROVED', label: "YDYO Onaylı" },
  { value: 'YDYO_REJECTED', label: "YDYO Reddetti" },
  { value: 'FACULTY_REVIEW', label: 'Fakültede' },
  { value: 'OIDB_FINAL', label: 'Yayın Bekliyor' },
  { value: 'ACCEPTED', label: 'Onaylandı' },
  { value: 'REJECTED', label: 'Reddedildi' },
]

const FACULTY_OPTIONS = [
  { value: 'ALL', label: 'Tüm Fakülteler' },
  { value: 'Mühendislik Fakültesi', label: 'Mühendislik Fakültesi' },
  { value: 'Fen Fakültesi', label: 'Fen Fakültesi' },
  { value: 'Mimarlık Fakültesi', label: 'Mimarlık Fakültesi' },
]

const ANNOUNCEABLE_STATUSES = ['OIDB_FINAL', 'ACCEPTED', 'REJECTED']

const DOCUMENT_TYPE_LABEL = {
  STUDENT_CERTIFICATE: 'Öğrenci Belgesi',
  TRANSCRIPT: 'Transkript Belgesi',
  YKS_RESULT: 'YKS Sonuç Belgesi',
  LANGUAGE_CERT: 'Yabancı Dil Belgesi',
  COURSE_CONTENTS: 'Ders İçerikleri', // <-- Eklenecek satır
  OTHER_1: 'Ek belge 1', 
  OTHER_2: 'Ek belge 2', 
  OTHER_3: 'Ek belge 3', 
  OTHER_4: 'Ek belge 4', 
  OTHER_5: 'Ek belge 5', 
  ID_CARD: 'Kimlik Belgesi',
  OTHER: 'Diğer Belge',
}

// Modal dropdown'u için, başvuruda belge yoksa kullanılacak genel yedek liste.
const FALLBACK_DOCUMENT_OPTIONS = Object.entries(DOCUMENT_TYPE_LABEL).map(
  ([value, label]) => ({ value, label }),
)

const ACTION_CONFIG = {
  OIDB_REVIEW: [
    { key: 'request_update', label: 'Güncelleme İste', type: 'default' },
    { key: 'reject_application', label: 'Reddet', type: 'default', danger: true },
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
    borderRadius: 10,
    padding: 24,
    boxShadow: '0 4px 16px rgba(0, 0, 0, 0.06)',
    marginBottom: 24,
    border: '1px solid #f0f0f0',
  },
  filterBar: {
    background: '#ffffff',
    borderRadius: 10,
    padding: 24,
    boxShadow: '0 4px 16px rgba(0, 0, 0, 0.06)',
    marginBottom: 24,
    border: '1px solid #f0f0f0',
  },
  selectBox: {
    width: '100%',
    maxWidth: 320,
  },
  card: {
    background: '#ffffff',
    borderRadius: 10,
    padding: 24,
    marginBottom: 16,
    boxShadow: '0 4px 16px rgba(0, 0, 0, 0.06)',
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
    borderRadius: 10,
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
  const { message: antdMessage, modal } = App.useApp()
  const { logout } = useAuth()
  const location = useLocation()
  const isPendingRoute = location.pathname === '/oidb/pending'

  const [loading, setLoading] = useState(true)
  const [applications, setApplications] = useState([])
  const [details, setDetails] = useState({})
  const [selectedIds, setSelectedIds] = useState([])
  const [statusFilter, setStatusFilter] = useState(isPendingRoute ? 'OIDB_REVIEW' : 'ALL')
  const [facultyFilter, setFacultyFilter] = useState('ALL')
  const [searchTerm, setSearchTerm] = useState('')
  const [activeTabs, setActiveTabs] = useState({})
  const [detailsModal, setDetailsModal] = useState({
    open: false,
    applicationId: null,
  })

  // "Güncelleme İste" modalı durumu: hangi başvuru, seçilen belge tip(ler)i ve not.
  const [revisionModal, setRevisionModal] = useState({
    open: false,
    applicationId: null,
    documentTypes: [],
    notes: '',
  })
  const [revisionSubmitting, setRevisionSubmitting] = useState(false)

  useEffect(() => {
    setStatusFilter(isPendingRoute ? 'OIDB_REVIEW' : 'ALL')
  }, [isPendingRoute])

  // Liste çekme tek noktada; hem ilk yüklemede hem de aksiyon sonrası sessiz
  // yenilemede kullanılır (aksiyon sonrası kart güncel statüyle gelsin diye).
  const loadApplications = useCallback(async () => {
    try {
      const list = await applicationsApi.getOidbApplications()
      setApplications(list ?? [])
    } catch {
      antdMessage.error('Başvurular yüklenirken bir hata oluştu.')
    }
  }, [antdMessage])

  useEffect(() => {
    setLoading(true)
    loadApplications().finally(() => setLoading(false))
  }, [loadApplications])

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
    // Open modal instead of expand
    if (!details[applicationId]) {
      antdMessage.loading({ content: 'Detaylar yükleniyor...', key: `loading-${applicationId}` })
      try {
        const detail = await applicationsApi.getOidbApplicationById(applicationId)
        setDetails((prev) => ({ ...prev, [applicationId]: detail }))
        antdMessage.destroy(`loading-${applicationId}`)
      } catch {
        antdMessage.error('Başvuru detayları alınamadı.')
        return
      }
    }

    setDetailsModal({
      open: true,
      applicationId,
    })
  }

  const executeAction = async (applicationId, actionKey) => {
    try {
      let result = null
      if (actionKey === 'request_update') result = await applicationsApi.requestApplicationUpdate(applicationId)
      if (actionKey === 'send_ydyo') result = await applicationsApi.sendToYdyo(applicationId)
      if (actionKey === 'forward_to_faculty') result = await applicationsApi.forwardToFaculty(applicationId)
      if (actionKey === 'reject_application') result = await applicationsApi.rejectApplication(applicationId)
      if (actionKey === 'share_result') result = await applicationsApi.shareResults([applicationId])

      if (result?.success !== false) {
        antdMessage.success('İşlem başarılı.')
        await loadApplications()
      } else {
        throw new Error('Action failed')
      }
    } catch (err) {
      // Varsa backend'in döndürdüğü anlamlı iş kuralı mesajını göster.
      antdMessage.error(
        err?.response?.data?.message || 'İşlem gerçekleştirilirken bir hata oluştu.',
      )
    }
  }

  const handleAction = (applicationId, actionKey) => {
    // Güncelleme İste → doğrudan istek atma; hangi belge(ler) + not seçimi için modal aç.
    if (actionKey === 'request_update') {
      setRevisionModal({
        open: true,
        applicationId,
        documentTypes: [], // memur düzeltilecek belgeleri kendi seçer (birden fazla olabilir)
        notes: '',
      })
      return
    }

    // Reddetme geri alınamaz bir işlem; önce onay penceresi göster.
    if (actionKey === 'reject_application') {
      modal.confirm({
        title: 'Başvuruyu Reddet',
        content: 'Bu başvuruyu reddetmek istediğinize emin misiniz? Başvurunun durumu "Reddedildi" olarak güncellenecektir.',
        okText: 'Evet, Reddet',
        okButtonProps: { danger: true },
        cancelText: 'Vazgeç',
        onOk: () => executeAction(applicationId, actionKey),
      })
      return
    }
    executeAction(applicationId, actionKey)
  }

  const closeRevisionModal = () => setRevisionModal((prev) => ({ ...prev, open: false }))

  const handleRevisionSubmit = async () => {
    if (!revisionModal.documentTypes || revisionModal.documentTypes.length === 0) {
      antdMessage.warning('Lütfen düzeltilmesi gereken en az bir belge seçin.')
      return
    }
    if (!revisionModal.notes.trim()) {
      antdMessage.warning('Lütfen öğrenciye iletilecek bir düzeltme notu yazın.')
      return
    }
    setRevisionSubmitting(true)
    try {
      await applicationsApi.requestApplicationUpdate(revisionModal.applicationId, {
        requestedDocumentTypes: revisionModal.documentTypes,
        revisionNotes: revisionModal.notes.trim(),
      })
      antdMessage.success('Güncelleme isteği öğrenciye iletildi.')
      closeRevisionModal()
      await loadApplications()
    } catch (err) {
      // Backend anlamlı bir iş kuralı mesajı döndürdüyse (örn. "sadece bir kez...") onu göster.
      antdMessage.error(
        err?.response?.data?.message || 'Güncelleme isteği gönderilirken bir hata oluştu.',
      )
    } finally {
      setRevisionSubmitting(false)
    }
  }

  // Modal dropdown'u: seçili başvuruya yüklü belgelerden türetilir; yoksa genel listeye düşer.
  const revisionDocOptions = (() => {
    const docs = details[revisionModal.applicationId]?.documents ?? []
    if (docs.length === 0) return FALLBACK_DOCUMENT_OPTIONS
    return docs.map((doc) => ({
      value: doc.docType,
      label: DOCUMENT_TYPE_LABEL[doc.docType] ?? doc.docType,
    }))
  })()

  const handleBulkShare = async () => {
    if (selectedIds.length === 0) return
    try {
      await applicationsApi.shareResults(selectedIds)
      antdMessage.success('Sonuç paylaşımı başarılı.')
      setSelectedIds([])
      await loadApplications()
    } catch {
      antdMessage.error('Sonuç paylaşımı sırasında bir hata oluştu.')
    }
  }

  // Tekli belge indirme — doğrudan link/window.open YERİNE axios ile yetkili GET.
  // responseType: 'blob' API servisinde set edilir; dosya adı orijinal fileName.
  const handleDownloadDocument = async (documentId, fileName) => {
    try {
      const res = await applicationsApi.downloadDocument(documentId)
      triggerBlobDownload(res.data, fileName || `belge_${documentId}`)
    } catch {
      antdMessage.error('Belge indirilemedi. Lütfen tekrar deneyin.')
    }
  }

  // Toplu (ZIP) indirme — başvuruya ait tüm belgeler tek arşivde, yetkili GET ile.
  const handleDownloadAllZip = async (applicationId) => {
    try {
      const res = await applicationsApi.downloadAllDocumentsZip(applicationId)
      triggerBlobDownload(res.data, `basvuru_${applicationId}_belgeler.zip`)
    } catch {
      antdMessage.error('Belgeler indirilemedi. Lütfen tekrar deneyin.')
    }
  }

  const renderActionButtons = (application) => {
    let actions = ACTION_CONFIG[application.status] ?? []
    // Bir kez düzeltme istendiyse "Güncelleme İste" artık kullanılamaz; memur yalnızca
    // YDYO'ya gönderebilir veya reddedebilir (backend de ikinci isteği reddeder).
    if (application.revisionRequestedBefore) {
      actions = actions.filter((a) => a.key !== 'request_update')
    }
    if (actions.length === 0) {
      return <Text type="secondary">Bu durumda yapılacak işlem yok.</Text>
    }

    const applicationId = application.applicationId
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
            <Space direction="vertical" style={{ width: '100%' }}>
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
            </Space>
          </Col>
          <Col xs={24} sm={12} lg={6}>
            <Space direction="vertical" style={{ width: '100%' }}>
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
            </Space>
          </Col>
          <Col xs={24} sm={24} lg={2} style={{ display: 'flex', alignItems: 'flex-end' }}>
            <Text style={{ display: 'block', fontWeight: 600 }}>{filteredApplications.length} başvuru gösteriliyor</Text>
          </Col>
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
          const detail = details[application.applicationId]
          const statusInfo = getOidbStatusInfo(application)

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
                      <Tag style={{ ...getStatusTint(application.status), border: 'none', borderRadius: 6, padding: '4px 12px', fontSize: 12, fontWeight: 500 }}>
                        {statusInfo.label}
                      </Tag>
                    </Col>
                    <Col xs={24} sm={12} md={8}>
                      <Text style={styles.summaryDetail}>Başvuru Tarihi</Text>
                      <Text style={styles.summaryValue}>{formatDate(application.submittedAt)}</Text>
                    </Col>
                  </Row>
                </Col>
                <Col>
                  <Button type="primary" onClick={() => handleToggleExpand(application.applicationId)}>
                    Detayları Gör
                  </Button>
                </Col>
              </Row>
            </div>
          )
        })
      )}

      <Modal
        title="Güncelleme İste"
        open={revisionModal.open}
        onOk={handleRevisionSubmit}
        onCancel={closeRevisionModal}
        confirmLoading={revisionSubmitting}
        okText="Gönder"
        cancelText="Vazgeç"
        destroyOnClose
      >
        <Text type="secondary">
          Öğrenciden hangi belge(ler)i düzeltmesini istediğinizi seçin ve bir not ekleyin.
          Öğrenci yalnızca seçtiğiniz belgeleri yeniden yükleyebilecek.
        </Text>
        <div style={{ marginTop: 16 }}>
          <Text strong>Düzeltilecek Belge(ler)</Text>
          <Select
            mode="multiple"
            allowClear
            style={{ width: '100%', marginTop: 6 }}
            placeholder="Bir veya daha fazla belge seçin"
            value={revisionModal.documentTypes}
            onChange={(value) => setRevisionModal((prev) => ({ ...prev, documentTypes: value }))}
            options={revisionDocOptions}
          />
        </div>
        <div style={{ marginTop: 16 }}>
          <Text strong>Düzeltme Notu <span style={{ color: 'red' }}>*</span></Text>
          <Input.TextArea
            style={{ marginTop: 6 }}
            rows={4}
            maxLength={500}
            showCount
            placeholder="Örn: Belge okunamıyor, lütfen PDF olarak yeniden yükleyin."
            value={revisionModal.notes}
            onChange={(e) => setRevisionModal((prev) => ({ ...prev, notes: e.target.value }))}
          />
        </div>
      </Modal>

      {/* Details Modal */}
      <Modal
        title={detailsModal.applicationId && details[detailsModal.applicationId] ? `Başvuru Detayları - ${details[detailsModal.applicationId].studentName}` : 'Başvuru Detayları'}
        open={detailsModal.open}
        onCancel={() => setDetailsModal({ open: false, applicationId: null })}
        footer={null}
        width={1000}
        destroyOnClose
        bodyStyle={{ maxHeight: '70vh', overflowY: 'auto', padding: '24px' }}
      >
        {details[detailsModal.applicationId] && (
          <Tabs
            defaultActiveKey="general"
            items={[
              {
                key: 'general',
                label: 'Genel Bilgiler',
                children: (
                  <div style={{ marginTop: 16 }}>
                    <Row gutter={[16, 16]}>
                      <Col xs={24} sm={12}>
                        <Text style={styles.summaryDetail}>Mevcut Üniversite</Text>
                        <Text style={styles.summaryValue}>{details[detailsModal.applicationId]?.currentUniversity}</Text>
                      </Col>
                      <Col xs={24} sm={12}>
                        <Text style={styles.summaryDetail}>Mevcut Bölüm</Text>
                        <Text style={styles.summaryValue}>{details[detailsModal.applicationId]?.currentDepartment}</Text>
                      </Col>
                      <Col xs={24} sm={12}>
                        <Text style={styles.summaryDetail}>Hedef Bölüm</Text>
                        <Text style={styles.summaryValue}>{details[detailsModal.applicationId]?.targetDepartment}</Text>
                      </Col>
                      <Col xs={24} sm={12}>
                        <Text style={styles.summaryDetail}>Dönem</Text>
                        <Text style={styles.summaryValue}>{details[detailsModal.applicationId]?.semester}</Text>
                      </Col>
                    </Row>
                    <Divider />
                    <Title level={5}>İşlemler</Title>
                    {renderActionButtons(applications.find(a => a.applicationId === detailsModal.applicationId))}
                  </div>
                ),
              },
              {
                key: 'documents',
                label: `Dökümanlar (${details[detailsModal.applicationId]?.documents?.length ?? 0})`,
                children: (
                  <div style={{ marginTop: 16 }}>
                    <Row align="middle" justify="space-between" style={{ marginBottom: 16 }}>
                      <Col>
                        <Title level={5} style={{ marginBottom: 0 }}>Dökümanlar</Title>
                      </Col>
                      <Col>
                        <Button
                          icon={<DownloadOutlined />}
                          disabled={!details[detailsModal.applicationId]?.documents?.length}
                          onClick={() => handleDownloadAllZip(detailsModal.applicationId)}
                        >
                          Tümünü İndir
                        </Button>
                      </Col>
                    </Row>

                    {details[detailsModal.applicationId]?.documents?.length ? (
                      details[detailsModal.applicationId].documents.map((doc) => (
                        <div key={doc.documentId} style={styles.documentRow}>
                          <div style={styles.documentMeta}>
                            <Text strong>{doc.fileName}</Text>
                            <Text type="secondary">{DOCUMENT_TYPE_LABEL[doc.docType] ?? 'Belge'}</Text>
                            <Text type="secondary" style={{ fontSize: 12 }}>
                              {doc.size} • Yüklenme: {formatDate(doc.uploadedAt)}
                            </Text>
                          </div>
                          <Button icon={<DownloadOutlined />} onClick={() => handleDownloadDocument(doc.documentId, doc.fileName)}>
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
                  <div style={{ marginTop: 16 }}>
                    <Title level={5}>Geçmiş</Title>
                    {details[detailsModal.applicationId]?.statusHistory && details[detailsModal.applicationId].statusHistory.length > 0 ? (
                      <Timeline mode="left">
                        {details[detailsModal.applicationId].statusHistory.map((entry, index) => {
                          const info = STATUS_MAP[entry.status] ?? { label: entry.status, color: 'default' }
                          const date = entry.changedAt ? formatDate(entry.changedAt) : '-'
                          return (
                            <Timeline.Item key={`history-${index}`} color={info.color}>
                              <Text strong>{info.label}</Text>
                              {entry.note && (
                                <div style={{ marginTop: 2 }}>{entry.note}</div>
                              )}
                              <div style={{ marginTop: 4, color: '#999' }}>{date}</div>
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
        )}
      </Modal>
    </div>
  )
}
