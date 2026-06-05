import { useState } from 'react'
import { Tabs, Table, Button, Typography, Tag, Modal, Row, Col, Space, Input, Radio, Divider, Alert } from 'antd'
import { DownOutlined, UpOutlined } from '@ant-design/icons'

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

const INTIBAK_DATA = [
  { mevcutDersKodu: 'BIL101', mevcutDersAdi: 'Programlama', kredi: 4, iyeteDersKodu: 'CENG102', iyeteDersAdi: 'Bilgisayar Pr', iyteKredi: 4, denklik: 'Tam Denk' },
  { mevcutDersKodu: 'MAT101', mevcutDersAdi: 'Calculus I', kredi: 4, iyeteDersKodu: 'MATH101', iyeteDersAdi: 'Calculus I', iyteKredi: 4, denklik: 'Tam Denk' },
]

const YGKDashboard = () => {
  const [activeTab, setActiveTab] = useState('pending')
  const [evaluationModal, setEvaluationModal] = useState(false)
  const [selectedStudent, setSelectedStudent] = useState(null)
  const [evalNote, setEvalNote] = useState('')
  const [conditionsDecision, setConditionsDecision] = useState('approved')
  const [showConditionsWarning, setShowConditionsWarning] = useState(false)
  const [expandedEvaluated, setExpandedEvaluated] = useState({})

  const openEvaluationModal = (record) => {
    setSelectedStudent(record)
    setEvalNote('')
    setConditionsDecision('approved')
    setShowConditionsWarning(false)
    setEvaluationModal(true)
  }

  const toggleEvaluatedExpand = (id) => {
    setExpandedEvaluated(prev => ({ ...prev, [id]: !prev[id] }))
  }

  const evaluatedExpandedKeys = Object.keys(expandedEvaluated)
    .filter(k => expandedEvaluated[k])
    .map(Number)

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

      <Modal
        title="YGK Değerlendirmesi"
        open={evaluationModal}
        onCancel={() => setEvaluationModal(false)}
        width={900}
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

            <Row gutter={[16, 16]}>
              <Col span={24}>
                <Title level={4}>İntibak Tablosu (Ders Denklik Tablosu)</Title>
              </Col>
              <Col span={24}>
                <Table
                  columns={[
                    { title: 'Mevcut Ders Kodu', dataIndex: 'mevcutDersKodu', key: 'mevcutDersKodu' },
                    { title: 'Mevcut Ders Adı', dataIndex: 'mevcutDersAdi', key: 'mevcutDersAdi' },
                    { title: 'Kredi', dataIndex: 'kredi', key: 'kredi' },
                    { title: 'İYTE Ders Kodu', dataIndex: 'iyeteDersKodu', key: 'iyeteDersKodu' },
                    { title: 'İYTE Ders Adı', dataIndex: 'iyeteDersAdi', key: 'iyeteDersAdi' },
                    { title: 'Kredi', dataIndex: 'iyteKredi', key: 'iyteKredi' },
                    { title: 'Denklik', dataIndex: 'denklik', key: 'denklik' },
                  ]}
                  dataSource={INTIBAK_DATA}
                  pagination={false}
                  size="small"
                  bordered
                />
              </Col>
              <Col span={24} style={{ marginTop: 8 }}>
                <Button type="primary" danger disabled={conditionsDecision === 'rejected'}>
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
    </div>
  )
}

export default YGKDashboard
