import { useState, useEffect } from 'react'
import { Tabs, Table, Button, Typography, Tag, Modal, Row, Col, Space, Input, Radio, Divider, message, Select, Alert, Spin } from 'antd'
import { DownloadOutlined, DownOutlined, UpOutlined } from '@ant-design/icons'
import apiClient from '../../api/client'

const { Title, Text } = Typography

const TINT = {
  amber: { background: '#FEF3C7', color: '#B45309' },
  green: { background: '#D1FAE5', color: '#047857' },
  red: { background: '#FEE2E2', color: '#B91C1C' },
  blue: { background: '#DBEAFE', color: '#1E40AF' },
  gray: { background: '#F3F4F6', color: '#6B7280' },
}

const styles = {
  page: { fontFamily: "'DM Sans', sans-serif" },
  card: {
    background: '#ffffff',
    borderRadius: 10,
    padding: 24,
    marginBottom: 16,
    boxShadow: '0 4px 16px rgba(0, 0, 0, 0.06)',
    border: '1px solid #f0f0f0',
  },
}

const REJECTION_CODES = [
  { value: 'DENKLIK_YETERSIZ', label: 'Ders denkliği %80 eşiğinin altında' },
  { value: 'EKSIK_BELGE', label: 'Eksik veya geçersiz belge' },
  { value: 'KOSULLAR_SAGLANAMADI', label: 'Bölüm koşulları karşılanamıyor' },
  { value: 'DIGER', label: 'Diğer (nota açıklama ekle)' },
]

const DENKLIK_LABELS = {
  TAM_DENKLIK: { label: 'Tam Denklik', tint: TINT.green },
  KISMI_DENKLIK: { label: 'Kısmi Denklik', tint: TINT.amber },
  DENKLIK_YOK: { label: 'Denklik Yok', tint: TINT.red },
}

// Kurul KABUL edince başvuru downstream'e ilerler (dekan → OIDB_FINAL_REVIEW → RESULT_PUBLISHED),
// böylece statüsü artık FACULTY_BOARD_ACCEPTED olmaz ve eski sorguda "Sonuçlandırılanlar"dan
// düşerdi. Bu iki ileri statüye YALNIZCA kurul kabul ettiyse ulaşılır, bu yüzden kabul kolu
// için güvenle dahil edilir. (REJECTED paylaşımlı bir terminal statü olduğundan ret koluna
// EKLENMEZ — başka aşamada reddedilen başvuruları bu listeye sızdırırdı.)
const BOARD_ACCEPTED_STATUSES = ['FACULTY_BOARD_ACCEPTED', 'OIDB_FINAL_REVIEW', 'RESULT_PUBLISHED']
const BOARD_REJECTED_STATUSES = ['FACULTY_BOARD_REJECTED']
const BOARD_COMPLETED_STATUSES = [...BOARD_ACCEPTED_STATUSES, ...BOARD_REJECTED_STATUSES]

// "Sonuçlandırılanlar" listesini birden çok statüden tek seferde toplar.
const fetchCompletedApplications = async () => {
  const responses = await Promise.all(
    BOARD_COMPLETED_STATUSES.map((status) =>
      apiClient.get('/applications', { params: { status, size: 500 } }),
    ),
  )
  return responses.flatMap((res) => res.data.content ?? [])
}

const handleError = (err) => {
  const status = err?.response?.status
  if (status === 400) message.error(err.response.data?.message || 'Geçersiz istek.')
  else if (status === 403) message.error('Bu işlem için yetkiniz yok.')
  else if (status === 404) message.error('Başvuru bulunamadı.')
  else message.error('Sunucuya ulaşılamadı, lütfen tekrar deneyin.')
}

const FakulteKuruluDashboard = () => {
  const [activeTab, setActiveTab] = useState('pending')
  const [pendingList, setPendingList] = useState([])
  const [completedList, setCompletedList] = useState([])
  const [listLoading, setListLoading] = useState(false)

  const [decisionModal, setDecisionModal] = useState(false)
  const [selectedStudent, setSelectedStudent] = useState(null)
  const [boardReview, setBoardReview] = useState(null)
  const [modalLoading, setModalLoading] = useState(false)

  const [evalNote, setEvalNote] = useState('')
  const [decision, setDecision] = useState('approve')
  const [rejectionCode, setRejectionCode] = useState(null)
  const [submitting, setSubmitting] = useState(false)
  const [belowThresholdWarning, setBelowThresholdWarning] = useState(null)

  const [expandedApproved, setExpandedApproved] = useState({})
  const [intibakExpanded, setIntibakExpanded] = useState(false)

  useEffect(() => {
    fetchLists()
  }, [])

  const fetchLists = async () => {
    setListLoading(true)
    try {
      const [pendingRes, completed] = await Promise.all([
        apiClient.get('/applications', { params: { status: 'FACULTY_BOARD_REVIEW', size: 500 } }),
        fetchCompletedApplications(),
      ])
      setPendingList(pendingRes.data.content ?? [])
      setCompletedList(completed)
    } catch (err) {
      handleError(err)
    } finally {
      setListLoading(false)
    }
  }

  const openDecisionModal = async (record) => {
    setSelectedStudent(record)
    setBoardReview(null)
    setEvalNote('')
    setDecision('approve')
    setRejectionCode(null)
    setBelowThresholdWarning(null)
    setIntibakExpanded(false)
    setDecisionModal(true)
    setModalLoading(true)
    try {
      const data = await apiClient.get(`/applications/${record.id}/board-review`).then(r => r.data)
      setBoardReview(data)
    } catch (err) {
      handleError(err)
      setDecisionModal(false)
    } finally {
      setModalLoading(false)
    }
  }

  const closeModal = () => {
    setDecisionModal(false)
    setBelowThresholdWarning(null)
  }

  const handleSubmit = async () => {
    if (!selectedStudent || !boardReview) return
    setSubmitting(true)
    try {
      const res = await apiClient
        .patch(`/applications/${selectedStudent.id}/faculty-board-review`, {
          approved: decision === 'approve',
          notes: evalNote || null,
          rejectionCode: decision === 'reject' ? rejectionCode : null,
        })
        .then(r => r.data)

      // Karar KESİN: PATCH başarılıysa başvuru artık FACULTY_BOARD_ACCEPTED/REJECTED.
      // Modalı HER DURUMDA kapat — aksi halde (denklik <%80'de modal açık kaldığından)
      // aynı başvuruya ikinci karar denenip "uygun aşamada değil" hatası alınıyordu.
      // %80 altı uyarısı artık toast olarak gösterilir.
      if (res.belowThreshold) {
        message.warning(
          `Karar kaydedildi. Denklik oranı (%${Math.round(res.equivalencyRatio * 100)}) %80 eşiğinin altında.`
        )
      }
      closeModal()

      // Pending listeden kaldır, tamamlananlar listesini yenile
      setPendingList(prev => prev.filter(s => s.id !== selectedStudent.id))
      setCompletedList(await fetchCompletedApplications())
    } catch (err) {
      handleError(err)
    } finally {
      setSubmitting(false)
    }
  }

  const toggleApprovedExpand = (id) => {
    setExpandedApproved(prev => ({ ...prev, [id]: !prev[id] }))
  }

  const approvedExpandedKeys = Object.keys(expandedApproved)
    .filter(k => expandedApproved[k])
    .map(Number)

  const renderApprovedExpanded = (record) => (
    <div style={{ padding: '12px 24px', background: '#fafafa' }}>
      <Row gutter={[16, 12]}>
        <Col span={6}>
          <div style={{ background: '#fff', padding: 12, borderRadius: 6, border: '1px solid #f0f0f0' }}>
            <Text type="secondary" style={{ display: 'block', fontSize: 12 }}>GPA</Text>
            <Text strong style={{ fontSize: 18 }}>{record.gpa}</Text>
          </div>
        </Col>
        <Col span={6}>
          <div style={{ background: '#fff', padding: 12, borderRadius: 6, border: '1px solid #f0f0f0' }}>
            <Text type="secondary" style={{ display: 'block', fontSize: 12 }}>YKS Puanı</Text>
            <Text strong style={{ fontSize: 18 }}>{record.sayYksScore}</Text>
          </div>
        </Col>
        <Col span={12}>
          <div style={{ background: '#fff', padding: 12, borderRadius: 6, border: '1px solid #f0f0f0' }}>
            <Text type="secondary" style={{ display: 'block', fontSize: 12 }}>Başvuru Tarihi</Text>
            <Text strong>{record.submissionDate}</Text>
          </div>
        </Col>
      </Row>
    </div>
  )

  const equivalencyRatio = boardReview?.intibak?.equivalencyRatio ?? null
  const conditionsMet = boardReview?.intibak?.conditionsMet ?? true
  const intibakRows = boardReview?.intibak?.rows ?? []

  const isSubmitDisabled = decision === 'reject' && !rejectionCode

  return (
    <div style={styles.page}>
      <Title level={2} style={{ marginBottom: 24 }}>Fakülte Kurulu</Title>

      <div style={styles.card}>
        <Tabs
          activeKey={activeTab}
          onChange={setActiveTab}
          items={[
            {
              key: 'pending',
              label: `Karar Bekleniyor (${pendingList.length})`,
              children: (
                <Table
                  dataSource={pendingList}
                  loading={listLoading}
                  columns={[
                    {
                      title: 'Ad Soyad',
                      dataIndex: 'studentName',
                      key: 'studentName',
                    },
                    {
                      title: 'Bölüm',
                      dataIndex: 'targetDepartment',
                      key: 'targetDepartment',
                    },
                    {
                      title: 'GPA',
                      dataIndex: 'gpa',
                      key: 'gpa',
                    },
                    {
                      title: 'YKS Puanı',
                      dataIndex: 'sayYksScore',
                      key: 'sayYksScore',
                    },
                    {
                      title: 'Başvuru Tarihi',
                      dataIndex: 'submissionDate',
                      key: 'submissionDate',
                    },
                    {
                      title: 'İşlem',
                      key: 'action',
                      render: (_, record) => (
                        <Button
                          type="primary"
                          size="small"
                          style={{ background: '#8B1A2B', borderColor: '#8B1A2B' }}
                          onClick={() => openDecisionModal(record)}
                        >
                          Değerlendir
                        </Button>
                      ),
                    },
                  ]}
                  rowKey="id"
                  pagination={false}
                  size="small"
                />
              ),
            },
            {
              key: 'approved',
              label: `Sonuçlandırılanlar (${completedList.length})`,
              children: (
                <Table
                  dataSource={completedList}
                  loading={listLoading}
                  columns={[
                    {
                      title: 'Ad Soyad',
                      dataIndex: 'studentName',
                      key: 'studentName',
                    },
                    {
                      title: 'Bölüm',
                      dataIndex: 'targetDepartment',
                      key: 'targetDepartment',
                    },
                    {
                      title: 'GPA',
                      dataIndex: 'gpa',
                      key: 'gpa',
                    },
                    {
                      title: 'Karar',
                      dataIndex: 'status',
                      key: 'status',
                      render: (status) => {
                        const isAccepted = BOARD_ACCEPTED_STATUSES.includes(status)
                        const tint = isAccepted ? TINT.green : TINT.red
                        const label = isAccepted ? 'Onaylandı' : 'Reddedildi'
                        return <Tag style={{ ...tint, border: 'none', borderRadius: 6 }}>{label}</Tag>
                      },
                    },
                    {
                      title: '',
                      key: 'expand',
                      width: 48,
                      render: (_, record) => (
                        <Button
                          type="text"
                          icon={expandedApproved[record.id]
                            ? <UpOutlined style={{ color: '#8B1A2B' }} />
                            : <DownOutlined style={{ color: '#8B1A2B' }} />}
                          onClick={() => toggleApprovedExpand(record.id)}
                        />
                      ),
                    },
                  ]}
                  expandable={{
                    expandedRowRender: renderApprovedExpanded,
                    expandedRowKeys: approvedExpandedKeys,
                    showExpandColumn: false,
                  }}
                  rowKey="id"
                  pagination={false}
                  size="small"
                />
              ),
            },
          ]}
        />
      </div>

      <Modal
        title="Başvuru Değerlendirmesi"
        open={decisionModal}
        onCancel={closeModal}
        width={800}
        style={{ top: 20 }}
        footer={[
          <Button key="cancel" onClick={closeModal}>İptal</Button>,
          <Button
            key="submit"
            type="primary"
            danger
            loading={submitting}
            disabled={isSubmitDisabled || modalLoading}
            onClick={handleSubmit}
          >
            Değerlendirmeyi Tamamla ve İlet
          </Button>,
        ]}
      >
        {modalLoading ? (
          <div style={{ textAlign: 'center', padding: '40px 0' }}>
            <Spin size="large" />
          </div>
        ) : boardReview && (
          <div style={{ maxHeight: '75vh', overflowY: 'auto', overflowX: 'hidden', paddingRight: 10 }}>

            {/* belowThreshold uyarısı */}
            {belowThresholdWarning != null && (
              <Alert
                style={{ marginBottom: 16 }}
                type="warning"
                showIcon
                message={`Karar kaydedildi. Ancak denklik oranı (%${Math.round(belowThresholdWarning * 100)}) %80 eşiğinin altında.`}
              />
            )}

            <Row gutter={[16, 16]}>
              <Col span={24}>
                <Title level={4} style={{ marginTop: 16 }}>Öğrenci Bilgileri</Title>
              </Col>
              <Col span={12}>
                <Text strong>Ad Soyad:</Text>
                <Text style={{ marginLeft: 8 }}>{boardReview.application?.studentName}</Text>
              </Col>
              <Col span={12}>
                <Text strong>Bölüm:</Text>
                <Text style={{ marginLeft: 8 }}>{boardReview.application?.targetDepartment}</Text>
              </Col>
              <Col span={12}>
                <Text strong>GPA:</Text>
                <Text style={{ marginLeft: 8 }}>{boardReview.application?.gpa}</Text>
              </Col>
              <Col span={12}>
                <Text strong>YKS Puanı:</Text>
                <Text style={{ marginLeft: 8 }}>{boardReview.application?.sayYksScore}</Text>
              </Col>
            </Row>

            <Divider />

            <Row gutter={[16, 16]}>
              <Col span={24}>
                <Title level={4}>YGK Değerlendirmesi</Title>
              </Col>
              <Col span={8}>
                <div style={{ background: '#f5f5f5', padding: 12, borderRadius: 6 }}>
                  <Text type="secondary" style={{ display: 'block', fontSize: 12 }}>Kompozit Skor</Text>
                  <Text strong style={{ fontSize: 18, color: '#8B1A2B' }}>{boardReview.compositeScore}</Text>
                </div>
              </Col>
              <Col span={8}>
                <div style={{ background: '#f5f5f5', padding: 12, borderRadius: 6 }}>
                  <Text type="secondary" style={{ display: 'block', fontSize: 12 }}>Sıralama</Text>
                  <Text strong style={{ fontSize: 18 }}>#{boardReview.ranking}</Text>
                </div>
              </Col>
              <Col span={8}>
                <div style={{ background: '#f5f5f5', padding: 12, borderRadius: 6 }}>
                  <Text type="secondary" style={{ display: 'block', fontSize: 12 }}>Koşullar</Text>
                  <Tag
                    style={{
                      marginTop: 4,
                      ...(boardReview.conditionsMet ? TINT.green : TINT.red),
                      border: 'none',
                      borderRadius: 6,
                    }}
                  >
                    {boardReview.conditionsMet ? 'Sağlandı' : 'Sağlanamadı'}
                  </Tag>
                </div>
              </Col>
              {boardReview.generalNote && (
                <Col span={24}>
                  <Text strong>YGK Notu:</Text>
                  <div style={{ marginTop: 8, background: '#f5f5f5', padding: 12, borderRadius: 6 }}>
                    <Text>{boardReview.generalNote}</Text>
                  </div>
                </Col>
              )}
            </Row>

            <Divider />

            {/* İntibak Tablosu */}
            <Row gutter={[16, 16]}>
              <Col span={24} style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <Button
                  type="link"
                  icon={intibakExpanded ? <UpOutlined /> : <DownOutlined />}
                  style={{ paddingLeft: 0, fontWeight: 600, fontSize: 16 }}
                  onClick={() => setIntibakExpanded(prev => !prev)}
                >
                  İntibak Raporu
                </Button>
                {conditionsMet && equivalencyRatio != null && (
                  <Tag
                    style={{
                      ...(equivalencyRatio >= 0.8 ? TINT.green : equivalencyRatio >= 0.5 ? TINT.amber : TINT.red),
                      border: 'none',
                      borderRadius: 6,
                      fontSize: 13,
                      padding: '2px 10px',
                    }}
                  >
                    Denklik Oranı: %{Math.round(equivalencyRatio * 100)}
                  </Tag>
                )}
              </Col>
              {intibakExpanded && (
                <Col span={24}>
                  <Table
                    columns={[
                      { title: 'Mevcut Ders Kodu', dataIndex: 'sourceCode', key: 'sourceCode' },
                      { title: 'Mevcut Ders Adı', dataIndex: 'sourceName', key: 'sourceName' },
                      { title: 'Kredi', dataIndex: 'sourceCredit', key: 'sourceCredit' },
                      { title: 'Not', dataIndex: 'sourceGrade', key: 'sourceGrade' },
                      { title: 'İYTE Ders Kodu', dataIndex: 'targetCode', key: 'targetCode', render: (v) => v ?? '—' },
                      { title: 'İYTE Ders Adı', dataIndex: 'targetName', key: 'targetName', render: (v) => v ?? '—' },
                      { title: 'Kredi', dataIndex: 'targetCredit', key: 'targetCredit', render: (v) => v ?? '—' },
                      { title: 'İYTE Not', dataIndex: 'targetGrade', key: 'targetGrade', render: (v) => v ?? '—' },
                      {
                        title: 'Denklik',
                        dataIndex: 'equivalencyStatus',
                        key: 'equivalencyStatus',
                        render: (status) => {
                          const info = DENKLIK_LABELS[status] ?? { label: status, tint: TINT.gray }
                          return <Tag style={{ ...info.tint, border: 'none', borderRadius: 6 }}>{info.label}</Tag>
                        },
                      },
                    ]}
                    dataSource={intibakRows}
                    rowKey="id"
                    pagination={false}
                    size="small"
                    bordered
                  />
                </Col>
              )}
            </Row>

            <Divider />

            <Row gutter={[16, 16]}>
              <Col span={24}>
                <Title level={4}>Kurul Kararı</Title>
              </Col>
              <Col span={24}>
                <Radio.Group value={decision} onChange={(e) => { setDecision(e.target.value); setRejectionCode(null) }}>
                  <Space direction="vertical">
                    <Radio value="approve">Başvuruyu Onayla</Radio>
                    <Radio value="reject">Başvuruyu Reddet</Radio>
                  </Space>
                </Radio.Group>
              </Col>
              {decision === 'reject' && (
                <Col span={24}>
                  <Text strong>
                    Ret Gerekçesi <span style={{ color: '#B91C1C' }}>*</span>
                  </Text>
                  <Select
                    style={{ width: '100%', marginTop: 8 }}
                    placeholder="Gerekçe seçiniz..."
                    value={rejectionCode}
                    onChange={setRejectionCode}
                    options={REJECTION_CODES}
                  />
                </Col>
              )}
            </Row>

            <Divider />

            <Row gutter={[16, 16]}>
              <Col span={24}>
                <Text strong>Değerlendirme Notu:</Text>
              </Col>
              <Col span={24}>
                <Input.TextArea
                  rows={4}
                  value={evalNote}
                  onChange={(e) => setEvalNote(e.target.value)}
                  placeholder="Değerlendirme notunuzu giriniz..."
                />
              </Col>
            </Row>

          </div>
        )}
      </Modal>
    </div>
  )
}

export default FakulteKuruluDashboard
