import { useState, useEffect, useMemo } from 'react'
import { Button, Typography, Select, Spin, Empty, Table, Modal, App, ConfigProvider, Tooltip, Alert } from 'antd'
import {
  DownloadOutlined,
  SendOutlined,
  MailOutlined,
  PhoneOutlined,
  UserOutlined,
  CloseOutlined,
  QuestionCircleOutlined,
} from '@ant-design/icons'
import {
  ydyoApi,
  PENDING_YDYO_STATUSES,
  deriveDocumentApproval,
  deriveExamStatus,
  deriveExemptionStatus,
} from '../../api/ydyo'
import YdyoApplicationDetail from './YdyoApplicationDetail'

const { Title, Text } = Typography

// ─── Badge config (soft tint) ─────────────────────────────────────────────────
const TINT = {
  amber: { background: '#FEF3C7', color: '#B45309' },
  green: { background: '#D1FAE5', color: '#047857' },
  red:   { background: '#FEE2E2', color: '#B91C1C' },
  gray:  { background: '#F3F4F6', color: '#6B7280' },
}

const DOC_APPROVAL_BADGE = {
  PENDING:      { label: 'Beklemede',   tint: 'amber' },
  APPROVED:     { label: 'Onaylandı',   tint: 'green' },
  NOT_APPROVED: { label: 'Onaylanmadı', tint: 'red'   },
}

const EXAM_BADGE = {
  NOT_DETERMINED: { label: 'Henüz Belirlenmedi',      tint: 'gray'  },
  NOT_REQUIRED:   { label: 'Sınav Gerekli Değil',     tint: 'gray'  },
  PENDING:        { label: 'Sınav Sonucu Bekleniyor', tint: 'amber' },
  PASSED:         { label: 'Sınav Başarılı',          tint: 'green' },
  FAILED:         { label: 'Sınav Başarısız',         tint: 'red'   },
}

const EXEMPTION_BADGE = {
  PENDING:    { label: 'Beklemede',  tint: 'amber' },
  EXEMPT:     { label: 'Muaf',       tint: 'green' },
  NOT_EXEMPT: { label: 'Muaf Değil', tint: 'red'   },
}

function Badge({ label, tint }) {
  return (
    <span
      style={{
        ...TINT[tint],
        display: 'inline-flex',
        alignItems: 'center',
        gap: 6,
        fontSize: 12,
        fontWeight: 600,
        padding: '3px 12px',
        borderRadius: 999,
        whiteSpace: 'nowrap',
      }}
    >
      <span style={{ width: 7, height: 7, borderRadius: '50%', background: TINT[tint].color }} />
      {label}
    </span>
  )
}

// İsimden baş harfler — avatar için.
const initials = (name) =>
  (name ?? '?')
    .split(' ')
    .filter(Boolean)
    .map((w) => w[0])
    .slice(0, 2)
    .join('')
    .toUpperCase()

const styles = {
  page: {
    fontFamily: "'DM Sans', sans-serif",
  },
  pageHeader: {
    marginBottom: 24,
  },
  card: {
    background: '#ffffff',
    borderRadius: 10,
    padding: 24,
    boxShadow: '0 4px 16px rgba(0,0,0,0.06)',
    border: '1px solid #f0f0f0',
    marginBottom: 24,
  },
  // Modal maroon başlık çubuğu — content overflow:hidden köşeleri kırpar.
  modalHeader: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    background: '#8B1A2B',
    padding: '16px 24px',
  },
  // Tablo kartı: padding yok + köşeler kırpılı, böylece maroon header kenara hizalı.
  tableCard: {
    background: '#ffffff',
    borderRadius: 10,
    overflow: 'hidden',
    boxShadow: '0 4px 16px rgba(0,0,0,0.06)',
    border: '1px solid #f0f0f0',
    marginBottom: 24,
  },
  filterBar: {
    display: 'flex',
    flexWrap: 'wrap',
    alignItems: 'center',
    gap: 16,
  },
  filterField: {
    display: 'flex',
    flexDirection: 'column',
    gap: 4,
  },
  actionRow: {
    display: 'flex',
    flexWrap: 'wrap',
    justifyContent: 'flex-end',
    gap: 12,
    marginBottom: 16,
  },
  rowHeader: {
    display: 'flex',
    alignItems: 'center',
    gap: 16,
    flexWrap: 'wrap',
  },
  rowMain: {
    minWidth: 220,
    flex: '1 1 220px',
  },
  avatar: {
    width: 38,
    height: 38,
    borderRadius: '50%',
    background: '#FBEDEF',
    color: '#8B1A2B',
    fontWeight: 700,
    fontSize: 14,
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    flexShrink: 0,
  },
  rowBadges: {
    display: 'flex',
    gap: 8,
    flexWrap: 'wrap',
    alignItems: 'center',
  },
  contact: {
    display: 'flex',
    gap: 14,
    flexWrap: 'wrap',
    marginTop: 2,
  },
  loadingWrap: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    minHeight: 300,
    gap: 12,
  },
}

const ALL = '__ALL__'

export default function YdyoDashboard() {
  const [loading, setLoading] = useState(true)
  const [applications, setApplications] = useState([])
  const [yearFilter, setYearFilter] = useState(ALL)
  const [examFilter, setExamFilter] = useState(ALL)
  const [exemptionFilter, setExemptionFilter] = useState(ALL)
  const [selected, setSelected] = useState(null)   // application shown in the modal
  const [transmitted, setTransmitted] = useState(false)   // ÖİDB'ye iletildi → liste kilitli (YDYO Completed)
  const { message, modal } = App.useApp()

  // No status param → mock returns every YDYO stage for the board.
  const loadApplications = () =>
    ydyoApi.getApplications()
      .then((page) => setApplications(page.content ?? []))
      .catch(() => message.error('Başvurular yüklenirken bir hata oluştu.'))
      .finally(() => setLoading(false))

  useEffect(() => {
    loadApplications()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const academicYears = useMemo(
    () => [...new Set(applications.map((a) => a.academicYear).filter(Boolean))],
    [applications]
  )

  const filtered = useMemo(() => {
    return applications.filter((app) => {
      if (yearFilter !== ALL && app.academicYear !== yearFilter) return false
      if (examFilter !== ALL && deriveExamStatus(app) !== examFilter) return false
      if (exemptionFilter !== ALL && deriveExemptionStatus(app) !== exemptionFilter) return false
      return true
    })
  }, [applications, yearFilter, examFilter, exemptionFilter])

  // ── "Tablo Oluştur" → client-side CSV export of the visible list ──
  const handleExportCsv = () => {
    if (filtered.length === 0) {
      message.info('Dışa aktarılacak kayıt bulunmuyor.')
      return
    }
    const headers = ['Ad Soyad', 'E-posta', 'Telefon', 'Belge Onayı', 'Sınav Sonucu', 'Sınav Puanı', 'Muafiyet Sonucu', 'Açıklama']
    const rows = filtered.map((app) => [
      app.studentName,
      app.email,
      app.phone,
      DOC_APPROVAL_BADGE[deriveDocumentApproval(app)].label,
      EXAM_BADGE[deriveExamStatus(app)].label,
      app.examScore ?? '',
      EXEMPTION_BADGE[deriveExemptionStatus(app)].label,
      (app.notes ?? '').replace(/\s+/g, ' ').trim(),
    ])
    const escape = (val) => `"${String(val ?? '').replace(/"/g, '""')}"`
    const csv = [headers, ...rows].map((r) => r.map(escape).join(',')).join('\r\n')
    // BOM so Excel reads UTF-8 (Turkish chars) correctly.
    const blob = new Blob(['﻿' + csv], { type: 'text/csv;charset=utf-8;' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = 'ydyo-basvuru-listesi.csv'
    link.click()
    URL.revokeObjectURL(url)
    message.success('Liste CSV olarak indirildi.')
  }

  // ── "Sonuçları ÖİDB'ye İlet" → client-side guard only (no backend endpoint) ──
  const handleForwardToOidb = () => {
    const unfinished = applications.filter((a) => PENDING_YDYO_STATUSES.includes(a.status))
    if (unfinished.length > 0) {
      message.warning('Tüm öğrenci kayıtları tamamlanmadan liste iletilemez.')
      return
    }
    modal.confirm({
      title: 'Sonuçları ÖİDB\'ye İlet',
      content: 'Tüm değerlendirmeler tamamlandı. Sonuçlar ÖİDB listesine iletilsin mi?',
      okText: 'İlet',
      cancelText: 'Vazgeç',
      okButtonProps: { style: { background: '#8B1A2B', borderColor: '#8B1A2B' } },
      // Mock: iletim sonrası kayıtlar "YDYO Completed" sayılır ve liste kilitlenir.
      // TODO: real → backend transmit endpoint + YDYO_COMPLETED status geçişi (Arda).
      onOk: () => {
        setTransmitted(true)
        message.success('Liste başarıyla ÖİDB\'ye iletildi.')
      },
    })
  }

  if (loading) {
    return (
      <div style={styles.loadingWrap}>
        <Spin size="large" />
        <Text type="secondary">Yükleniyor...</Text>
      </div>
    )
  }

  // Beyaz başlık üzerinde "?" ipucu — durum sütununun ne işe yaradığını anlatır.
  const HeaderWithHelp = (title, help) => (
    <span style={{ display: 'inline-flex', alignItems: 'center', gap: 6 }}>
      {title}
      <Tooltip title={help} color="#1f212b">
        <QuestionCircleOutlined style={{ color: 'rgba(255,255,255,0.75)', fontSize: 13 }} />
      </Tooltip>
    </span>
  )

  // Her rozet kendi sütununda; başlık + "?" ipucu ne olduğunu açıklar.
  const columns = [
    {
      title: 'Öğrenci',
      key: 'student',
      render: (_, app) => (
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <div style={styles.avatar}>{initials(app.studentName)}</div>
          <Text style={{ fontWeight: 600, fontSize: 15 }}>{app.studentName}</Text>
        </div>
      ),
    },
    {
      title: 'E-posta',
      key: 'email',
      render: (_, app) => (
        <Text type="secondary" style={{ fontSize: 13 }}>
          <MailOutlined style={{ marginRight: 6 }} />{app.email}
        </Text>
      ),
    },
    {
      title: 'Telefon',
      key: 'phone',
      width: 160,
      render: (_, app) => (
        <Text type="secondary" style={{ fontSize: 13 }}>
          <PhoneOutlined style={{ marginRight: 6 }} />{app.phone}
        </Text>
      ),
    },
    {
      title: HeaderWithHelp(
        'Belge Onayı',
        'Öğrencinin yüklediği belgelerin YDYO değerlendirmesi. Onaylandı → muafiyet için yeterli; Onaylanmadı → yeterlilik sınavına yönlendirilir; Beklemede → henüz değerlendirilmedi.'
      ),
      key: 'docApproval',
      width: 160,
      align: 'center',
      render: (_, app) => <Badge {...DOC_APPROVAL_BADGE[deriveDocumentApproval(app)]} />,
    },
    {
      title: HeaderWithHelp(
        'Sınav Sonucu',
        'İngilizce yeterlilik sınavının durumu. Belge onaylandıysa sınav gerekmez; aksi halde sonuç beklenir, ardından Başarılı/Başarısız olur.'
      ),
      key: 'exam',
      width: 210,
      align: 'center',
      render: (_, app) => <Badge {...EXAM_BADGE[deriveExamStatus(app)]} />,
    },
    {
      title: HeaderWithHelp(
        'Sınav Puanı',
        'Yeterlilik sınavına giren adayın aldığı puan. Sınav gerekmeyen (belge onaylı) ya da henüz sonucu girilmemiş kayıtlarda boştur.'
      ),
      dataIndex: 'examScore',
      key: 'examScore',
      width: 120,
      align: 'center',
      render: (score) =>
        score != null
          ? <Text style={{ fontWeight: 600 }}>{score}</Text>
          : <Text type="secondary">—</Text>,
    },
    {
      title: HeaderWithHelp(
        'Muafiyet Sonucu',
        'Öğrencinin İngilizce hazırlıktan muaf olup olmadığı. Belge onayı veya sınav sonucuna göre otomatik belirlenir.'
      ),
      key: 'exemption',
      width: 160,
      align: 'center',
      render: (_, app) => <Badge {...EXEMPTION_BADGE[deriveExemptionStatus(app)]} />,
    },
    {
      title: 'Açıklama',
      dataIndex: 'notes',
      key: 'notes',
      width: 240,
      ellipsis: true,
      render: (notes) =>
        notes ? (
          <Tooltip title={notes}>
            <Text type="secondary" style={{ fontSize: 13 }}>{notes}</Text>
          </Tooltip>
        ) : (
          <Text type="secondary" style={{ fontSize: 13 }}>—</Text>
        ),
    },
  ]

  return (
    <div style={styles.page}>
      <div style={styles.pageHeader}>
        <Title level={2} style={{ margin: 0, color: '#1f212b', fontWeight: 700 }}>
          YDYO Başvuru Yönetimi
        </Title>
        <Text type="secondary" style={{ fontSize: 14 }}>
          Yabancı Diller Yüksekokulu — yatay geçiş başvuru değerlendirmesi
        </Text>
      </div>

      {/* Filtre çubuğu */}
      <div style={styles.card}>
        <div style={styles.filterBar}>
          <div style={styles.filterField}>
            <Text type="secondary" style={{ fontSize: 12 }}>Akademik Yıl</Text>
            <Select
              value={yearFilter}
              onChange={setYearFilter}
              style={{ width: 180 }}
              options={[
                { value: ALL, label: 'Tümü' },
                ...academicYears.map((y) => ({ value: y, label: y })),
              ]}
            />
          </div>

          <div style={styles.filterField}>
            <Text type="secondary" style={{ fontSize: 12 }}>Sınav Durumu</Text>
            <Select
              value={examFilter}
              onChange={setExamFilter}
              style={{ width: 220 }}
              options={[
                { value: ALL, label: 'Tümü' },
                ...Object.entries(EXAM_BADGE).map(([value, { label }]) => ({ value, label })),
              ]}
            />
          </div>

          <div style={styles.filterField}>
            <Text type="secondary" style={{ fontSize: 12 }}>Muafiyet Durumu</Text>
            <Select
              value={exemptionFilter}
              onChange={setExemptionFilter}
              style={{ width: 180 }}
              options={[
                { value: ALL, label: 'Tümü' },
                ...Object.entries(EXEMPTION_BADGE).map(([value, { label }]) => ({ value, label })),
              ]}
            />
          </div>

          <div style={{ marginLeft: 'auto' }}>
            <Text style={{ fontWeight: 600, fontSize: 14 }}>Başvuru: {filtered.length}</Text>
          </div>
        </div>
      </div>

      {/* İletim sonrası "YDYO Completed" bilgisi */}
      {transmitted && (
        <Alert
          type="success"
          showIcon
          style={{ marginBottom: 16 }}
          message="Liste ÖİDB'ye iletildi"
          description="Tüm kayıtlar YDYO Completed statüsündedir. Liste kilitlenmiştir."
        />
      )}

      {/* Aksiyon satırı */}
      <div style={styles.actionRow}>
        <Button icon={<DownloadOutlined />} onClick={handleExportCsv}>
          Tablo Oluştur
        </Button>
        <Button
          type="primary"
          icon={<SendOutlined />}
          disabled={transmitted}
          style={transmitted ? undefined : { background: '#8B1A2B', borderColor: '#8B1A2B' }}
          onClick={handleForwardToOidb}
        >
          {transmitted ? 'ÖİDB\'ye İletildi' : 'Sonuçları ÖİDB\'ye İlet'}
        </Button>
      </div>

      {/* Liste */}
      <div style={styles.tableCard}>
        {filtered.length === 0 ? (
          <div style={{ padding: 24 }}>
            <Empty
              image={Empty.PRESENTED_IMAGE_SIMPLE}
              description="Seçilen filtrelere uygun başvuru bulunmuyor."
            />
          </div>
        ) : (
          <ConfigProvider
            theme={{
              components: {
                Table: {
                  headerBg: '#8B1A2B',
                  headerColor: '#ffffff',
                  headerSplitColor: 'rgba(255,255,255,0.2)',
                  rowHoverBg: '#FBEDEF',
                  cellPaddingBlock: 14,
                  borderColor: '#f0f0f0',
                },
              },
            }}
          >
            <Table
              rowKey="applicationId"
              columns={columns}
              dataSource={filtered}
              pagination={false}
              // Satıra tıklayınca detay modal olarak açılır (iletilmişse salt-okunur).
              onRow={(app) => ({
                onClick: () => setSelected(app),
                style: { cursor: 'pointer' },
              })}
            />
          </ConfigProvider>
        )}
      </div>

      {/* Öğrenci detay — modal pop-up */}
      <Modal
        open={selected != null}
        onCancel={() => setSelected(null)}
        // antd header'ı yerine maroon başlığı içeride render ediyoruz; böylece
        // tablodaki gibi content'in overflow:hidden'ı köşeleri tam kapsar.
        title={null}
        closable={false}
        footer={null}
        width={920}
        destroyOnHidden
        styles={{
          content: { padding: 0, overflow: 'hidden', borderRadius: 12 },
          body: { padding: 0 },
        }}
      >
        {selected && (
          <div>
            <div style={styles.modalHeader}>
              <span style={{ display: 'flex', alignItems: 'center', gap: 10, color: '#fff', fontSize: 16, fontWeight: 600 }}>
                <UserOutlined /> {selected.studentName}
              </span>
              <CloseOutlined
                style={{ color: '#fff', cursor: 'pointer', fontSize: 16 }}
                onClick={() => setSelected(null)}
              />
            </div>
            <div style={{ maxHeight: '72vh', overflowY: 'auto' }}>
              <YdyoApplicationDetail
                application={selected}
                embedded
                locked={transmitted}
                onChange={() => {
                  // Değerlendirme kaydedilince listeyi tazele ve modalı kapat (listeye döner).
                  loadApplications()
                  setSelected(null)
                }}
              />
            </div>
          </div>
        )}
      </Modal>
    </div>
  )
}
