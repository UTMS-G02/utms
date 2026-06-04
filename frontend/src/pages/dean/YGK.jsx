import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Tabs, Table, Button, Typography, Tag, Modal, Row, Col, Space, Input, Select, Radio, Divider, Alert } from 'antd'
import { DownloadOutlined, DownOutlined, UpOutlined } from '@ant-design/icons'

const { Title, Text } = Typography

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

const YGK = () => {
  const navigate = useNavigate()
  const [activeTab, setActiveTab] = useState('pending')
  const [expandedRows, setExpandedRows] = useState({})
  const [evaluationModal, setEvaluationModal] = useState(false)
  const [selectedStudent, setSelectedStudent] = useState(null)
  const [evalNote, setEvalNote] = useState('')
  const [conditionsDecision, setConditionsDecision] = useState('approved')
  const [showConditionsWarning, setShowConditionsWarning] = useState(false)

  const getStatusColor = (durum) => {
    if (durum.includes('Bekleniyor')) return 'orange'
    if (durum.includes('Onaylandı')) return 'green'
    if (durum.includes('İnceleme')) return 'blue'
    return 'default'
  }

  const toggleExpand = (id) => {
    setExpandedRows(prev => ({ ...prev, [id]: !prev[id] }))
  }

  const openEvaluationModal = (record) => {
    setSelectedStudent(record)
    setEvalNote('')
    setConditionsDecision('approved')
    setShowConditionsWarning(false)
    setEvaluationModal(true)
  }

  const renderPendingExpanded = (record) => (
    <div style={{ paddingLeft: 40, background: '#fafafa', padding: 16, borderRadius: 4 }}>
      <Row gutter={[16, 16]}>
        <Col span={12}>
          <Text strong>E-posta:</Text>
          <Text style={{ marginLeft: 8 }}>{record.email}</Text>
        </Col>
        <Col span={12}>
          <Text strong>Telefon:</Text>
          <Text style={{ marginLeft: 8 }}>{record.tel}</Text>
        </Col>
        <Col span={12}>
          <Text strong>GPA:</Text>
          <Text style={{ marginLeft: 8 }}>{record.gpa}</Text>
        </Col>
        <Col span={12}>
          <Text strong>YKS Puanı:</Text>
          <Text style={{ marginLeft: 8 }}>{record.yksPuani}</Text>
        </Col>
        <Col span={12}>
          <Text strong>Başvuru Tarihi:</Text>
          <Text style={{ marginLeft: 8 }}>{record.basvuruTarihi}</Text>
        </Col>
        <Col span={12}>
          <Text strong>Mevcut Üniversite:</Text>
          <Text style={{ marginLeft: 8 }}>{record.mevcutUni}</Text>
        </Col>
        <Col span={24}>
          <Text strong>İngilizce Yeterlilik:</Text>
          <Tag style={{ marginLeft: 8 }} color={record.ingilizce === 'Onaylandı' ? 'green' : 'orange'}>{record.ingilizceDetay}</Tag>
        </Col>
      </Row>
    </div>
  )

  const renderEvaluatedExpanded = (record) => (
    <div style={{ paddingLeft: 40, background: '#fafafa', padding: 16, borderRadius: 4 }}>
      <Row gutter={[16, 16]}>
        <Col span={12}>
          <Text strong>E-posta:</Text>
          <Text style={{ marginLeft: 8 }}>{record.email}</Text>
        </Col>
        <Col span={12}>
          <Text strong>Telefon:</Text>
          <Text style={{ marginLeft: 8 }}>{record.tel}</Text>
        </Col>
        <Col span={12}>
          <Text strong>GPA:</Text>
          <Text style={{ marginLeft: 8 }}>{record.gpa}</Text>
        </Col>
        <Col span={12}>
          <Text strong>YKS Puanı:</Text>
          <Text style={{ marginLeft: 8 }}>{record.yksPuani}</Text>
        </Col>
        <Col span={12}>
          <Text strong>Başvuru Tarihi:</Text>
          <Text style={{ marginLeft: 8 }}>{record.basvuruTarihi}</Text>
        </Col>
        <Col span={12}>
          <Text strong>Mevcut Üniversite:</Text>
          <Text style={{ marginLeft: 8 }}>{record.mevcutUni}</Text>
        </Col>
        <Col span={24}>
          <Text strong>YGK Değerlendirme Notu:</Text>
          <div style={{ marginTop: 8, background: '#fff', padding: 12, borderRadius: 4, border: '1px solid #f0f0f0' }}>
            <Text>{record.ygkNotu}</Text>
          </div>
        </Col>
      </Row>
    </div>
  )

  const pendingColumns = [
    { title: 'Ad Soyad', dataIndex: 'ad', key: 'ad', render: (_, record) => `${record.ad} ${record.soyad}` },
    { title: 'Bölüm', dataIndex: 'bolum', key: 'bolum' },
    { title: 'Composite Score', dataIndex: 'compositeScore', key: 'compositeScore' },
    { title: 'Oto. Sıralama', dataIndex: 'otoSiralama', key: 'otoSiralama', render: (otoSiralama) => <Tag color="blue">{otoSiralama}</Tag> },
    { title: 'İngilizce', dataIndex: 'ingilizce', key: 'ingilizce', render: (ingilizce) => <Tag color={ingilizce === 'Onaylandı' ? 'green' : 'orange'}>{ingilizce}</Tag> },
    { title: 'Durum', dataIndex: 'durum', key: 'durum', render: (durum) => <Tag color={getStatusColor(durum)}>{durum}</Tag> },
    {
      title: 'İşlem', key: 'action', render: (_, record) => (
        <Button type="primary" size="small" onClick={() => openEvaluationModal(record)}>Değerlendir</Button>
      ),
    },
    {
      title: '', key: 'expand', render: (_, record) => (
        <Button type="text" icon={expandedRows[record.id] ? <UpOutlined /> : <DownOutlined />} onClick={() => toggleExpand(record.id)} />
      ),
    },
  ]

  const evaluatedColumns = [
    { title: 'Ad Soyad', dataIndex: 'ad', key: 'ad', render: (_, record) => `${record.ad} ${record.soyad}` },
    { title: 'Bölüm', dataIndex: 'bolum', key: 'bolum' },
    { title: 'Composite Score', dataIndex: 'compositeScore', key: 'compositeScore' },
    { title: 'Oto. Sıralama', dataIndex: 'otoSiralama', key: 'otoSiralama', render: (otoSiralama) => <Tag color="blue">{otoSiralama}</Tag> },
    { title: 'İngilizce', dataIndex: 'ingilizce', key: 'ingilizce', render: (ingilizce) => <Tag color={ingilizce === 'Onaylandı' ? 'green' : 'orange'}>{ingilizce}</Tag> },
    { title: 'Durum', dataIndex: 'durum', key: 'durum', render: (durum) => <Tag color={getStatusColor(durum)}>{durum}</Tag> },
    {
      title: '', key: 'expand', render: (_, record) => (
        <Button type="text" icon={expandedRows[record.id] ? <UpOutlined /> : <DownOutlined />} onClick={() => toggleExpand(record.id)} />
      ),
    },
  ]

  return (
    <div style={{ padding: 24, fontFamily: "'DM Sans', sans-serif" }}>
      <Title level={2} style={{ marginBottom: 24 }}>YGK (Yönetim Genel Kurulu)</Title>

      <div style={{ background: '#fff', padding: 24, borderRadius: 8, boxShadow: '0 2px 8px rgba(0,0,0,0.06)' }}>
        <Alert
          message="Otomatik ve Manuel Değerlendirme Süreci"
          description="Sistem GPA/YKS kontrolü, composite score hesaplaması ve asil/yedek liste sıralamasını otomatik yapmıştır. Sizin göreviniz: Bölüme özel koşulları kontrol etmek ve İntibak (ders denklik) tablosunu hazırlamak."
          type="info"
          showIcon
          style={{ marginBottom: 16 }}
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
                  columns={pendingColumns}
                  dataSource={PENDING_STUDENTS}
                  pagination={false}
                  rowKey="id"
                  expandable={{
                    expandedRowRender: (record) => expandedRows[record.id] ? renderPendingExpanded(record) : null,
                    expandedRowKeys: Object.keys(expandedRows).filter(k => expandedRows[k]),
                  }}
                />
              ),
            },
            {
              key: 'evaluated',
              label: `Değerlendirilen ${EVALUATED_STUDENTS.length}`,
              children: (
                <Table
                  columns={evaluatedColumns}
                  dataSource={EVALUATED_STUDENTS}
                  pagination={false}
                  rowKey="id"
                  expandable={{
                    expandedRowRender: (record) => expandedRows[record.id] ? renderEvaluatedExpanded(record) : null,
                    expandedRowKeys: Object.keys(expandedRows).filter(k => expandedRows[k]),
                  }}
                />
              ),
            },
          ]}
        />

        <Space style={{ marginTop: 16 }}>
          <Button onClick={() => navigate('/dean/dekanlık-ofisi')}>Dekanlık Ofisi'ne Git</Button>
          <Button onClick={() => navigate('/dean/faculty-board')}>Fakülte Kurulu'na Git</Button>
        </Space>
      </div>

      <Modal
        title="YGK Değerlendirmesi"
        open={evaluationModal}
        onCancel={() => setEvaluationModal(false)}
        width={900}
        footer={[
          <Button key="cancel" onClick={() => setEvaluationModal(false)}>İptal</Button>,
          <Button key="submit" type="primary" danger>Değerlendirmeyi Tamamla ve İlet</Button>,
        ]}
      >
        {selectedStudent && (
          <div>
            <Row gutter={[16, 16]}>
              <Col span={24}>
                <Title level={4}>Öğrenci Bilgileri</Title>
              </Col>
              <Col span={12}>
                <Text strong>Ad Soyad:</Text>
                <Text style={{ marginLeft: 8 }}>{selectedStudent.ad} {selectedStudent.soyad}</Text>
              </Col>
              <Col span={12}>
                <Text strong>Bölüm:</Text>
                <Text style={{ marginLeft: 8 }}>{selectedStudent.bolum}</Text>
              </Col>
              <Col span={12}>
                <Text strong>Mevcut Üniversite:</Text>
                <Text style={{ marginLeft: 8 }}>{selectedStudent.mevcutUni}</Text>
              </Col>
            </Row>

            <Divider />

            <Row gutter={[16, 16]}>
              <Col span={24}>
                <Title level={4}>Otomatik Hesaplanan Değerler</Title>
              </Col>
              <Col span={6}>
                <div style={{ background: '#f5f5f5', padding: 12, borderRadius: 4 }}>
                  <Text strong style={{ display: 'block' }}>GPA</Text>
                  <Text style={{ fontSize: 18, fontWeight: 'bold' }}>{selectedStudent.gpa}</Text>
                </div>
              </Col>
              <Col span={6}>
                <div style={{ background: '#f5f5f5', padding: 12, borderRadius: 4 }}>
                  <Text strong style={{ display: 'block' }}>YKS Puanı</Text>
                  <Text style={{ fontSize: 18, fontWeight: 'bold' }}>{selectedStudent.yksPuani}</Text>
                </div>
              </Col>
              <Col span={6}>
                <div style={{ background: '#f5f5f5', padding: 12, borderRadius: 4 }}>
                  <Text strong style={{ display: 'block' }}>Composite Score</Text>
                  <Text style={{ fontSize: 18, fontWeight: 'bold', color: '#8B1A2B' }}>{selectedStudent.compositeScore}</Text>
                </div>
              </Col>
              <Col span={6}>
                <div style={{ background: '#f5f5f5', padding: 12, borderRadius: 4 }}>
                  <Text strong style={{ display: 'block' }}>Oto. Sıralama</Text>
                  <Tag color="blue" style={{ marginTop: 8 }}>{selectedStudent.otoSiralama}</Tag>
                </div>
              </Col>
              <Col span={24}>
                <Text strong>İngilizce Yeterlilik:</Text>
                <Tag style={{ marginLeft: 8 }} color={selectedStudent.ingilizce === 'Onaylandı' ? 'green' : 'orange'}>
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
                <Radio.Group onChange={(e) => {
                  setConditionsDecision(e.target.value)
                  if (e.target.value === 'rejected') {
                    setShowConditionsWarning(true)
                  } else {
                    setShowConditionsWarning(false)
                  }
                }} value={conditionsDecision}>
                  <Space direction="vertical" style={{ width: '100%' }}>
                    <div>
                      <Radio value="approved">
                        <span style={{ marginLeft: 8 }}>✓ Koşullar Karşılanıyor</span>
                      </Radio>
                      <Text style={{ marginLeft: 40, color: '#999' }}>Öğrenci bölümün belirlediği tüm özel koşulları sağlıyor.</Text>
                    </div>
                    <div>
                      <Radio value="rejected">
                        <span style={{ marginLeft: 8 }}>✗ Koşullar Karşılanmıyor</span>
                      </Radio>
                      <Text style={{ marginLeft: 40, color: '#999' }}>Öğrenci bir veya daha fazla bölüm koşulunu karşılamıyor.</Text>
                    </div>
                  </Space>
                </Radio.Group>
              </Col>
              {showConditionsWarning && (
                <Col span={24}>
                  <Alert
                    message="Yetersiz Koşullar"
                    description="Lütfen aşağıdaki not kısmında hangi koşulların karşılanmadığını belirtiniz."
                    type="warning"
                    showIcon
                  />
                </Col>
              )}
            </Row>

            <Divider />

            <Row gutter={[16, 16]}>
              <Col span={24}>
                <Title level={4}>İntibak Tablosu (Ders Denlik Tablosu)</Title>
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
                />
              </Col>
              <Col span={24}>
                <Button type="primary" danger>+ Ders Ekle</Button>
              </Col>
            </Row>

            <Divider />

            <Row gutter={[16, 16]}>
              <Col span={24}>
                <Title level={4}>Genel Değerlendirme Notu</Title>
              </Col>
              <Col span={24}>
                <Input.TextArea rows={4} value={evalNote} onChange={(e) => setEvalNote(e.target.value)} placeholder="Değerlendirme notunuzu giriniz..." />
              </Col>
            </Row>
          </div>
        )}
      </Modal>
    </div>
  )
}

export default YGK
