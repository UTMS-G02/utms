import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Tabs, Table, Button, Typography, Tag, Modal, Row, Col, Space, Divider, Input, Select, Badge } from 'antd'
import { DownloadOutlined, DownOutlined, UpOutlined } from '@ant-design/icons'

const { Title, Text } = Typography

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
  { id: 6, ad: 'Burak', soyad: 'Yıldız', email: 'burak.yildiz@example.edu.tr', tel: '+90 537 765 4321', bolum: 'Bilgisayar Mühendisliği', durum: 'Onaylandı', basvuruTarihi: '03.01.2024', fakulte: 'Mühendislik Fakültesi', ygkKarari: 'Asil Liste', kurulKarari: 'Onaylandı', kurulNotu: 'YGK değerlendirmesi ve intibak raporu incelendi. Başvuru onaylanmıştır. ÖIDB\'ye iletilmesi uygun görülmüştür.' },
  { id: 7, ad: 'Deniz', soyad: 'Kaya', email: 'deniz.kaya@example.edu.tr', tel: '+90 538 123 4567', bolum: 'Makina Mühendisliği', durum: 'Reddedildi', basvuruTarihi: '18.01.2024', fakulte: 'Mühendislik Fakültesi', ygkKarari: 'Yedek Liste', kurulKarari: 'Reddedildi', kurulNotu: 'Bölüm kapasite dolmuştur. Başvuru reddedilmiştir.' },
]

const DekanlikOfisi = () => {
  const navigate = useNavigate()
  const [activeTab, setActiveTab] = useState('oidb')
  const [expandedRows, setExpandedRows] = useState({})
  const [searchText, setSearchText] = useState('')
  const [filterType, setFilterType] = useState('tumu')

  const getStatusColor = (durum) => {
    if (durum.includes('Bekliyor')) return 'orange'
    if (durum.includes('Tamamlandı')) return 'blue'
    if (durum.includes('Onaylandı')) return 'green'
    if (durum.includes('Reddedildi')) return 'red'
    return 'default'
  }

  const toggleExpand = (id) => {
    setExpandedRows(prev => ({ ...prev, [id]: !prev[id] }))
  }

  const renderOIDBExpanded = (record) => (
    <div style={{ paddingLeft: 40, background: '#fafafa', padding: 16, borderRadius: 4 }}>
      <Row gutter={16}>
        <Col span={24}>
          <Text strong>Fakülte:</Text>
          <Text style={{ marginLeft: 8 }}>{record.fakulte}</Text>
        </Col>
      </Row>
    </div>
  )

  const renderYGKExpanded = (record) => (
    <div style={{ paddingLeft: 40, background: '#fafafa', padding: 16, borderRadius: 4 }}>
      <Row gutter={[16, 16]}>
        <Col span={24}>
          <Text strong>Fakülte:</Text>
          <Text style={{ marginLeft: 8 }}>{record.fakulte}</Text>
        </Col>
        <Col span={24}>
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

  const renderFacultyExpanded = (record) => (
    <div style={{ paddingLeft: 40, background: '#fafafa', padding: 16, borderRadius: 4 }}>
      <Row gutter={[16, 16]}>
        <Col span={24}>
          <Text strong>Fakülte:</Text>
          <Text style={{ marginLeft: 8 }}>{record.fakulte}</Text>
        </Col>
        <Col span={24}>
          <Text strong>YGK Kararı:</Text>
          <Tag style={{ marginLeft: 8 }} color={record.ygkKarari === 'Asil Liste' ? 'green' : 'orange'}>{record.ygkKarari}</Tag>
        </Col>
        <Col span={24}>
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

  const renderOIDBTable = () => {
    const columns = [
      { title: 'Ad Soyad', dataIndex: 'ad', key: 'ad', render: (_, record) => `${record.ad} ${record.soyad}` },
      { title: 'E-posta', dataIndex: 'email', key: 'email' },
      { title: 'Telefon', dataIndex: 'tel', key: 'tel' },
      { title: 'Bölüm', dataIndex: 'bolum', key: 'bolum' },
      { title: 'Durum', dataIndex: 'durum', key: 'durum', render: (durum) => <Tag color={getStatusColor(durum)}>{durum}</Tag> },
      { title: 'Başvuru Tarihi', dataIndex: 'basvuruTarihi', key: 'basvuruTarihi' },
      {
        title: '', key: 'expand', render: (_, record) => (
          <Button type="text" icon={expandedRows[record.id] ? <UpOutlined /> : <DownOutlined />} onClick={() => toggleExpand(record.id)} />
        ),
      },
    ]

    return (
      <Table
        columns={columns}
        dataSource={OIDB_STUDENTS}
        pagination={false}
        rowKey="id"
        expandable={{
          expandedRowRender: (record) => expandedRows[record.id] ? renderOIDBExpanded(record) : null,
          expandedRowKeys: Object.keys(expandedRows).filter(k => expandedRows[k]),
        }}
        rowClassName={() => 'expandable-row'}
      />
    )
  }

  const renderYGKTable = () => {
    const columns = [
      { title: 'Ad Soyad', dataIndex: 'ad', key: 'ad', render: (_, record) => `${record.ad} ${record.soyad}` },
      { title: 'E-posta', dataIndex: 'email', key: 'email' },
      { title: 'Telefon', dataIndex: 'tel', key: 'tel' },
      { title: 'Bölüm', dataIndex: 'bolum', key: 'bolum' },
      { title: 'Durum', dataIndex: 'durum', key: 'durum', render: (durum) => <Tag color={getStatusColor(durum)}>{durum}</Tag> },
      { title: 'Başvuru Tarihi', dataIndex: 'basvuruTarihi', key: 'basvuruTarihi' },
      {
        title: '', key: 'expand', render: (_, record) => (
          <Button type="text" icon={expandedRows[record.id] ? <UpOutlined /> : <DownOutlined />} onClick={() => toggleExpand(record.id)} />
        ),
      },
    ]

    return (
      <Table
        columns={columns}
        dataSource={YGK_STUDENTS}
        pagination={false}
        rowKey="id"
        expandable={{
          expandedRowRender: (record) => expandedRows[record.id] ? renderYGKExpanded(record) : null,
          expandedRowKeys: Object.keys(expandedRows).filter(k => expandedRows[k]),
        }}
        rowClassName={() => 'expandable-row'}
      />
    )
  }

  const renderFacultyTable = () => {
    const columns = [
      { title: 'Ad Soyad', dataIndex: 'ad', key: 'ad', render: (_, record) => `${record.ad} ${record.soyad}` },
      { title: 'E-posta', dataIndex: 'email', key: 'email' },
      { title: 'Telefon', dataIndex: 'tel', key: 'tel' },
      { title: 'Bölüm', dataIndex: 'bolum', key: 'bolum' },
      { title: 'Durum', dataIndex: 'durum', key: 'durum', render: (durum) => <Tag color={getStatusColor(durum)}>{durum}</Tag> },
      { title: 'Başvuru Tarihi', dataIndex: 'basvuruTarihi', key: 'basvuruTarihi' },
      {
        title: '', key: 'expand', render: (_, record) => (
          <Button type="text" icon={expandedRows[record.id] ? <UpOutlined /> : <DownOutlined />} onClick={() => toggleExpand(record.id)} />
        ),
      },
    ]

    return (
      <Table
        columns={columns}
        dataSource={FACULTY_STUDENTS}
        pagination={false}
        rowKey="id"
        expandable={{
          expandedRowRender: (record) => expandedRows[record.id] ? renderFacultyExpanded(record) : null,
          expandedRowKeys: Object.keys(expandedRows).filter(k => expandedRows[k]),
        }}
        rowClassName={() => 'expandable-row'}
      />
    )
  }

  return (
    <div style={{ padding: 24, fontFamily: "'DM Sans', sans-serif" }}>
      <Title level={2} style={{ marginBottom: 24 }}>Dekanlık Ofisi</Title>

      <div style={{ background: '#fff', padding: 24, borderRadius: 8, boxShadow: '0 2px 8px rgba(0,0,0,0.06)' }}>
        <Row gutter={16} style={{ marginBottom: 16 }}>
          <Col span={8}>
            <Input placeholder="Ad, Soyad veya Numara ile ara..." value={searchText} onChange={(e) => setSearchText(e.target.value)} />
          </Col>
          <Col span={8}>
            <Select value={filterType} onChange={setFilterType} style={{ width: '100%' }}>
              <Select.Option value="tumu">Tümü</Select.Option>
            </Select>
          </Col>
          <Col span={8} style={{ textAlign: 'right' }}>
            <Button type="primary" danger>YGK'ya İlet (0)</Button>
          </Col>
        </Row>

        <Tabs
          activeKey={activeTab}
          onChange={setActiveTab}
          items={[
            {
              key: 'oidb',
              label: `ÖİDB'den Gelen ${OIDB_STUDENTS.length}`,
              children: renderOIDBTable(),
            },
            {
              key: 'ygk',
              label: `YGK'dan Gelen ${YGK_STUDENTS.length}`,
              children: renderYGKTable(),
            },
            {
              key: 'faculty',
              label: `Fakülte Kurulu'ndan Gelen ${FACULTY_STUDENTS.length}`,
              children: renderFacultyTable(),
            },
          ]}
        />

        <Space style={{ marginTop: 16 }}>
          <Button onClick={() => navigate('/dean/faculty-board')}>Fakülte Kurulu'na Git</Button>
          <Button onClick={() => navigate('/dean/ygk')}>YGK'ya Git</Button>
        </Space>
      </div>
    </div>
  )
}

export default DekanlikOfisi
