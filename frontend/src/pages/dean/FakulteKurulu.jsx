import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Tabs, Table, Button, Typography, Tag, Modal, Row, Col, Space, Input, Select, Divider } from 'antd'
import { DownloadOutlined, DownOutlined, UpOutlined } from '@ant-design/icons'

const { Title, Text } = Typography

// Mock data
const PENDING_STUDENTS = [
  { id: 1, ad: 'Can', soyad: 'Öztürk', email: 'can.ozturk@example.edu.tr', tel: '+90 535 765 4321', bolum: 'Bilgisayar Mühendisliği', ygkKarari: 'Asil Liste', basvuruTarihi: '08.01.2024', durum: 'Değerlendirme Bekleniyor', fakulte: 'Mühendislik Fakültesi', mevcutUni: 'Gazi Üniversitesi', gpa: 3.45, intibakRaporu: 'İntibak Raporunu Görüntüle', ygkNotu: 'Öğrencinin transkripti incelenmiş, tüm dersler intibak edilebilir niteliktedir. Asil listeye alınması önerilmektedir.' },
  { id: 2, ad: 'Elif', soyad: 'Şahin', email: 'elif.sahin@example.edu.tr', tel: '+90 536 123 4567', bolum: 'Makina Mühendisliği', ygkKarari: 'Yedek Liste', basvuruTarihi: '05.01.2024', durum: 'Değerlendirme Bekleniyor', fakulte: 'Mühendislik Fakültesi', mevcutUni: 'İTÜ', gpa: 3.2, intibakRaporu: 'İntibak Raporunu Görüntüle', ygkNotu: 'Öğrenci ön koşulları yerine getirmiştir.' },
  { id: 3, ad: 'Ahmet', soyad: 'Yılmaz', email: 'ahmet.yilmaz@example.edu.tr', tel: '+90 535 234 5678', bolum: 'Elektrik Mühendisliği', ygkKarari: 'Asil Liste', basvuruTarihi: '10.01.2024', durum: 'Değerlendirme Bekleniyor', fakulte: 'Mühendislik Fakültesi', mevcutUni: 'Orta Doğu Teknik Üniversitesi', gpa: 3.65, intibakRaporu: 'İntibak Raporunu Görüntüle', ygkNotu: 'Başarılı bir öğrencidir. Değerlendirme önerilir.' },
]

const EVALUATED_STUDENTS = [
  { id: 4, ad: 'Burak', soyad: 'Yıldız', email: 'burak.yildiz@example.edu.tr', tel: '+90 537 765 4321', bolum: 'Bilgisayar Mühendisliği', ygkKarari: 'Asil Liste', basvuruTarihi: '03.01.2024', durum: 'Onaylandı', fakulte: 'Mühendislik Fakültesi', mevcutUni: 'İTÜ', gpa: 3.78, kurulKarari: 'Onaylandı', kurulNotu: 'YGK değerlendirmesi ve intibak raporu incelendi. Başvuru onaylanmıştır. ÖIDB\'ye iletilmesi uygun görülmüştür.' },
  { id: 5, ad: 'Deniz', soyad: 'Kaya', email: 'deniz.kaya@example.edu.tr', tel: '+90 538 123 4567', bolum: 'Makina Mühendisliği', ygkKarari: 'Yedek Liste', basvuruTarihi: '18.01.2024', durum: 'Reddedildi', fakulte: 'Mühendislik Fakültesi', mevcutUni: 'Gazi Üniversitesi', gpa: 2.8, kurulKarari: 'Reddedildi', kurulNotu: 'Bölüm kapasite dolmuştur. Başvuru reddedilmiştir.' },
]

const INTIBAK_DATA = [
  { mevcutDersKodu: 'BIL101', mevcutDersAdi: 'Programlama', kredi: 4, iyeteDersKodu: 'CENG102', iyeteDersAdi: 'Bilgisayar Pr', iyteKredi: 4, denklik: 'Tam Denk' },
  { mevcutDersKodu: 'BIL102', mevcutDersAdi: 'İleri Programlama', kredi: 4, iyeteDersKodu: 'CENG103', iyeteDersAdi: 'İleri Programlama', iyteKredi: 4, denklik: 'Tam Denk' },
]

const FakulteKurulu = () => {
  const navigate = useNavigate()
  const [activeTab, setActiveTab] = useState('pending')
  const [expandedRows, setExpandedRows] = useState({})
  const [evaluationModal, setEvaluationModal] = useState(false)
  const [selectedStudent, setSelectedStudent] = useState(null)
  const [evalNote, setEvalNote] = useState('')

  const getStatusColor = (durum) => {
    if (durum.includes('Bekleniyor')) return 'orange'
    if (durum.includes('Onaylandı')) return 'green'
    if (durum.includes('Reddedildi')) return 'red'
    return 'default'
  }

  const toggleExpand = (id) => {
    setExpandedRows(prev => ({ ...prev, [id]: !prev[id] }))
  }

  const openEvaluationModal = (record) => {
    setSelectedStudent(record)
    setEvalNote('')
    setEvaluationModal(true)
  }

  const renderPendingExpanded = (record) => (
    <div style={{ paddingLeft: 40, background: '#fafafa', padding: 16, borderRadius: 4 }}>
      <Row gutter={[16, 16]}>
        <Col span={12}>
          <Text strong>Fakülte:</Text>
          <Text style={{ marginLeft: 8 }}>{record.fakulte}</Text>
        </Col>
        <Col span={12}>
          <Text strong>Mevcut Üniversite:</Text>
          <Text style={{ marginLeft: 8 }}>{record.mevcutUni}</Text>
        </Col>
        <Col span={12}>
          <Text strong>GPA:</Text>
          <Text style={{ marginLeft: 8 }}>{record.gpa}</Text>
        </Col>
        <Col span={12}>
          <Text strong>YGK Kararı:</Text>
          <Tag style={{ marginLeft: 8 }} color={record.ygkKarari === 'Asil Liste' ? 'green' : 'orange'}>{record.ygkKarari}</Tag>
        </Col>
        <Col span={24}>
          <Text strong>İntibak Raporu:</Text>
          <Button type="link" icon={<DownloadOutlined />} style={{ paddingLeft: 8 }}>{record.intibakRaporu}</Button>
        </Col>
        <Col span={24}>
          <Text strong>YGK Notu:</Text>
          <div style={{ marginTop: 8, background: '#fff', padding: 12, borderRadius: 4, border: '1px solid #f0f0f0' }}>
            <Text>{record.ygkNotu}</Text>
          </div>
        </Col>
      </Row>
    </div>
  )

  const renderEvaluatedExpanded = (record) => (
    <div style={{ paddingLeft: 40, background: '#fafafa', padding: 16, borderRadius: 4 }}>
      <Row gutter={[16, 16]}>
        <Col span={12}>
          <Text strong>Fakülte:</Text>
          <Text style={{ marginLeft: 8 }}>{record.fakulte}</Text>
        </Col>
        <Col span={12}>
          <Text strong>Mevcut Üniversite:</Text>
          <Text style={{ marginLeft: 8 }}>{record.mevcutUni}</Text>
        </Col>
        <Col span={12}>
          <Text strong>GPA:</Text>
          <Text style={{ marginLeft: 8 }}>{record.gpa}</Text>
        </Col>
        <Col span={12}>
          <Text strong>YGK Kararı:</Text>
          <Tag style={{ marginLeft: 8 }} color={record.ygkKarari === 'Asil Liste' ? 'green' : 'orange'}>{record.ygkKarari}</Tag>
        </Col>
        <Col span={12}>
          <Text strong>Kurul Kararı:</Text>
          <Tag style={{ marginLeft: 8 }} color={record.kurulKarari === 'Onaylandı' ? 'green' : 'red'}>{record.kurulKarari}</Tag>
        </Col>
        <Col span={24}>
          <Text strong>Kurul Notu:</Text>
          <div style={{ marginTop: 8, background: '#fff', padding: 12, borderRadius: 4, border: '1px solid #f0f0f0' }}>
            <Text>{record.kurulNotu}</Text>
          </div>
        </Col>
      </Row>
    </div>
  )

  const pendingColumns = [
    { title: 'Ad Soyad', dataIndex: 'ad', key: 'ad', render: (_, record) => `${record.ad} ${record.soyad}` },
    { title: 'E-posta', dataIndex: 'email', key: 'email' },
    { title: 'Bölüm', dataIndex: 'bolum', key: 'bolum' },
    { title: 'YGK Kararı', dataIndex: 'ygkKarari', key: 'ygkKarari', render: (ygkKarari) => <Tag color={ygkKarari === 'Asil Liste' ? 'green' : 'orange'}>{ygkKarari}</Tag> },
    { title: 'Durum', dataIndex: 'durum', key: 'durum', render: (durum) => <Tag color={getStatusColor(durum)}>{durum}</Tag> },
    { title: 'Başvuru Tarihi', dataIndex: 'basvuruTarihi', key: 'basvuruTarihi' },
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
    { title: 'E-posta', dataIndex: 'email', key: 'email' },
    { title: 'Bölüm', dataIndex: 'bolum', key: 'bolum' },
    { title: 'YGK Kararı', dataIndex: 'ygkKarari', key: 'ygkKarari', render: (ygkKarari) => <Tag color={ygkKarari === 'Asil Liste' ? 'green' : 'orange'}>{ygkKarari}</Tag> },
    { title: 'Durum', dataIndex: 'durum', key: 'durum', render: (durum) => <Tag color={getStatusColor(durum)}>{durum}</Tag> },
    { title: 'Başvuru Tarihi', dataIndex: 'basvuruTarihi', key: 'basvuruTarihi' },
    {
      title: '', key: 'expand', render: (_, record) => (
        <Button type="text" icon={expandedRows[record.id] ? <UpOutlined /> : <DownOutlined />} onClick={() => toggleExpand(record.id)} />
      ),
    },
  ]

  return (
    <div style={{ padding: 24, fontFamily: "'DM Sans', sans-serif" }}>
      <Title level={2} style={{ marginBottom: 24 }}>Fakülte Kurulu</Title>

      <div style={{ background: '#fff', padding: 24, borderRadius: 8, boxShadow: '0 2px 8px rgba(0,0,0,0.06)' }}>
        <Tabs
          activeKey={activeTab}
          onChange={setActiveTab}
          items={[
            {
              key: 'pending',
              label: `Değerlendirme Bekleyenler ${PENDING_STUDENTS.length}`,
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
          <Button onClick={() => navigate('/dean/ygk')}>YGK'ya Git</Button>
        </Space>
      </div>

      <Modal
        title="Başvuru Değerlendirmesi"
        open={evaluationModal}
        onCancel={() => setEvaluationModal(false)}
        width={800}
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
                <Title level={4}>YGK Değerlendirmesi</Title>
              </Col>
              <Col span={24}>
                <Text strong>YGK Kararı:</Text>
                <Tag style={{ marginLeft: 8 }} color={selectedStudent.ygkKarari === 'Asil Liste' ? 'green' : 'orange'}>{selectedStudent.ygkKarari}</Tag>
              </Col>
              <Col span={24}>
                <Text strong>YGK Notu:</Text>
                <div style={{ marginTop: 8, background: '#f5f5f5', padding: 12, borderRadius: 4 }}>
                  <Text>{selectedStudent.ygkNotu}</Text>
                </div>
              </Col>
              <Col span={24}>
                <Button type="link" icon={<DownloadOutlined />}>{selectedStudent.intibakRaporu}</Button>
              </Col>
            </Row>

            <Divider />

            <Row gutter={[16, 16]}>
              <Col span={24}>
                <Title level={4}>Kurul Kararı</Title>
              </Col>
              <Col span={24}>
                <Space direction="vertical" style={{ width: '100%' }}>
                  <div>
                    <input type="radio" name="decision" value="approve" id="approve" defaultChecked />
                    <label htmlFor="approve" style={{ marginLeft: 8 }}>Başvuruyu Onayla</label>
                  </div>
                  <div>
                    <input type="radio" name="decision" value="reject" id="reject" />
                    <label htmlFor="reject" style={{ marginLeft: 8 }}>Başvuruyu Reddet</label>
                  </div>
                </Space>
              </Col>
            </Row>

            <Divider />

            <Row gutter={[16, 16]}>
              <Col span={24}>
                <Text strong>Değerlendirme Notu:</Text>
              </Col>
              <Col span={24}>
                <Input.TextArea rows={4} value={evalNote} onChange={(e) => setEvalNote(e.target.value)} placeholder="Değerlendirme notunuzu giriniz..." />
              </Col>
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
          </div>
        )}
      </Modal>
    </div>
  )
}

export default FakulteKurulu
