import { useState, useEffect } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import {
  Button,
  Typography,
  Spin,
  Input,
  Select,
  Space,
  Row,
  Col,
  App,
} from 'antd'
import {
  FileTextOutlined,
  DownloadOutlined,
  SafetyCertificateOutlined,
  FormOutlined,
} from '@ant-design/icons'
import { useAuth } from '../../contexts/AuthContext'
import {
  ydyoApi,
  deriveDocumentApproval,
  deriveExamStatus,
  deriveExemptionStatus,
} from '../../api/ydyo'

const { Title, Text, Link } = Typography
const { TextArea } = Input

// ─── Badge config (soft tint) — mirrors the board ─────────────────────────────
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

// "Belge Onay Durumu" dropdown — sadece 2 seçenek (TC-7.0/7.10).
const DOC_OPTIONS = [
  { value: 'APPROVED',     label: 'Onaylandı'   },
  { value: 'NOT_APPROVED', label: 'Onaylanmadı' },
]

// "Onaylanmadı" seçilince aktifleşen "Sınav Sonucu" dropdown'u (EXAM_BADGE key'leri).
const EXAM_OPTIONS = [
  { value: 'PENDING', label: 'Sınav Sonucu Bekleniyor' },
  { value: 'PASSED',  label: 'Sınav Başarılı'          },
  { value: 'FAILED',  label: 'Sınav Başarısız'         },
]

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

// Badge with a caption above it so each status is self-explanatory.
function LabeledBadge({ caption, label, tint }) {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
      <Text type="secondary" style={{ fontSize: 11 }}>{caption}</Text>
      <div><Badge label={label} tint={tint} /></div>
    </div>
  )
}

// Başlık şeritli, kenarlıklı bölüm kartı — her parça net ayrılsın.
function Section({ icon, title, children }) {
  return (
    <div style={styles.section}>
      <div style={styles.sectionHead}>{icon}{title}</div>
      <div style={styles.sectionBody}>{children}</div>
    </div>
  )
}

// Üstte gri büyük-harf etiket, altta değer (referans tasarımdaki alan düzeni).
function Field({ label, children }) {
  return (
    <div>
      <div
        style={{
          fontSize: 11,
          letterSpacing: 0.4,
          textTransform: 'uppercase',
          color: '#9095A0',
          fontWeight: 600,
          marginBottom: 4,
        }}
      >
        {label}
      </div>
      <div style={{ fontSize: 14, color: '#1f212b' }}>{children}</div>
    </div>
  )
}

const formatDate = (iso) => {
  if (!iso) return '—'
  return new Intl.DateTimeFormat('tr-TR', { day: 'numeric', month: 'long', year: 'numeric' }).format(new Date(iso))
}

const styles = {
  page: {
    fontFamily: "'DM Sans', sans-serif",
  },
  pageHeader: {
    marginBottom: 32,
  },
  panel: {
    background: '#fafafa',
    border: '1px solid #f0f0f0',
    borderRadius: 8,
    padding: 16,
  },
  documentRow: {
    display: 'flex',
    alignItems: 'center',
    gap: 10,
    padding: '10px 12px',
    background: '#ffffff',
    border: '1px solid #f0f0f0',
    borderRadius: 8,
    marginBottom: 8,
  },
  badgeRow: {
    display: 'flex',
    gap: 28,
    flexWrap: 'wrap',
    margin: '16px 0',
  },
  fieldGrid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))',
    gap: '16px 24px',
  },
  // Kenardan kenara bant — modal body padding'i 0; girintiyi sadece bu bantlar verir.
  section: {
    borderBottom: '1px solid #f0f0f0',
  },
  // Tablo header'ıyla aynı dil: dolu maroon + beyaz yazı.
  sectionHead: {
    display: 'flex',
    alignItems: 'center',
    gap: 8,
    background: '#8B1A2B',
    color: '#ffffff',
    padding: '12px 24px',
    fontWeight: 700,
    fontSize: 13.5,
  },
  sectionBody: {
    padding: '18px 24px 22px',
  },
  statusRow: {
    display: 'flex',
    gap: 28,
    flexWrap: 'wrap',
    marginTop: 16,
    paddingTop: 16,
    borderTop: '1px dashed #eee',
  },
  readonlyField: {
    display: 'flex',
    flexDirection: 'column',
    gap: 6,
  },
  loadingWrap: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    minHeight: 200,
    gap: 12,
  },
}

// ─── Detail body (shared by embedded & standalone modes) ──────────────────────
// `locked` true ise (ÖİDB'ye iletildi) salt-okunur; aksi halde form düzenlenebilir
// kalır — karar değiştirilebilir, açıklama not gibi güncellenebilir (İlet'e kadar).
function DetailBody({ application, onChange, locked = false }) {
  const { user } = useAuth()
  const { message } = App.useApp()
  // Form mevcut kayıttan ön-doldurulur → tekrar açılınca düzenlenebilir kalır.
  const [docStatus, setDocStatus] = useState(() => {
    const d = deriveDocumentApproval(application)
    return d === 'APPROVED' || d === 'NOT_APPROVED' ? d : undefined
  })
  const [examStatus, setExamStatus] = useState(() => {
    if (deriveDocumentApproval(application) !== 'NOT_APPROVED') return undefined
    const e = deriveExamStatus(application)
    return e === 'PASSED' || e === 'FAILED' ? e : 'PENDING'
  })
  const [notes, setNotes] = useState(application.notes ?? '')
  const [submitting, setSubmitting] = useState(false)

  // Reviewer = logged-in YDYO staff. getMe → {id}, login → {userId}.
  const reviewerId = user?.userId ?? user?.id ?? null

  const {
    applicationId,
    studentName,
    tcKimlikNo,
    email,
    phone,
    currentUniversity,
    currentDepartment,
    targetDepartment,
    academicYear,
    submittedAt,
    documents = [],
    englishCertificate,
    notes: savedNotes,
  } = application

  // "Belge Onay Durumu" seçimine göre kararı seç. "Onaylanmadı"da sınav dropdown'u aktif.
  const examActive = docStatus === 'NOT_APPROVED'

  // "Sınav Sonucu" alanının gösterdiği değer (Onaylandı → kilitli "Sınav Gerekli Değil").
  const examSelectValue = docStatus === 'APPROVED' ? 'NOT_REQUIRED' : examStatus

  // "Muafiyet Sonucu" her zaman sistemce türetilir (read-only):
  //   Onaylandı → Muaf · Onaylanmadı+Başarılı → Muaf · +Başarısız → Muaf Değil · +Bekleniyor/boş → Beklemede
  let exemptionKey = 'PENDING'
  if (docStatus === 'APPROVED') exemptionKey = 'EXEMPT'
  else if (examActive) {
    exemptionKey = examStatus === 'PASSED' ? 'EXEMPT' : examStatus === 'FAILED' ? 'NOT_EXEMPT' : 'PENDING'
  }

  // Belge Onay Durumu değişince sınav seçimini sıfırla ("Onaylanmadı"da "Bekleniyor" başlasın).
  const handleDocStatusChange = (val) => {
    setDocStatus(val)
    setExamStatus(val === 'NOT_APPROVED' ? 'PENDING' : undefined)
  }

  const descriptionItems = [
    { key: 'name',    label: 'Ad Soyad',          children: studentName },
    { key: 'tc',      label: 'TC Kimlik No',      children: tcKimlikNo },
    { key: 'email',   label: 'E-posta',           children: email },
    { key: 'phone',   label: 'Telefon',           children: phone },
    { key: 'curUni',  label: 'Mevcut Üniversite', children: currentUniversity },
    { key: 'curDept', label: 'Mevcut Bölüm',      children: currentDepartment },
    { key: 'tgtDept', label: 'Hedef Bölüm',       children: targetDepartment },
    { key: 'date',    label: 'Başvuru Tarihi',    children: formatDate(submittedAt) },
    { key: 'year',    label: 'Akademik Yıl',      children: academicYear },
  ]

  // ── Belge değerlendirmesini kaydet (mock; gerçek API TODO) ──
  const handleSave = async () => {
    if (!docStatus) {
      message.warning('Lütfen belge onay durumunu seçiniz.')
      return
    }
    if (!notes.trim()) {
      message.warning('Lütfen açıklama giriniz.')   // UC-7 8-EX
      return
    }
    const trimmedNotes = notes.trim()
    setSubmitting(true)
    try {
      // ⚠️ Only reviewerId is sent — never the full reviewer entity.
      if (docStatus === 'APPROVED') {
        // Belge yeterli → muaf, sınav gerekmez.
        // TODO: real → PATCH /applications/{id}/ydyo-initial-review
        await ydyoApi.submitInitialReview(applicationId, {
          approved: true, requiresExam: false, notes: trimmedNotes, reviewerId,
        })
      } else if (examStatus === 'PASSED' || examStatus === 'FAILED') {
        // Belge yetersiz ama sınav sonucu girildi → doğrudan sınav sonucu kaydı.
        // TODO: real → PATCH /applications/{id}/ydyo-exam-result
        await ydyoApi.submitExamResult(applicationId, {
          passed: examStatus === 'PASSED', examScore: null, notes: trimmedNotes, reviewerId,
        })
      } else {
        // Belge yetersiz, henüz sonuç yok → sınava yönlendir (Bekleniyor).
        // TODO: real → PATCH /applications/{id}/ydyo-initial-review
        await ydyoApi.submitInitialReview(applicationId, {
          approved: false, requiresExam: true, notes: trimmedNotes, reviewerId,
        })
      }
      message.success('Değerlendirme kaydedildi.')
      setNotes('')
      setDocStatus(undefined)
      setExamStatus(undefined)
      onChange?.()
    } catch {
      message.error('Değerlendirme kaydedilemedi.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div>
      {/* Öğrenci bilgileri + durum özeti — başlık çubuğu (ad) bu bölümün başlığı. */}
      <div style={{ ...styles.sectionBody, borderBottom: '1px solid #f0f0f0' }}>
        <div style={styles.fieldGrid}>
          {descriptionItems.map((it) => (
            <Field key={it.key} label={it.label}>{it.children}</Field>
          ))}
        </div>
        <div style={styles.statusRow}>
          <LabeledBadge caption="Belge Onayı" {...DOC_APPROVAL_BADGE[deriveDocumentApproval(application)]} />
          <LabeledBadge caption="Sınav Sonucu" {...EXAM_BADGE[deriveExamStatus(application)]} />
          <LabeledBadge caption="Muafiyet Sonucu" {...EXEMPTION_BADGE[deriveExemptionStatus(application)]} />
        </div>
      </div>

      {/* Belgeler */}
      <Section icon={<FileTextOutlined />} title="Belgeler">
      <Row gutter={[16, 16]}>
        <Col xs={24} md={12}>
          <Title level={5} style={{ marginTop: 0 }}>Başvuru Belgeleri</Title>
          <div style={styles.panel}>
            {documents.length === 0 ? (
              <Text type="secondary">Belge bulunmuyor.</Text>
            ) : (
              documents.map((doc) => (
                <div key={doc.documentId} style={styles.documentRow}>
                  <FileTextOutlined style={{ color: '#8B1A2B', fontSize: 16 }} />
                  <Link href={ydyoApi.getDocumentDownloadUrl(doc.documentId)} target="_blank" style={{ flex: 1 }}>
                    {doc.fileName}
                  </Link>
                  <DownloadOutlined style={{ color: '#6B7280' }} />
                </div>
              ))
            )}
          </div>
        </Col>

        <Col xs={24} md={12}>
          <Title level={5} style={{ marginTop: 0 }}>İngilizce Yeterlilik Belgesi</Title>
          <div style={styles.panel}>
            {englishCertificate ? (
              <>
                <div style={styles.documentRow}>
                  <SafetyCertificateOutlined style={{ color: '#8B1A2B', fontSize: 16 }} />
                  <Link href={ydyoApi.getDocumentDownloadUrl(englishCertificate.documentId)} target="_blank" style={{ flex: 1 }}>
                    {englishCertificate.fileName}
                  </Link>
                  <DownloadOutlined style={{ color: '#6B7280' }} />
                </div>
                {/* Sınav türü / puan her zaman makinece okunamayabilir → opsiyonel. */}
                {(englishCertificate.examType || englishCertificate.score != null) && (
                  <Space size={48} style={{ padding: '4px 4px 0' }}>
                    <div>
                      <Text type="secondary" style={{ fontSize: 12, display: 'block' }}>Sınav Türü</Text>
                      <Text>{englishCertificate.examType ?? '—'}</Text>
                    </div>
                    <div>
                      <Text type="secondary" style={{ fontSize: 12, display: 'block' }}>Puan</Text>
                      <Text>{englishCertificate.score ?? '—'}</Text>
                    </div>
                  </Space>
                )}
              </>
            ) : (
              <Text type="secondary">Belge yüklenmemiş.</Text>
            )}
          </div>
        </Col>
      </Row>
      </Section>

      {/* Belge değerlendirme — İlet'e kadar düzenlenebilir; iletilince salt-okunur */}
      <Section icon={<FormOutlined />} title="Belge Değerlendirme">
        {!locked ? (
          <Row gutter={[16, 16]}>
            <Col xs={24} md={8}>
              <div style={styles.readonlyField}>
                <Text style={{ fontSize: 13, fontWeight: 500 }}>
                  Belge Onay Durumu <span style={{ color: '#B91C1C' }}>*</span>
                </Text>
                <Select
                  value={docStatus}
                  onChange={handleDocStatusChange}
                  placeholder="Seçiniz"
                  style={{ width: '100%' }}
                  options={DOC_OPTIONS}
                />
              </div>
            </Col>
            <Col xs={12} md={8}>
              <div style={styles.readonlyField}>
                <Text style={{ fontSize: 13, fontWeight: 500 }}>Sınav Sonucu</Text>
                <Select
                  value={examSelectValue}
                  onChange={setExamStatus}
                  disabled={!examActive}
                  placeholder="—"
                  style={{ width: '100%' }}
                  options={
                    docStatus === 'APPROVED'
                      ? [{ value: 'NOT_REQUIRED', label: 'Sınav Gerekli Değil' }]
                      : EXAM_OPTIONS
                  }
                />
                <Text type="secondary" style={{ fontSize: 11 }}>
                  {examActive ? 'Sınav sonucunu seçiniz' : 'Otomatik belirlenir'}
                </Text>
              </div>
            </Col>
            <Col xs={12} md={8}>
              <div style={styles.readonlyField}>
                <Text style={{ fontSize: 13, fontWeight: 500 }}>Muafiyet Sonucu</Text>
                <div><Badge {...EXEMPTION_BADGE[exemptionKey]} /></div>
                <Text type="secondary" style={{ fontSize: 11 }}>Otomatik belirlenir (değiştirilemez)</Text>
              </div>
            </Col>

            <Col span={24}>
              <Text style={{ fontSize: 13, fontWeight: 500, display: 'block', marginBottom: 6 }}>
                Açıklama <span style={{ color: '#B91C1C' }}>*</span>
              </Text>
              <TextArea
                rows={3}
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
                placeholder='Değerlendirme açıklaması giriniz (zorunlu). Örn. "Cambridge C1 ile muaf".'
              />
            </Col>

            <Col span={24}>
              <Button
                type="primary"
                loading={submitting}
                style={{ background: '#8B1A2B', borderColor: '#8B1A2B' }}
                onClick={handleSave}
              >
                Kaydet
              </Button>
            </Col>
          </Row>
        ) : (
          // ÖİDB'ye iletildi → salt-okunur özet (artık düzenlenemez).
          <Space direction="vertical" size={8}>
            <Text type="secondary" style={{ fontSize: 13 }}>
              Liste ÖİDB'ye iletildi; bu kayıt artık düzenlenemez.
            </Text>
            <Space size={24} wrap>
              <Space>
                <Text style={{ fontSize: 13 }}>Belge Onayı:</Text>
                <Badge {...DOC_APPROVAL_BADGE[deriveDocumentApproval(application)]} />
              </Space>
              <Space>
                <Text style={{ fontSize: 13 }}>Sınav:</Text>
                <Badge {...EXAM_BADGE[deriveExamStatus(application)]} />
              </Space>
              <Space>
                <Text style={{ fontSize: 13 }}>Muafiyet:</Text>
                <Badge {...EXEMPTION_BADGE[deriveExemptionStatus(application)]} />
              </Space>
            </Space>
            {application.examScore != null && (
              <Text type="secondary" style={{ fontSize: 13 }}>
                Sınav Notu: <strong>{application.examScore}</strong>
              </Text>
            )}
            {savedNotes && (
              <Text type="secondary" style={{ fontSize: 13 }}>
                <strong>Açıklama:</strong> {savedNotes}
              </Text>
            )}
          </Space>
        )}
      </Section>
    </div>
  )
}

/**
 * YdyoApplicationDetail
 *   • embedded   → receives `application` + `onChange`, renders inline (accordion body).
 *   • standalone → reads :id from the route, fetches, renders with page header/footer.
 */
export default function YdyoApplicationDetail({ application: appProp, onChange, embedded = false, locked = false }) {
  if (embedded && appProp) {
    return <DetailBody application={appProp} onChange={onChange} locked={locked} />
  }
  return <StandaloneDetail />
}

function StandaloneDetail() {
  const [loading, setLoading] = useState(true)
  const [application, setApplication] = useState(null)
  const navigate = useNavigate()
  const { id } = useParams()
  const { message } = App.useApp()

  const load = () =>
    ydyoApi.getApplicationById(id)
      .then((data) => setApplication(data ?? null))
      .catch(() => message.error('Başvuru detayı yüklenirken bir hata oluştu.'))
      .finally(() => setLoading(false))

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id])

  if (loading) {
    return (
      <div style={styles.loadingWrap}>
        <Spin size="large" />
        <Text type="secondary">Yükleniyor...</Text>
      </div>
    )
  }

  if (!application) {
    return (
      <div style={styles.page}>
        <Text type="secondary">Başvuru bulunamadı.</Text>
        <div style={{ marginTop: 16 }}>
          <Button onClick={() => navigate('/ydyo/dashboard')}>Panele Dön</Button>
        </div>
      </div>
    )
  }

  return (
    <div style={styles.page}>
      <div style={styles.pageHeader}>
        <Title level={2} style={{ margin: 0, color: '#1f212b', fontWeight: 700 }}>
          Başvuru Detayı
        </Title>
        <Text type="secondary" style={{ fontSize: 14 }}>
          {application.studentName} — yatay geçiş başvuru değerlendirmesi
        </Text>
      </div>

      <div
        style={{
          background: '#ffffff',
          borderRadius: 10,
          overflow: 'hidden',
          boxShadow: '0 4px 16px rgba(0,0,0,0.06)',
          border: '1px solid #f0f0f0',
        }}
      >
        <DetailBody application={application} onChange={load} />
      </div>

      <div style={{ marginTop: 24 }}>
        <Button onClick={() => navigate('/ydyo/dashboard')}>Panele Dön</Button>
      </div>
    </div>
  )
}
