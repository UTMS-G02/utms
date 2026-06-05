import { useState } from 'react'
import { Tabs, Table, Button, Typography, Tag, Row, Col, Input, Select } from 'antd'
import { DownloadOutlined, DownOutlined, UpOutlined } from '@ant-design/icons'

const { Title, Text } = Typography

const TINT = {
  amber: { background: '#FEF3C7', color: '#B45309' },
  green: { background: '#D1FAE5', color: '#047857' },
  red: { background: '#FEE2E2', color: '#B91C1C' },
  blue: { background: '#DBEAFE', color: '#1E40AF' },
  purple: { background: '#E9D5FF', color: '#7E22CE' },
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
const OIDB_STUDENTS = [
  { id: 1, ad: 'Ayşe', soyad: 'Yılmaz', email: 'ayse.yilmaz@example.edu.tr', tel: '+90 532 123 4567', bolum: 'Bilgisayar Mühendisliği', durum: 'Akademik İnceleme Bekliyor', basvuruTarihi: '15.01.2024', fakulte: 'Mühendislik Fakültesi' },
  { id: 2, ad: 'Mehmet', soyad: 'Demir', email: 'mehmet.demir@example.edu.tr', tel: '+90 533 765 4321', bolum: 'Makina Mühendisliği', durum: 'Akademik İnceleme Bekliyor', basvuruTarihi: '12.01.2024', fakulte: 'Mühendislik Fakültesi' },
  { id: 3, ad: 'Zeynep', soyad: 'Aydın', email: 'zeynep.aydin@example.edu.tr', tel: '+90 534 123 4567', bolum: 'Bilgisayar Mühendisliği', durum: 'Akademik İnceleme Bekliyor', basvuruTarihi: '10.01.2024', fakulte: 'Mühendislik Fakültesi' },
]

const YGK_STUDENTS = [
  { id: 4, ad: 'Can', soyad: 'Öztürk', email: 'can.ozturk@example.edu.tr', tel: '+90 535 765 4321', bolum: 'Bilgisayar Mühendisliği', durum: 'YGK Değerlendirmesi Tamamlandı', basvuruTarihi: '08.01.2024', fakulte: 'Mühendislik Fakültesi', ygkKarari: 'Asil Liste', intibakRaporu: 'İntibak Raporunu Görüntüle', ygkNotu: 'Öğrencinin transkripti incelenmiş, tüm dersler intibak edilebilir niteliktedir. Asil listeye alınması önerilmektedir.' },
  { id: 5, ad: 'Elif', soyad: 'Şahin', email: 'elif.sahin@example.edu.tr', tel: '+90 536 123 4567', bolum: 'Makina Mühendisliği', durum: 'YGK Değerlendirmesi Tamamlandı', basvuruTarihi: '05.01.2024', fakulte: 'Mühendislik Fakültesi', ygkKarari: 'Yedek Liste', intibakRaporu: 'İntibak Raporunu Görüntüle', ygkNotu: 'Öğrenci ön koşulları yerine getirmiştir.' },
]

const FACULTY_STUDENTS = [
  { id: 6, ad: 'Burak', soyad: 'Yıldız', email: 'burak.yildiz@example.edu.tr', tel: '+90 537 765 4321', bolum: 'Bilgisayar Mühendisliği', durum: 'Onaylandı', basvuruTarihi: '03.01.2024', fakulte: 'Mühendislik Fakültesi', ygkKarari: 'Asil Liste', kurulKarari: 'Onaylandı', kurulNotu: "YGK değerlendirmesi ve intibak raporu incelendi. Başvuru onaylanmıştır. ÖIDB'ye iletilmesi uygun görülmüştür." },
  { id: 7, ad: 'Deniz', soyad: 'Kaya', email: 'deniz.kaya@example.edu.tr', tel: '+90 538 123 4567', bolum: 'Makina Mühendisliği', durum: 'Reddedildi', basvuruTarihi: '18.01.2024', fakulte: 'Mühendislik Fakültesi', ygkKarari: 'Yedek Liste', kurulKarari: 'Reddedildi', kurulNotu: 'Bölüm kapasite dolmuştur. Başvuru reddedilmiştir.' },
]

function StatusBadge({ durum }) {
  let tint
  if (durum.includes('Bekliyor')) tint = TINT.amber
  else if (durum.includes('Tamamlandı')) tint = TINT.purple
  else if (durum.includes('Onaylandı')) tint = TINT.green
  else if (durum.includes('Reddedildi')) tint = TINT.red
  else tint = TINT.gray

  return (
    <span style={{
      ...tint,
      display: 'inline-flex', alignItems: 'center', gap: 6,
      fontSize: 12, fontWeight: 600, padding: '3px 12px',
      borderRadius: 999, whiteSpace: 'nowrap',
    }}>
      <span style={{ width: 7, height: 7, borderRadius: '50%', background: tint.color }} />
      {durum}
    </span>
  )
}

const DekanlikOfisi = () => {
  const [activeTab, setActiveTab] = useState('oidb')
  const [expandedRows, setExpandedRows] = useState({})
  const [searchText, setSearchText] = useState('')
  const [filterType, setFilterType] = useState('tumu')
  const [selectedRowKeys, setSelectedRowKeys] = useState([])

  const toggleExpand = (id) => {
    setExpandedRows(prev => ({ ...prev, [id]: !prev[id] }))
  }

  const handleTabChange = (key) => {
    setActiveTab(key)
    setSelectedRowKeys([])
  }

  const rowSelection = {
    selectedRowKeys,
    onChange: (newSelectedRowKeys) => setSelectedRowKeys(newSelectedRowKeys),
  }

  const getActionButtonText = () => {
    const count = selectedRowKeys.length
    if (activeTab === 'oidb') return `YGK'ya İlet (${count})`
    if (activeTab === 'ygk') return `Fakülte Kurulu'na İlet (${count})`
    if (activeTab === 'faculty') return `ÖİDB'ye Gönder (${count})`
    return `İşlem Yap (${count})`
  }

  const expandedRowKeys = Object.keys(expandedRows)
    .filter(k => expandedRows[k])
    .map(Number)

  const filterBySearch = (list) => {
    const q = searchText.trim().toLowerCase()
    if (!q) return list
    return list.filter(s =>
      `${s.ad} ${s.soyad}`.toLowerCase().includes(q) ||
      s.email.toLowerCase().includes(q) ||
      s.bolum.toLowerCase().includes(q)
    )
  }

  const filteredOIDB = filterBySearch(OIDB_STUDENTS)
  const filteredYGK = filterBySearch(YGK_STUDENTS)
  const filteredFaculty = filterBySearch(FACULTY_STUDENTS)

  const expandToggleCol = {
    title: '',
    key: 'expand',
    width: 48,
    render: (_, record) => (
      <Button
        type="text"
        icon={expandedRows[record.id]
          ? <UpOutlined style={{ color: '#8B1A2B' }} />
          : <DownOutlined style={{ color: '#8B1A2B' }} />}
        onClick={() => toggleExpand(record.id)}
      />
    ),
  }

  const commonColumns = [
    { title: 'Ad Soyad', dataIndex: 'ad', key: 'ad', render: (_, r) => `${r.ad} ${r.soyad}` },
    { title: 'E-posta', dataIndex: 'email', key: 'email' },
    { title: 'Telefon', dataIndex: 'tel', key: 'tel' },
    { title: 'Bölüm', dataIndex: 'bolum', key: 'bolum' },
    {
      title: 'Durum',
      dataIndex: 'durum',
      key: 'durum',
      render: (durum) => <StatusBadge durum={durum} />,
    },
    { title: 'Başvuru Tarihi', dataIndex: 'basvuruTarihi', key: 'basvuruTarihi' },
    expandToggleCol,
  ]

  const renderOIDBExpanded = (record) => (
    <div style={{ padding: '12px 24px', background: '#fafafa' }}>
      <Row gutter={[16, 8]}>
        <Col span={12}><Text strong>Fakülte:</Text><Text style={{ marginLeft: 8 }}>{record.fakulte}</Text></Col>
        <Col span={12}><Text strong>Başvuru Tarihi:</Text><Text style={{ marginLeft: 8 }}>{record.basvuruTarihi}</Text></Col>
      </Row>
    </div>
  )

  const renderYGKExpanded = (record) => (
    <div style={{ padding: '12px 24px', background: '#fafafa' }}>
      <Row gutter={[16, 12]}>
        <Col span={12}><Text strong>Fakülte:</Text><Text style={{ marginLeft: 8 }}>{record.fakulte}</Text></Col>
        <Col span={12}>
          <Text strong>YGK Kararı:</Text>
          <Tag style={{ marginLeft: 8, ...(record.ygkKarari === 'Asil Liste' ? TINT.green : TINT.amber), border: 'none', borderRadius: 6 }}>
            {record.ygkKarari}
          </Tag>
        </Col>
        <Col span={24}>
          <Text strong>İntibak Raporu:</Text>
          <Button type="link" icon={<DownloadOutlined />} style={{ paddingLeft: 8 }}>{record.intibakRaporu}</Button>
        </Col>
        <Col span={24}>
          <Text strong>YGK Notu:</Text>
          <div style={{ marginTop: 8, background: '#fff', padding: 12, borderRadius: 6, border: '1px solid #f0f0f0' }}>
            <Text>{record.ygkNotu}</Text>
          </div>
        </Col>
      </Row>
    </div>
  )

  const renderFacultyExpanded = (record) => (
    <div style={{ padding: '12px 24px', background: '#fafafa' }}>
      <Row gutter={[16, 12]}>
        <Col span={12}><Text strong>Fakülte:</Text><Text style={{ marginLeft: 8 }}>{record.fakulte}</Text></Col>
        <Col span={12}>
          <Text strong>YGK Kararı:</Text>
          <Tag style={{ marginLeft: 8, ...(record.ygkKarari === 'Asil Liste' ? TINT.green : TINT.amber), border: 'none', borderRadius: 6 }}>
            {record.ygkKarari}
          </Tag>
        </Col>
        <Col span={12}>
          <Text strong>Kurul Kararı:</Text>
          <Tag style={{ marginLeft: 8, ...(record.kurulKarari === 'Onaylandı' ? TINT.green : TINT.red), border: 'none', borderRadius: 6 }}>
            {record.kurulKarari}
          </Tag>
        </Col>
        <Col span={24}>
          <Text strong>Kurul Notu:</Text>
          <div style={{ marginTop: 8, background: '#fff', padding: 12, borderRadius: 6, border: '1px solid #f0f0f0' }}>
            <Text>{record.kurulNotu}</Text>
          </div>
        </Col>
      </Row>
    </div>
  )

  return (
    <div style={styles.page}>
      <Title level={2} style={{ marginBottom: 24 }}>Dekanlık Ofisi</Title>

      <div style={styles.card}>
        <Row gutter={16} style={{ marginBottom: 16 }}>
          <Col span={8}>
            <Input
              placeholder="Ad, Soyad veya Numara ile ara..."
              value={searchText}
              onChange={(e) => setSearchText(e.target.value)}
            />
          </Col>
          <Col span={8}>
          </Col>
          <Col span={8} style={{ textAlign: 'right' }}>
            <Button
              type="primary"
              style={{
                background: selectedRowKeys.length > 0 ? '#8B1A2B' : undefined,
                borderColor: selectedRowKeys.length > 0 ? '#8B1A2B' : undefined,
              }}
              disabled={selectedRowKeys.length === 0}
            >
              {getActionButtonText()}
            </Button>
          </Col>
        </Row>

        <Tabs
          activeKey={activeTab}
          onChange={handleTabChange}
          items={[
            {
              key: 'oidb',
              label: `ÖİDB'den Gelen ${filteredOIDB.length}`,
              children: (
                <Table
                  columns={commonColumns}
                  dataSource={filteredOIDB}
                  pagination={false}
                  rowKey="id"
                  rowSelection={rowSelection}
                  expandable={{
                    expandedRowRender: renderOIDBExpanded,
                    expandedRowKeys,
                    showExpandColumn: false,
                  }}
                  size="small"
                />
              ),
            },
            {
              key: 'ygk',
              label: `YGK'dan Gelen ${filteredYGK.length}`,
              children: (
                <Table
                  columns={commonColumns}
                  dataSource={filteredYGK}
                  pagination={false}
                  rowKey="id"
                  rowSelection={rowSelection}
                  expandable={{
                    expandedRowRender: renderYGKExpanded,
                    expandedRowKeys,
                    showExpandColumn: false,
                  }}
                  size="small"
                />
              ),
            },
            {
              key: 'faculty',
              label: `Fakülte Kurulu'ndan Gelen ${filteredFaculty.length}`,
              children: (
                <Table
                  columns={commonColumns}
                  dataSource={filteredFaculty}
                  pagination={false}
                  rowKey="id"
                  rowSelection={rowSelection}
                  expandable={{
                    expandedRowRender: renderFacultyExpanded,
                    expandedRowKeys,
                    showExpandColumn: false,
                  }}
                  size="small"
                />
              ),
            },
          ]}
        />

        <div style={{ marginTop: 16 }}>
          <Text type="secondary">
            {selectedRowKeys.length > 0
              ? `${selectedRowKeys.length} başvuru seçildi`
              : `${activeTab === 'oidb' ? filteredOIDB.length : activeTab === 'ygk' ? filteredYGK.length : filteredFaculty.length} başvuru gösteriliyor`}
          </Text>
        </div>
      </div>
    </div>
  )
}

export default DekanlikOfisi
