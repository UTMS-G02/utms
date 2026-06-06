import { useState } from 'react'
import {
  Tabs, Table, Button, Typography, Tag, Modal, Row, Col, Space, Input,
  Radio, Divider, Alert, Form, Select, InputNumber, Popconfirm, message,
} from 'antd'
import { DownOutlined, UpOutlined } from '@ant-design/icons'
import intibakApi from '../../api/intibak'

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

const GRADE_OPTIONS = ['AA', 'BA', 'BB', 'CB', 'CC', 'DC', 'DD', 'FF'].map((g) => ({
  value: g,
  label: g,
}))

const EQUIVALENCY_OPTIONS = [
  { value: 'TAM_DENKLIK', label: 'Tam Denklik' },
  { value: 'KISMI_DENKLIK', label: 'Kısmi Denklik' },
  { value: 'DENK_DEGIL', label: 'Denk Değil' },
]

const EQUIVALENCY_DISPLAY = {
  TAM_DENKLIK: { label: 'Tam Denk', tint: TINT.green },
  KISMI_DENKLIK: { label: 'Kısmi Denk', tint: TINT.amber },
  DENK_DEGIL: { label: 'Denk Değil', tint: TINT.red },
}

// Mock data
const PENDING_STUDENTS = [
  { id: 1, ad: 'Burak', soyad: 'Yıldız', email: 'burak.yildiz@example.edu.tr', tel: '+90 537 765 4321', bolum: 'Bilgisayar Mühendisliği', compositeScore: 90.5, otoSiralama: 'Asil Liste #1', ingilizce: 'Onaylandı', durum: 'Onaylandı', basvuruTarihi: '03.01.2024', gpa: 3.78, yksPuani: 498, mevcutUni: 'İTÜ', ingilizceDetay: 'Onaylandı - TOEFL iBT 95 - Yeterli seviye' },
  { id: 2, ad: 'Deniz', soyad: 'Kaya', email: 'deniz.kaya@example.edu.tr', tel: '+90 538 123 4567', bolum: 'Makina Mühendisliği', compositeScore: 78.9, otoSiralama: 'Yedek Liste #5', ingilizce: 'Onaylandı', durum: 'Onaylandı', basvuruTarihi: '18.01.2024', gpa: 2.9, yksPuani: 420, mevcutUni: 'Gazi Üniversitesi', ingilizceDetay: 'Onaylandı - TOEFL iBT 85 - Yeterli seviye' },
  { id: 3, ad: 'Ayşe', soyad: 'Yılmaz', email: 'ayse.yilmaz@example.edu.tr', tel: '+90 532 123 4567', bolum: 'İnşaat Mühendisliği', compositeScore: 85.3, otoSiralama: 'Asil Liste #2', ingilizce: 'Bekleniyor', durum: 'Bekleniyor', basvuruTarihi: '12.01.2024', gpa: 3.5, yksPuani: 480, mevcutUni: 'Orta Doğu Teknik Üniversitesi', ingilizceDetay: 'İngilizce Yeterlilik Sınıfında' },
]

const EVALUATED_STUDENTS = [
  { id: 4, ad: 'Can', soyad: 'Öztürk', email: 'can.ozturk@example.edu.tr', tel: '+90 535 765 4321', bolum: 'Bilgisayar Mühendisliği', compositeScore: 82.5, otoSiralama: 'Asil Liste #3', ingilizce: 'Onaylandı', durum: 'Onaylandı', basvuruTarihi: '08.01.2024', gpa: 3.45, yksPuani: 485, mevcutUni: 'Gazi Üniversitesi', ygkNotu: 'Başarılı bir öğrencidir. Değerlendirme yapılmıştır.' },
  { id: 5, ad: 'Elif', soyad: 'Şahin', email: 'elif.sahin@example.edu.tr', tel: '+90 536 123 4567', bolum: 'Elektrik Mühendisliği', compositeScore: 75.2, otoSiralama: 'Yedek Liste #8', ingilizce: 'Bekleniyor', durum: 'İnceleme Yapılmıştır', basvuruTarihi: '05.01.2024', gpa: 3.1, yksPuani: 450, mevcutUni: 'Teknik Üniversite', ygkNotu: 'Yedek liste kapsamında değerlendirilmiştir.' },
]

const YGKDashboard = () => {
  const [activeTab, setActiveTab] = useState('pending')
  const [evaluationModal, setEvaluationModal] = useState(false)
  const [selectedStudent, setSelectedStudent] = useState(null)
  const [evalNote, setEvalNote] = useState('')
  const [conditionsDecision, setConditionsDecision] = useState('approved')
  const [showConditionsWarning, setShowConditionsWarning] = useState(false)
  const [expandedEvaluated, setExpandedEvaluated] = useState({})

  // İntibak state
  const [intibakRows, setIntibakRows] = useState([])
  const [intibakLoading, setIntibakLoading] = useState(false)
  const [equivalencyRatio, setEquivalencyRatio] = useState(null)
  const [rowModalOpen, setRowModalOpen] = useState(false)
  const [editingRow, setEditingRow] = useState(null)
  const [dualSource, setDualSource] = useState(false)
  const [rowSaving, setRowSaving] = useState(false)
  const [rowForm] = Form.useForm()
  const watchedEquivalencyStatus = Form.useWatch('equivalencyStatus', rowForm)

  const MOCK_INTIBAK = {
    1: {
      rows: [
        { id: 1, sourceCode: 'BLM101', sourceName: 'Programlamaya Giriş', sourceCredit: 4, sourceGrade: 'AA', source2Code: null, targetCode: 'CENG101', targetName: 'Introduction to Programming', targetCredit: 4, targetGrade: 'AA', equivalencyStatus: 'TAM_DENKLIK' },
        { id: 2, sourceCode: 'MAT201', sourceName: 'Diferansiyel Denklemler', sourceCredit: 3, sourceGrade: 'BA', source2Code: null, targetCode: 'MATH201', targetName: 'Differential Equations', targetCredit: 3, targetGrade: 'BA', equivalencyStatus: 'KISMI_DENKLIK' },
      ],
      equivalencyRatio: 0.87,
      conditionsMet: true,
    },
    2: {
      rows: [
        { id: 3, sourceCode: 'BLM101', sourceName: 'Programlamaya Giriş', sourceCredit: 4, sourceGrade: 'CC', source2Code: null, targetCode: 'CENG101', targetName: 'Introduction to Programming', targetCredit: 4, targetGrade: 'CC', equivalencyStatus: 'KISMI_DENKLIK' },
        { id: 4, sourceCode: 'BIO100', sourceName: 'Biology', sourceCredit: 2, sourceGrade: 'FF', source2Code: null, targetCode: null, targetName: null, targetCredit: null, targetGrade: null, equivalencyStatus: 'DENK_DEGIL' },
      ],
      equivalencyRatio: 0.62,
      conditionsMet: true,
    },
    3: {
      rows: [],
      equivalencyRatio: null,
      conditionsMet: true,
    },
  }

  const fetchIntibak = async (applicationId) => {
    setIntibakLoading(true)
    try {
      const mockData = MOCK_INTIBAK[applicationId]
      const data = mockData ?? await intibakApi.getIntibak(applicationId)
      setIntibakRows(data.rows ?? [])
      setEquivalencyRatio(data.equivalencyRatio)
      if (data.conditionsMet === false) {
        setConditionsDecision('rejected')
        setShowConditionsWarning(true)
      }
    } catch {
      setIntibakRows([])
      setEquivalencyRatio(null)
    } finally {
      setIntibakLoading(false)
    }
  }

  const openEvaluationModal = (record) => {
    setSelectedStudent(record)
    setEvalNote('')
    setConditionsDecision('approved')
    setShowConditionsWarning(false)
    setIntibakRows([])
    setEquivalencyRatio(null)
    setEvaluationModal(true)
    fetchIntibak(record.id)
  }

  const toggleEvaluatedExpand = (id) => {
    setExpandedEvaluated(prev => ({ ...prev, [id]: !prev[id] }))
  }

  const evaluatedExpandedKeys = Object.keys(expandedEvaluated)
    .filter(k => expandedEvaluated[k])
    .map(Number)

  const openAddRow = () => {
    setEditingRow(null)
    setDualSource(false)
    rowForm.resetFields()
    setRowModalOpen(true)
  }

  const openEditRow = (row) => {
    setEditingRow(row)
    setDualSource(!!row.source2Code)
    rowForm.setFieldsValue({
      sourceCode: row.sourceCode,
      sourceName: row.sourceName,
      sourceCredit: row.sourceCredit,
      sourceGrade: row.sourceGrade,
      source2Code: row.source2Code,
      source2Name: row.source2Name,
      source2Credit: row.source2Credit,
      source2Grade: row.source2Grade,
      targetCode: row.targetCode,
      targetName: row.targetName,
      targetCredit: row.targetCredit,
      targetGrade: row.targetGrade,
      equivalencyStatus: row.equivalencyStatus,
    })
    setRowModalOpen(true)
  }

  const handleRowSubmit = async () => {
    try {
      const values = await rowForm.validateFields()
      setRowSaving(true)
      if (!dualSource) {
        values.source2Code = null
        values.source2Name = null
        values.source2Credit = null
        values.source2Grade = null
      }
      if (values.equivalencyStatus === 'DENK_DEGIL') {
        values.targetCode = null
        values.targetName = null
        values.targetCredit = null
        values.targetGrade = null
      }
      let updatedTable
      if (editingRow) {
        updatedTable = await intibakApi.updateRow(selectedStudent.id, editingRow.id, values)
      } else {
        updatedTable = await intibakApi.addRow(selectedStudent.id, values)
      }
      setIntibakRows(updatedTable.rows ?? [])
      setEquivalencyRatio(updatedTable.equivalencyRatio)
      setRowModalOpen(false)
    } catch (err) {
      if (err?.errorFields) return
      message.error('İşlem başarısız: ' + (err?.response?.data?.message ?? 'Bir hata oluştu'))
    } finally {
      setRowSaving(false)
    }
  }

  const handleDeleteRow = async (rowId) => {
    try {
      const updatedTable = await intibakApi.deleteRow(selectedStudent.id, rowId)
      setIntibakRows(updatedTable.rows ?? [])
      setEquivalencyRatio(updatedTable.equivalencyRatio)
    } catch (err) {
      message.error('Satır silinemedi: ' + (err?.response?.data?.message ?? 'Bir hata oluştu'))
    }
  }

  const intibakColumns = [
    {
      title: 'Kaynak Kodu',
      key: 'sourceCode',
      width: 120,
      render: (_, row) =>
        row.source2Code ? (
          <span>{row.sourceCode}<br /><Text type="secondary" style={{ fontSize: 11 }}>+ {row.source2Code}</Text></span>
        ) : row.sourceCode,
    },
    {
      title: 'Kaynak Ders Adı',
      key: 'sourceName',
      render: (_, row) =>
        row.source2Name ? (
          <span>{row.sourceName}<br /><Text type="secondary" style={{ fontSize: 11 }}>{row.source2Name}</Text></span>
        ) : row.sourceName,
    },
    {
      title: 'Kredi',
      key: 'sourceCredit',
      width: 60,
      render: (_, row) =>
        row.source2Credit != null
          ? `${row.sourceCredit}+${row.source2Credit}`
          : row.sourceCredit,
    },
    { title: 'Not', dataIndex: 'sourceGrade', key: 'sourceGrade', width: 55 },
    { title: 'İYTE Kodu', dataIndex: 'targetCode', key: 'targetCode', width: 100, render: (v) => v ?? '—' },
    { title: 'İYTE Ders Adı', dataIndex: 'targetName', key: 'targetName', render: (v) => v ?? '—' },
    { title: 'İYTE Kredi', dataIndex: 'targetCredit', key: 'targetCredit', width: 80, render: (v) => v ?? '—' },
    { title: 'İYTE Not', dataIndex: 'targetGrade', key: 'targetGrade', width: 75, render: (v) => v ?? '—' },
    {
      title: 'Denklik',
      dataIndex: 'equivalencyStatus',
      key: 'equivalencyStatus',
      width: 105,
      render: (val) => {
        const { label, tint } = EQUIVALENCY_DISPLAY[val] ?? { label: val, tint: TINT.gray }
        return <Tag style={{ ...tint, border: 'none', borderRadius: 6 }}>{label}</Tag>
      },
    },
    {
      title: '',
      key: 'actions',
      width: 120,
      render: (_, row) => (
        <Space size={4}>
          <Button size="small" onClick={() => openEditRow(row)}>Düzenle</Button>
          <Popconfirm
            title="Bu satırı silmek istiyor musunuz?"
            onConfirm={() => handleDeleteRow(row.id)}
            okText="Evet"
            cancelText="İptal"
          >
            <Button size="small" danger>Sil</Button>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  const renderEvaluatedExpanded = (record) => (
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
            <Text strong style={{ fontSize: 18 }}>{record.yksPuani}</Text>
          </div>
        </Col>
        <Col span={6}>
          <div style={{ background: '#fff', padding: 12, borderRadius: 6, border: '1px solid #f0f0f0' }}>
            <Text type="secondary" style={{ display: 'block', fontSize: 12 }}>Composite Score</Text>
            <Text strong style={{ fontSize: 18, color: '#8B1A2B' }}>{record.compositeScore}</Text>
          </div>
        </Col>
        <Col span={6}>
          <div style={{ background: '#fff', padding: 12, borderRadius: 6, border: '1px solid #f0f0f0' }}>
            <Text type="secondary" style={{ display: 'block', fontSize: 12 }}>Oto. Sıralama</Text>
            <Tag style={{ marginTop: 4, ...TINT.blue, border: 'none', borderRadius: 6 }}>{record.otoSiralama}</Tag>
          </div>
        </Col>
        <Col span={12}>
          <Text strong>Mevcut Üniversite:</Text>
          <Text style={{ marginLeft: 8 }}>{record.mevcutUni}</Text>
        </Col>
        <Col span={12}>
          <Text strong>Başvuru Tarihi:</Text>
          <Text style={{ marginLeft: 8 }}>{record.basvuruTarihi}</Text>
        </Col>
        <Col span={24}>
          <Text strong>YGK Değerlendirme Notu:</Text>
          <div style={{ marginTop: 8, background: '#fff', padding: 12, borderRadius: 6, border: '1px solid #f0f0f0' }}>
            <Text>{record.ygkNotu}</Text>
          </div>
        </Col>
      </Row>
    </div>
  )

  return (
    <div style={styles.page}>
      <Title level={2} style={{ marginBottom: 24 }}>YGK Değerlendirme Paneli</Title>

      <div style={styles.card}>
        <Alert
          message="Otomatik ve Manuel Değerlendirme Süreci"
          description="Sistem GPA/YKS kontrolü, composite score hesaplaması ve asil/yedek liste sıralamasını otomatik yapmıştır. Sizin göreviniz: Bölüme özel koşulları kontrol etmek ve İntibak (ders denklik) tablosunu hazırlamak."
          type="info"
          showIcon
          style={{ marginBottom: 24 }}
        />

        <Tabs
          activeKey={activeTab}
          onChange={setActiveTab}
          items={[
            {
              key: 'pending',
              label: `Değerlendirme Bekleyen ${PENDING_STUDENTS.length}`,
              children: (
                <Table
                  dataSource={PENDING_STUDENTS}
                  columns={[
                    { title: 'Ad Soyad', dataIndex: 'ad', key: 'ad', render: (_, r) => `${r.ad} ${r.soyad}` },
                    { title: 'Bölüm', dataIndex: 'bolum', key: 'bolum' },
                    { title: 'Skoru', dataIndex: 'compositeScore', key: 'score' },
                    {
                      title: 'İngilizce',
                      dataIndex: 'ingilizce',
                      key: 'eng',
                      render: (text) => (
                        <Tag style={{ ...(text === 'Onaylandı' ? TINT.green : TINT.amber), border: 'none', borderRadius: 6 }}>
                          {text}
                        </Tag>
                      ),
                    },
                    {
                      title: 'İşlem',
                      key: 'action',
                      render: (_, record) => (
                        <Button
                          type="primary"
                          size="small"
                          style={{ background: '#8B1A2B', borderColor: '#8B1A2B' }}
                          onClick={() => openEvaluationModal(record)}
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
              key: 'evaluated',
              label: `Değerlendirilenler ${EVALUATED_STUDENTS.length}`,
              children: (
                <Table
                  dataSource={EVALUATED_STUDENTS}
                  columns={[
                    { title: 'Ad Soyad', dataIndex: 'ad', key: 'ad', render: (_, r) => `${r.ad} ${r.soyad}` },
                    { title: 'Bölüm', dataIndex: 'bolum', key: 'bolum' },
                    { title: 'Skoru', dataIndex: 'compositeScore', key: 'score' },
                    {
                      title: 'Durum',
                      dataIndex: 'durum',
                      key: 'durum',
                      render: (text) => {
                        const tint = text === 'Onaylandı' ? TINT.green
                          : text.includes('İnceleme') ? TINT.blue
                          : TINT.amber
                        return <Tag style={{ ...tint, border: 'none', borderRadius: 6 }}>{text}</Tag>
                      },
                    },
                    {
                      title: '',
                      key: 'expand',
                      width: 48,
                      render: (_, record) => (
                        <Button
                          type="text"
                          icon={expandedEvaluated[record.id]
                            ? <UpOutlined style={{ color: '#8B1A2B' }} />
                            : <DownOutlined style={{ color: '#8B1A2B' }} />}
                          onClick={() => toggleEvaluatedExpand(record.id)}
                        />
                      ),
                    },
                  ]}
                  expandable={{
                    expandedRowRender: renderEvaluatedExpanded,
                    expandedRowKeys: evaluatedExpandedKeys,
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

      {/* ── Değerlendirme Modalı ── */}
      <Modal
        title="YGK Değerlendirmesi"
        open={evaluationModal}
        onCancel={() => setEvaluationModal(false)}
        width={960}
        style={{ top: 20 }}
        footer={[
          <Button key="cancel" onClick={() => setEvaluationModal(false)}>İptal</Button>,
          <Button key="submit" type="primary" danger>Değerlendirmeyi Tamamla ve İlet</Button>,
        ]}
      >
        {selectedStudent && (
          <div style={{ maxHeight: '75vh', overflowY: 'auto', overflowX: 'hidden', paddingRight: 10 }}>
            <Alert
              message={`${selectedStudent.ad} ${selectedStudent.soyad} - ${selectedStudent.bolum}`}
              type="info"
              style={{ marginBottom: 16, marginTop: 8 }}
            />

            <Row gutter={[16, 16]}>
              <Col span={24}>
                <Title level={4}>Öğrenci Bilgileri</Title>
              </Col>
              <Col span={12}>
                <Text strong>Mevcut Üniversite:</Text>
                <Text style={{ marginLeft: 8 }}>{selectedStudent.mevcutUni}</Text>
              </Col>
              <Col span={12}>
                <Text strong>Başvuru Tarihi:</Text>
                <Text style={{ marginLeft: 8 }}>{selectedStudent.basvuruTarihi}</Text>
              </Col>
              <Col span={12}>
                <Text strong>E-posta:</Text>
                <Text style={{ marginLeft: 8 }}>{selectedStudent.email}</Text>
              </Col>
              <Col span={12}>
                <Text strong>Telefon:</Text>
                <Text style={{ marginLeft: 8 }}>{selectedStudent.tel}</Text>
              </Col>
            </Row>

            <Divider />

            <Row gutter={[16, 16]}>
              <Col span={24}>
                <Title level={4}>Otomatik Hesaplanan Değerler</Title>
              </Col>
              <Col span={6}>
                <div style={{ background: '#f5f5f5', padding: 12, borderRadius: 6 }}>
                  <Text strong style={{ display: 'block' }}>GPA</Text>
                  <Text style={{ fontSize: 18, fontWeight: 'bold' }}>{selectedStudent.gpa}</Text>
                </div>
              </Col>
              <Col span={6}>
                <div style={{ background: '#f5f5f5', padding: 12, borderRadius: 6 }}>
                  <Text strong style={{ display: 'block' }}>YKS Puanı</Text>
                  <Text style={{ fontSize: 18, fontWeight: 'bold' }}>{selectedStudent.yksPuani}</Text>
                </div>
              </Col>
              <Col span={6}>
                <div style={{ background: '#f5f5f5', padding: 12, borderRadius: 6 }}>
                  <Text strong style={{ display: 'block' }}>Composite Score</Text>
                  <Text style={{ fontSize: 18, fontWeight: 'bold', color: '#8B1A2B' }}>{selectedStudent.compositeScore}</Text>
                </div>
              </Col>
              <Col span={6}>
                <div style={{ background: '#f5f5f5', padding: 12, borderRadius: 6 }}>
                  <Text strong style={{ display: 'block' }}>Oto. Sıralama</Text>
                  <Tag style={{ marginTop: 8, ...TINT.blue, border: 'none', borderRadius: 6 }}>{selectedStudent.otoSiralama}</Tag>
                </div>
              </Col>
              <Col span={24} style={{ marginTop: 8 }}>
                <Text strong>İngilizce Yeterlilik:</Text>
                <Tag style={{ marginLeft: 8, ...(selectedStudent.ingilizce === 'Onaylandı' ? TINT.green : TINT.amber), border: 'none', borderRadius: 6 }}>
                  {selectedStudent.ingilizceDetay}
                </Tag>
              </Col>
            </Row>

            <Divider />

            <Row gutter={[16, 16]}>
              <Col span={24}>
                <Title level={4}>Bölüme Özel Koşullar (Manuel Kontrol)</Title>
              </Col>
              <Col span={24}>
                <Radio.Group
                  value={conditionsDecision}
                  onChange={(e) => {
                    setConditionsDecision(e.target.value)
                    setShowConditionsWarning(e.target.value === 'rejected')
                  }}
                >
                  <Space direction="vertical" style={{ width: '100%' }}>
                    <div>
                      <Radio value="approved">
                        <span style={{ marginLeft: 8 }}>✓ Koşullar Karşılanıyor</span>
                      </Radio>
                      <Text style={{ marginLeft: 40, display: 'block', color: '#999' }}>Öğrenci bölümün belirlediği tüm özel koşulları sağlıyor.</Text>
                    </div>
                    <div>
                      <Radio value="rejected">
                        <span style={{ marginLeft: 8 }}>✗ Koşullar Karşılanmıyor</span>
                      </Radio>
                      <Text style={{ marginLeft: 40, display: 'block', color: '#999' }}>Öğrenci bir veya daha fazla bölüm koşulunu karşılamıyor.</Text>
                    </div>
                  </Space>
                </Radio.Group>
              </Col>
              {showConditionsWarning && (
                <Col span={24} style={{ marginTop: 12 }}>
                  <Alert
                    message="Yetersiz Koşullar"
                    description="Koşullar karşılanmadığında İntibak Tablosu seçeneği devre dışı bırakılacaktır. Lütfen aşağıdaki not kısmında hangi koşulların karşılanmadığını belirtiniz."
                    type="warning"
                    showIcon
                  />
                </Col>
              )}
            </Row>

            <Divider />

            {/* ── İntibak Tablosu ── */}
            <Row gutter={[16, 16]}>
              <Col span={24} style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <Title level={4} style={{ margin: 0 }}>İntibak Tablosu (Ders Denklik Tablosu)</Title>
                {equivalencyRatio != null && (
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
              {equivalencyRatio != null && equivalencyRatio < 0.8 && (
                <Col span={24}>
                  <Alert
                    type="warning"
                    showIcon
                    message={`Denklik oranı (%${Math.round(equivalencyRatio * 100)}) %80 eşiğinin altında — bu öğrenci yatay geçiş yapamaz.`}
                  />
                </Col>
              )}
              <Col span={24}>
                <Table
                  columns={intibakColumns}
                  dataSource={intibakRows}
                  rowKey="id"
                  loading={intibakLoading}
                  pagination={false}
                  size="small"
                  bordered
                  locale={{ emptyText: 'Henüz ders eklenmedi.' }}
                />
              </Col>
              <Col span={24}>
                <Button
                  type="primary"
                  danger
                  disabled={conditionsDecision === 'rejected'}
                  onClick={openAddRow}
                >
                  + Ders Ekle
                </Button>
              </Col>
            </Row>

            <Divider />

            <Row gutter={[16, 16]}>
              <Col span={24}>
                <Title level={4}>Genel Değerlendirme Notu</Title>
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

      {/* ── Satır Ekle / Düzenle Modalı ── */}
      <Modal
        title={editingRow ? 'Dersi Düzenle' : 'Ders Ekle'}
        open={rowModalOpen}
        onCancel={() => setRowModalOpen(false)}
        onOk={handleRowSubmit}
        okText={editingRow ? 'Güncelle' : 'Ekle'}
        cancelText="İptal"
        confirmLoading={rowSaving}
        width={680}
        destroyOnClose
      >
        <Form form={rowForm} layout="vertical" style={{ marginTop: 8 }}>
          <Divider orientation="left" orientationMargin={0} style={{ fontSize: 13, color: '#666' }}>
            Kaynak Ders (Mevcut Üniversite)
          </Divider>
          <Row gutter={12}>
            <Col span={8}>
              <Form.Item name="sourceCode" label="Ders Kodu" rules={[{ required: true, message: 'Zorunlu' }]}>
                <Input placeholder="BLM101" />
              </Form.Item>
            </Col>
            <Col span={10}>
              <Form.Item name="sourceName" label="Ders Adı" rules={[{ required: true, message: 'Zorunlu' }]}>
                <Input placeholder="Programlamaya Giriş" />
              </Form.Item>
            </Col>
            <Col span={3}>
              <Form.Item name="sourceCredit" label="Kredi" rules={[{ required: true, message: 'Zorunlu' }]}>
                <InputNumber min={0} style={{ width: '100%' }} />
              </Form.Item>
            </Col>
            <Col span={3}>
              <Form.Item name="sourceGrade" label="Not">
                <Select options={GRADE_OPTIONS} placeholder="-" allowClear />
              </Form.Item>
            </Col>
          </Row>

          <div style={{ marginBottom: 12 }}>
            <label style={{ cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 6, fontSize: 13 }}>
              <input
                type="checkbox"
                checked={dualSource}
                onChange={(e) => setDualSource(e.target.checked)}
              />
              2→1 eşleştirme (iki kaynak dersi tek İYTE dersine denk)
            </label>
          </div>

          {dualSource && (
            <Row gutter={12}>
              <Col span={8}>
                <Form.Item name="source2Code" label="2. Ders Kodu" rules={[{ required: true, message: 'Zorunlu' }]}>
                  <Input placeholder="BLM102" />
                </Form.Item>
              </Col>
              <Col span={10}>
                <Form.Item name="source2Name" label="2. Ders Adı" rules={[{ required: true, message: 'Zorunlu' }]}>
                  <Input placeholder="Veri Yapıları" />
                </Form.Item>
              </Col>
              <Col span={3}>
                <Form.Item name="source2Credit" label="Kredi" rules={[{ required: true, message: 'Zorunlu' }]}>
                  <InputNumber min={0} style={{ width: '100%' }} />
                </Form.Item>
              </Col>
              <Col span={3}>
                <Form.Item name="source2Grade" label="Not" rules={[{ required: true, message: 'Zorunlu' }]}>
                  <Select options={GRADE_OPTIONS} placeholder="-" />
                </Form.Item>
              </Col>
            </Row>
          )}

          <Divider orientation="left" orientationMargin={0} style={{ fontSize: 13, color: '#666' }}>
            İYTE Dersi
          </Divider>
          {watchedEquivalencyStatus === 'DENK_DEGIL' ? (
            <Alert
              type="info"
              showIcon
              message="Denk Değil seçildiğinde İYTE dersi girilmez."
              style={{ marginBottom: 16 }}
            />
          ) : (
            <Row gutter={12}>
              <Col span={8}>
                <Form.Item name="targetCode" label="Ders Kodu" rules={[{ required: true, message: 'Zorunlu' }]}>
                  <Input placeholder="CENG101" />
                </Form.Item>
              </Col>
              <Col span={10}>
                <Form.Item name="targetName" label="Ders Adı" rules={[{ required: true, message: 'Zorunlu' }]}>
                  <Input placeholder="Introduction to Programming" />
                </Form.Item>
              </Col>
              <Col span={3}>
                <Form.Item name="targetCredit" label="Kredi" rules={[{ required: true, message: 'Zorunlu' }]}>
                  <InputNumber min={0} style={{ width: '100%' }} />
                </Form.Item>
              </Col>
              <Col span={3}>
                <Form.Item name="targetGrade" label="Not">
                  <Select options={GRADE_OPTIONS} placeholder="-" allowClear />
                </Form.Item>
              </Col>
            </Row>
          )}

          <Form.Item
            name="equivalencyStatus"
            label="Denklik Durumu"
            rules={[{ required: true, message: 'Denklik durumu seçiniz' }]}
          >
            <Select options={EQUIVALENCY_OPTIONS} placeholder="Seçiniz" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default YGKDashboard
