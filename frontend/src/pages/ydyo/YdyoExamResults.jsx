import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Button,
  Typography,
  Table,
  Upload,
  Spin,
  Empty,
  Alert,
  App,
} from 'antd'
import { UploadOutlined } from '@ant-design/icons'
import { useAuth } from '../../contexts/AuthContext'
import { ydyoApi, YDYO_STATUS } from '../../api/ydyo'

const { Title, Text } = Typography

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
  uploadBar: {
    display: 'flex',
    alignItems: 'center',
    gap: 16,
    flexWrap: 'wrap',
  },
  footer: {
    display: 'flex',
    gap: 12,
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

// CSV kolonları birebir (TC-7.4/7.5/7.6): studentId, score, result.
const REQUIRED_COLUMNS = ['studentid', 'score', 'result']
// result kolonu pass/fail'i belirler (score eşiği DEĞİL).
const RESULT_PASS = 'Sınav Başarılı'
const RESULT_FAIL = 'Sınav Başarısız'

const defaultNote = (score, passed) =>
  score == null
    ? `Yeterlilik sınavı sonucu: ${passed ? 'Başarılı' : 'Başarısız'}.`
    : `Yeterlilik sınavı notu: ${score} (${passed ? 'Başarılı' : 'Başarısız'}).`

// ─── CSV parser (studentId / score / result) ──────────────────────────────────
const parseCsv = (text) => {
  const lines = text.split(/\r?\n/).map((l) => l.trim()).filter(Boolean)
  if (lines.length === 0) return { headers: [], rows: [] }
  const delimiter = lines[0].includes(';') ? ';' : ','
  const norm = (s) => (s ?? '').replace(/^"|"$/g, '').trim()
  const headers = lines[0].split(delimiter).map((h) => norm(h).toLowerCase())

  const idIdx = headers.indexOf('studentid')
  const scoreIdx = headers.indexOf('score')
  const resultIdx = headers.indexOf('result')

  const rows = lines.slice(1).map((line) => {
    const cells = line.split(delimiter).map(norm)
    const rawScore = scoreIdx >= 0 ? cells[scoreIdx].replace(',', '.') : ''
    const score = rawScore !== '' ? Number(rawScore) : null
    return {
      studentId: idIdx >= 0 && cells[idIdx] !== '' ? Number(cells[idIdx]) : null,
      score: Number.isNaN(score) ? null : score,
      result: resultIdx >= 0 ? cells[resultIdx] : null,
    }
  })
  return { headers, rows }
}

export default function YdyoExamResults() {
  const [loading, setLoading] = useState(true)
  const [processing, setProcessing] = useState(false)
  const [applications, setApplications] = useState([])
  const [summary, setSummary] = useState(null)
  const navigate = useNavigate()
  const { user } = useAuth()
  const { message } = App.useApp()

  const reviewerId = user?.userId ?? user?.id ?? null

  const loadPending = () =>
    ydyoApi.getApplications(YDYO_STATUS.EXAM_PENDING)
      .then((page) => setApplications(page.content ?? []))
      .catch(() => message.error('Sınav bekleyen başvurular yüklenemedi.'))
      .finally(() => setLoading(false))

  useEffect(() => {
    loadPending()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  // ── CSV'yi doğrudan işle (öndoldur + elle Kaydet adımı YOK) — TC-7.4/7.5/7.6 ──
  const processCsv = async (text) => {
    const { headers, rows } = parseCsv(text)

    // Eksik zorunlu kolon → hiçbir kayıt güncellenmez.
    if (REQUIRED_COLUMNS.some((c) => !headers.includes(c))) {
      message.error('Geçersiz dosya yapısı. Lütfen zorunlu sütunların (studentId, score, result) mevcut olduğunu kontrol ediniz.')
      return
    }
    if (rows.length === 0) {
      message.warning('Dosyada işlenebilir satır bulunamadı.')
      return
    }

    setProcessing(true)
    setSummary(null)

    let passed = 0
    let failed = 0
    let errors = 0

    // studentId, yüklü listede Başvuru No (applicationId) ile eşleştirilir.
    for (const row of rows) {
      const app = applications.find((a) => a.applicationId === row.studentId)
      const isPass = row.result === RESULT_PASS
      const isFail = row.result === RESULT_FAIL
      // Tanınmayan studentId, geçersiz/boş result veya boş satır → hatalı, atlanır.
      if (!app || (!isPass && !isFail)) {
        errors++
        continue
      }
      try {
        // TODO: real → PATCH /applications/{id}/ydyo-exam-result
        await ydyoApi.submitExamResult(app.applicationId, {
          examScore: row.score,
          passed: isPass,
          notes: defaultNote(row.score, isPass),
          reviewerId,
        })
        if (isPass) passed++
        else failed++
      } catch {
        errors++
      }
    }

    setSummary({ total: rows.length, passed, failed, errors })
    setProcessing(false)
    if (errors === 0) message.success('Sınav sonuçları kaydedildi.')
    else message.warning('Bazı kayıtlar işlenemedi.')
    loadPending()   // işlenen kayıtlar EXAM_PENDING listesinden çıkar
  }

  const handleUpload = (file) => {
    const isCsv = file.type === 'text/csv' || file.name.toLowerCase().endsWith('.csv')
    if (!isCsv) {
      message.error('Geçersiz dosya formatı. Lütfen bir .csv dosyası yükleyiniz.')   // TC-7.6
      return Upload.LIST_IGNORE
    }
    const reader = new FileReader()
    reader.onload = (e) => processCsv(String(e.target?.result ?? ''))
    reader.onerror = () => message.error('Dosya okunamadı.')
    reader.readAsText(file, 'utf-8')
    return Upload.LIST_IGNORE   // parse + işlemeyi kendimiz yapıyoruz; sunucuya upload yok
  }

  const columns = [
    {
      title: 'Başvuru No',
      dataIndex: 'applicationId',
      key: 'applicationId',
      width: 140,
      render: (id) => <Text style={{ fontWeight: 500 }}>#YG-{id}</Text>,
    },
    {
      title: 'Ad Soyad',
      dataIndex: 'studentName',
      key: 'studentName',
      render: (name) => <Text style={{ fontWeight: 500 }}>{name}</Text>,
    },
    {
      title: 'E-posta',
      dataIndex: 'email',
      key: 'email',
      render: (email) => <Text type="secondary">{email}</Text>,
    },
  ]

  if (loading) {
    return (
      <div style={styles.loadingWrap}>
        <Spin size="large" />
        <Text type="secondary">Yükleniyor...</Text>
      </div>
    )
  }

  return (
    <div style={styles.page}>
      <div style={styles.pageHeader}>
        <Title level={2} style={{ margin: 0, color: '#1f212b', fontWeight: 700 }}>
          Toplu Sınav Sonucu Yükle
        </Title>
        <Text type="secondary" style={{ fontSize: 14 }}>
          Sınava yönlendirilen adayların sonuçlarını CSV ile yükleyin; sonuçlar doğrudan işlenir
        </Text>
      </div>

      {summary && (
        <Alert
          type={summary.errors > 0 ? 'warning' : 'success'}
          showIcon
          style={{ marginBottom: 24 }}
          message={`Toplam: ${summary.total} · Başarılı (Muaf): ${summary.passed} · Başarısız (Muaf Değil): ${summary.failed} · Hatalı: ${summary.errors}`}
        />
      )}

      <div style={styles.card}>
        <div style={styles.uploadBar}>
          <Upload accept=".csv" maxCount={1} showUploadList={false} beforeUpload={handleUpload} disabled={processing}>
            <Button icon={<UploadOutlined />} loading={processing}>CSV Dosyası Yükle</Button>
          </Upload>
          <Text type="secondary" style={{ fontSize: 12 }}>
            Zorunlu sütunlar: <code>studentId</code>, <code>score</code>, <code>result</code>
            {' '}(<code>Sınav Başarılı</code> / <code>Sınav Başarısız</code>). Yüklenince sonuçlar doğrudan işlenir.
          </Text>
        </div>
        {processing && (
          <div style={{ marginTop: 16, display: 'flex', alignItems: 'center', gap: 10 }}>
            <Spin />
            <Text type="secondary">Sonuçlar işleniyor…</Text>
          </div>
        )}
      </div>

      <div style={styles.card}>
        {applications.length === 0 ? (
          <Empty
            image={Empty.PRESENTED_IMAGE_SIMPLE}
            description="Sınav sonucu bekleyen başvuru bulunmuyor."
          />
        ) : (
          <>
            <Text type="secondary" style={{ fontSize: 13, display: 'block', marginBottom: 12 }}>
              Sınav sonucu bekleyen başvurular — CSV'de <code>studentId</code> olarak Başvuru No değerini kullanın.
            </Text>
            <Table
              columns={columns}
              dataSource={applications}
              rowKey="applicationId"
              pagination={false}
              size="middle"
            />
          </>
        )}
      </div>

      <div style={styles.footer}>
        <Button onClick={() => navigate('/ydyo/dashboard')}>Panele Dön</Button>
      </div>
    </div>
  )
}
