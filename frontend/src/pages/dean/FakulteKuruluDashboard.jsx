import { useState } from 'react'
import { Tabs, Table, Button, Typography, Tag, Modal, Row, Col, Space, Input, Radio, Divider } from 'antd'
import { DownloadOutlined, DownOutlined, UpOutlined } from '@ant-design/icons'

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
const PENDING_FACULTY_STUDENTS = [
  { id: 1, ad: 'Burak', soyad: 'Yıldız', email: 'burak.yildiz@example.edu.tr', tel: '+90 537 765 4321', bolum: 'Bilgisayar Mühendisliği', compositeScore: 90.5, otoSiralama: 'Asil Liste #1', durum: 'Onay Bekleniyor', basvuruTarihi: '03.01.2024', gpa: 3.78, yksPuani: 498, mevcutUni: 'İTÜ', intibakStatus: 'Tamamlandı', ygkKarari: 'Asil Liste', ygkNotu: 'Öğrencinin transkripti incelenmiş, tüm dersler intibak edilebilir niteliktedir. Asil listeye alınması önerilmektedir.', intibakRaporu: 'İntibak Raporunu Görüntüle' },
  { id: 2, ad: 'Deniz', soyad: 'Kaya', email: 'deniz.kaya@example.edu.tr', tel: '+90 538 123 4567', bolum: 'Makina Mühendisliği', compositeScore: 78.9, otoSiralama: 'Yedek Liste #5', durum: 'İnceleme Aşamasında', basvuruTarihi: '18.01.2024', gpa: 2.9, yksPuani: 420, mevcutUni: 'Gazi Üniversitesi', intibakStatus: 'Tamamlandı', ygkKarari: 'Yedek Liste', ygkNotu: 'Öğrenci ön koşulları yerine getirmiştir.', intibakRaporu: 'İntibak Raporunu Görüntüle' },
  { id: 3, ad: 'Ayşe', soyad: 'Yılmaz', email: 'ayse.yilmaz@example.edu.tr', tel: '+90 532 123 4567', bolum: 'İnşaat Mühendisliği', compositeScore: 85.3, otoSiralama: 'Asil Liste #2', durum: 'Onay Bekleniyor', basvuruTarihi: '12.01.2024', gpa: 3.5, yksPuani: 480, mevcutUni: 'Orta Doğu Teknik Üniversitesi', intibakStatus: 'Tamamlandı', ygkKarari: 'Asil Liste', ygkNotu: 'Başarılı bir öğrencidir. Değerlendirme önerilir.', intibakRaporu: 'İntibak Raporunu Görüntüle' },
]

const APPROVED_STUDENTS = [
  { id: 4, ad: 'Can', soyad: 'Öztürk', email: 'can.ozturk@example.edu.tr', tel: '+90 535 765 4321', bolum: 'Bilgisayar Mühendisliği', compositeScore: 82.5, otoSiralama: 'Asil Liste #3', durum: 'Onaylandı', basvuruTarihi: '08.01.2024', gpa: 3.45, yksPuani: 485, mevcutUni: 'Gazi Üniversitesi', intibakStatus: 'Onaylandı', finalNot: 'YGK değerlendirmesi ve intibak raporu incelendi. Başvuru onaylanmıştır.', ygkKarari: 'Asil Liste', ygkNotu: 'İntibak raporu olumlu.', intibakRaporu: 'İntibak Raporunu Görüntüle' },
  { id: 5, ad: 'Elif', soyad: 'Şahin', email: 'elif.sahin@example.edu.tr', tel: '+90 536 123 4567', bolum: 'Elektrik Mühendisliği', compositeScore: 75.2, otoSiralama: 'Yedek Liste #8', durum: 'Reddedildi', basvuruTarihi: '05.01.2024', gpa: 3.1, yksPuani: 450, mevcutUni: 'Teknik Üniversite', intibakStatus: 'Reddedildi', finalNot: 'Bölüm kapasitesi dolmuştur.', ygkKarari: 'Yedek Liste', ygkNotu: 'Eksik dersler tamamlanmalı.', intibakRaporu: 'İntibak Raporunu Görüntüle' },
]

const INTIBAK_DATA = [
  { mevcutDersKodu: 'BIL101', mevcutDersAdi: 'Programlama', kredi: 4, iyeteDersKodu: 'CENG102', iyeteDersAdi: 'Bilgisayar Pr', iyteKredi: 4, denklik: 'Tam Denk' },
  { mevcutDersKodu: 'BIL102', mevcutDersAdi: 'İleri Programlama', kredi: 4, iyeteDersKodu: 'CENG103', iyeteDersAdi: 'İleri Programlama', iyteKredi: 4, denklik: 'Tam Denk' },
]

const FakulteKuruluDashboard = () => {
  const [activeTab, setActiveTab] = useState('pending')
  const [decisionModal, setDecisionModal] = useState(false)
  const [selectedStudent, setSelectedStudent] = useState(null)
  const [evalNote, setEvalNote] = useState('')
  const [decision, setDecision] = useState('approve')
  const [expandedApproved, setExpandedApproved] = useState({})
  const [intibakExpanded, setIntibakExpanded] = useState(false)

  const openDecisionModal = (record) => {
    setSelectedStudent(record)
    setEvalNote('')
    setDecision('approve')
    setIntibakExpanded(false)
    setDecisionModal(true)
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
          <Text strong>YGK Kararı:</Text>
          <Tag style={{ marginLeft: 8, ...(record.ygkKarari === 'Asil Liste' ? TINT.green : TINT.amber), border: 'none', borderRadius: 6 }}>
            {record.ygkKarari}
          </Tag>
        </Col>
        <Col span={24}>
          <Button type="link" icon={<DownloadOutlined />} style={{ paddingLeft: 0 }}>{record.intibakRaporu}</Button>
        </Col>
        <Col span={24}>
          <Text strong>Kurul Notu:</Text>
          <div style={{ marginTop: 8, background: '#fff', padding: 12, borderRadius: 6, border: '1px solid #f0f0f0' }}>
            <Text>{record.finalNot}</Text>
          </div>
        </Col>
      </Row>
    </div>
  )

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
              label: `Karar Bekleniyor ${PENDING_FACULTY_STUDENTS.length}`,
              children: (
                <Table
                  dataSource={PENDING_FACULTY_STUDENTS}
                  columns={[
                    { title: 'Ad Soyad', dataIndex: 'ad', key: 'ad', render: (_, r) => `${r.ad} ${r.soyad}` },
                    { title: 'Bölüm', dataIndex: 'bolum', key: 'bolum' },
                    { title: 'Skoru', dataIndex: 'compositeScore', key: 'score' },
                    {
                      title: 'İntibak',
                      dataIndex: 'intibakStatus',
                      key: 'intibak',
                      render: (text) => (
                        <Tag style={{ ...(text === 'Tamamlandı' ? TINT.green : TINT.amber), border: 'none', borderRadius: 6 }}>
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
              label: `Sonuçlandırılanlar ${APPROVED_STUDENTS.length}`,
              children: (
                <Table
                  dataSource={APPROVED_STUDENTS}
                  columns={[
                    { title: 'Ad Soyad', dataIndex: 'ad', key: 'ad', render: (_, r) => `${r.ad} ${r.soyad}` },
                    { title: 'Bölüm', dataIndex: 'bolum', key: 'bolum' },
                    { title: 'Skoru', dataIndex: 'compositeScore', key: 'score' },
                    {
                      title: 'Karar',
                      dataIndex: 'durum',
                      key: 'durum',
                      render: (text) => {
                        const tint = text === 'Onaylandı' ? TINT.green : text === 'Reddedildi' ? TINT.red : TINT.amber
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
        onCancel={() => setDecisionModal(false)}
        width={800}
        style={{ top: 20 }}
        footer={[
          <Button key="cancel" onClick={() => setDecisionModal(false)}>İptal</Button>,
          <Button key="submit" type="primary" danger>Değerlendirmeyi Tamamla ve İlet</Button>,
        ]}
      >
        {selectedStudent && (
          <div style={{ maxHeight: '75vh', overflowY: 'auto', overflowX: 'hidden', paddingRight: 10 }}>
            <Row gutter={[16, 16]}>
              <Col span={24}>
                <Title level={4} style={{ marginTop: 16 }}>Öğrenci Bilgileri</Title>
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
              <Col span={12}>
                <Text strong>GPA:</Text>
                <Text style={{ marginLeft: 8 }}>{selectedStudent.gpa}</Text>
              </Col>
            </Row>

            <Divider />

            <Row gutter={[16, 16]}>
              <Col span={24}>
                <Title level={4}>YGK Değerlendirmesi</Title>
              </Col>
              <Col span={24}>
                <Text strong>YGK Kararı:</Text>
                <Tag style={{ marginLeft: 8, ...(selectedStudent.ygkKarari === 'Asil Liste' ? TINT.green : TINT.amber), border: 'none', borderRadius: 6 }}>
                  {selectedStudent.ygkKarari}
                </Tag>
              </Col>
              <Col span={24}>
                <Text strong>YGK Notu:</Text>
                <div style={{ marginTop: 8, background: '#f5f5f5', padding: 12, borderRadius: 6 }}>
                  <Text>{selectedStudent.ygkNotu}</Text>
                </div>
              </Col>
              <Col span={24}>
                <Button
                  type="link"
                  icon={intibakExpanded ? <UpOutlined /> : <DownOutlined />}
                  style={{ paddingLeft: 0 }}
                  onClick={() => setIntibakExpanded(prev => !prev)}
                >
                  {selectedStudent.intibakRaporu}
                </Button>
                {intibakExpanded && (
                  <div style={{ marginTop: 12 }}>
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
                  </div>
                )}
              </Col>
            </Row>

            <Divider />

            <Row gutter={[16, 16]}>
              <Col span={24}>
                <Title level={4}>Kurul Kararı</Title>
              </Col>
              <Col span={24}>
                <Radio.Group value={decision} onChange={(e) => setDecision(e.target.value)}>
                  <Space direction="vertical">
                    <Radio value="approve">Başvuruyu Onayla</Radio>
                    <Radio value="reject">Başvuruyu Reddet</Radio>
                  </Space>
                </Radio.Group>
              </Col>
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
