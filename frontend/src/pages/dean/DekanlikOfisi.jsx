import { useState, useEffect } from 'react'
import { Tabs, Table, Button, Typography, Tag, Row, Col, Input, message, Spin } from 'antd'
import { DownloadOutlined, DownOutlined, UpOutlined } from '@ant-design/icons'
import apiClient from '../../api/client'

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

const STATUS_LABEL = {
  DEAN_OFFICE_REVIEW: 'Akademik İnceleme Bekliyor',
  YGK_REVIEW_DONE: 'YGK Değerlendirmesi Tamamlandı',
  FACULTY_BOARD_ACCEPTED: 'Fakülte Kurulu: Onaylandı',
  FACULTY_BOARD_REJECTED: 'Fakülte Kurulu: Reddedildi',
}

const BATCH_ACTION = {
  oidb: 'TO_YGK',
  ygk: 'TO_FACULTY_BOARD',
  faculty: 'TO_OIDB',
}

const mapApp = (dto) => ({
  id: dto.id,
  name: dto.studentName,
  email: dto.email,
  tel: dto.phoneNumber,
  bolum: dto.targetDepartment,
  fakulte: dto.targetFaculty,
  gpa: dto.gpa,
  yksScore: dto.sayYksScore,
  basvuruTarihi: dto.submissionDate,
  durum: STATUS_LABEL[dto.status] ?? dto.status,
  rawStatus: dto.status,
})

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
  const [selectedRowKeys, setSelectedRowKeys] = useState([])
  const [forwarding, setForwarding] = useState(false)

  const [oidbList, setOidbList] = useState([])
  const [ygkList, setYgkList] = useState([])
  const [facultyList, setFacultyList] = useState([])
  const [loading, setLoading] = useState(false)

  const fetchLists = async () => {
    setLoading(true)
    try {
      const [oidbRes, ygkRes, facRes] = await Promise.all([
        apiClient.get('/dean/applications', { params: { queue: 'FROM_OIDB' } }),
        apiClient.get('/dean/applications', { params: { queue: 'FROM_YGK' } }),
        apiClient.get('/dean/applications', { params: { queue: 'FROM_FACULTY' } }),
      ])
      setOidbList((oidbRes.data ?? []).map(mapApp))
      setYgkList((ygkRes.data ?? []).map(mapApp))
      setFacultyList((facRes.data ?? []).map(mapApp))
    } catch {
      message.error('Başvurular yüklenemedi.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { fetchLists() }, [])

  const toggleExpand = (id) => {
    setExpandedRows(prev => ({ ...prev, [id]: !prev[id] }))
  }

  const handleTabChange = (key) => {
    setActiveTab(key)
    setSelectedRowKeys([])
  }

  const rowSelection = {
    selectedRowKeys,
    onChange: (keys) => setSelectedRowKeys(keys),
  }

  const getActionButtonText = () => {
    const count = selectedRowKeys.length
    if (activeTab === 'oidb') return `YGK'ya İlet (${count})`
    if (activeTab === 'ygk') return `Fakülte Kurulu'na İlet (${count})`
    if (activeTab === 'faculty') return `ÖİDB'ye Gönder (${count})`
    return `İşlem Yap (${count})`
  }

  const handleForward = async () => {
    if (selectedRowKeys.length === 0) return
    setForwarding(true)
    try {
      await apiClient.post('/dean/applications/batch-forward', {
        ids: selectedRowKeys,
        action: BATCH_ACTION[activeTab],
      })
      message.success(`${selectedRowKeys.length} başvuru iletildi.`)
      setSelectedRowKeys([])
      fetchLists()
    } catch (err) {
      message.error(err?.response?.data?.message ?? 'İletim başarısız.')
    } finally {
      setForwarding(false)
    }
  }

  const expandedRowKeys = Object.keys(expandedRows).filter(k => expandedRows[k]).map(Number)

  const filterBySearch = (list) => {
    const q = searchText.trim().toLowerCase()
    if (!q) return list
    return list.filter(s =>
      (s.name ?? '').toLowerCase().includes(q) ||
      (s.email ?? '').toLowerCase().includes(q) ||
      (s.bolum ?? '').toLowerCase().includes(q)
    )
  }

  const filteredOIDB = filterBySearch(oidbList)
  const filteredYGK = filterBySearch(ygkList)
  const filteredFaculty = filterBySearch(facultyList)

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
    { title: 'Ad Soyad', dataIndex: 'name', key: 'name' },
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
        <Col span={12}><Text strong>GPA:</Text><Text style={{ marginLeft: 8 }}>{record.gpa}</Text></Col>
        <Col span={12}><Text strong>YKS Puanı:</Text><Text style={{ marginLeft: 8 }}>{record.yksScore}</Text></Col>
        <Col span={12}><Text strong>Başvuru Tarihi:</Text><Text style={{ marginLeft: 8 }}>{record.basvuruTarihi}</Text></Col>
      </Row>
    </div>
  )

  const renderYGKExpanded = (record) => (
    <div style={{ padding: '12px 24px', background: '#fafafa' }}>
      <Row gutter={[16, 12]}>
        <Col span={12}><Text strong>Fakülte:</Text><Text style={{ marginLeft: 8 }}>{record.fakulte}</Text></Col>
        <Col span={12}><Text strong>GPA:</Text><Text style={{ marginLeft: 8 }}>{record.gpa}</Text></Col>
        <Col span={12}><Text strong>YKS Puanı:</Text><Text style={{ marginLeft: 8 }}>{record.yksScore}</Text></Col>
        <Col span={12}><Text strong>Başvuru Tarihi:</Text><Text style={{ marginLeft: 8 }}>{record.basvuruTarihi}</Text></Col>
      </Row>
    </div>
  )

  const renderFacultyExpanded = (record) => {
    const accepted = record.rawStatus === 'FACULTY_BOARD_ACCEPTED'
    return (
      <div style={{ padding: '12px 24px', background: '#fafafa' }}>
        <Row gutter={[16, 12]}>
          <Col span={12}><Text strong>Fakülte:</Text><Text style={{ marginLeft: 8 }}>{record.fakulte}</Text></Col>
          <Col span={12}>
            <Text strong>Kurul Kararı:</Text>
            <Tag style={{ marginLeft: 8, ...(accepted ? TINT.green : TINT.red), border: 'none', borderRadius: 6 }}>
              {accepted ? 'Onaylandı' : 'Reddedildi'}
            </Tag>
          </Col>
          <Col span={12}><Text strong>GPA:</Text><Text style={{ marginLeft: 8 }}>{record.gpa}</Text></Col>
          <Col span={12}><Text strong>YKS Puanı:</Text><Text style={{ marginLeft: 8 }}>{record.yksScore}</Text></Col>
        </Row>
      </div>
    )
  }

  const currentCount = activeTab === 'oidb' ? filteredOIDB.length : activeTab === 'ygk' ? filteredYGK.length : filteredFaculty.length

  return (
    <div style={styles.page}>
      <Title level={2} style={{ marginBottom: 24 }}>Dekanlık Ofisi</Title>

      <div style={styles.card}>
        <Row gutter={16} style={{ marginBottom: 16 }}>
          <Col span={8}>
            <Input
              placeholder="Ad, Soyad veya Bölüm ile ara..."
              value={searchText}
              onChange={(e) => setSearchText(e.target.value)}
            />
          </Col>
          <Col span={8} />
          <Col span={8} style={{ textAlign: 'right' }}>
            <Button
              type="primary"
              style={{
                background: selectedRowKeys.length > 0 ? '#8B1A2B' : undefined,
                borderColor: selectedRowKeys.length > 0 ? '#8B1A2B' : undefined,
              }}
              disabled={selectedRowKeys.length === 0}
              loading={forwarding}
              onClick={handleForward}
            >
              {getActionButtonText()}
            </Button>
          </Col>
        </Row>

        <Spin spinning={loading}>
          <Tabs
            activeKey={activeTab}
            onChange={handleTabChange}
            items={[
              {
                key: 'oidb',
                label: `ÖİDB'den Gelen (${filteredOIDB.length})`,
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
                label: `YGK'dan Gelen (${filteredYGK.length})`,
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
                label: `Fakülte Kurulu'ndan Gelen (${filteredFaculty.length})`,
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
        </Spin>

        <div style={{ marginTop: 16 }}>
          <Text type="secondary">
            {selectedRowKeys.length > 0
              ? `${selectedRowKeys.length} başvuru seçildi`
              : `${currentCount} başvuru gösteriliyor`}
          </Text>
        </div>
      </div>
    </div>
  )
}

export default DekanlikOfisi