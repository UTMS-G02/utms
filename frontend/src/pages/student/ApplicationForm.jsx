import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Form, Input, Button, Typography, Row, Col, Select,
  InputNumber, DatePicker, Checkbox, Space, App, Upload, Alert,
} from 'antd'
import { UploadOutlined, CheckCircleOutlined, EyeOutlined } from '@ant-design/icons'
import dayjs from 'dayjs'
import { useAuth } from '../../contexts/AuthContext'
import { applicationsApi } from '../../api/applications'

const { Title, Text } = Typography
const { Option } = Select

// Fakülte → o fakülteye ait bölümler. Fakülte seçilince bölüm listesi buna göre filtrelenir.
const FACULTY_DEPARTMENTS = {
  'Mühendislik Fakültesi': [
    'Bilgisayar Mühendisliği',
    'Elektrik-Elektronik Mühendisliği',
    'Makine Mühendisliği',
    'Endüstri Mühendisliği',
    'Kimya Mühendisliği',
    'İnşaat Mühendisliği',
    'Gıda Mühendisliği',
    'Malzeme Bilimi ve Mühendisliği',
    'Biyomühendislik',
  ],
  'Mimarlık Fakültesi': [
    'Mimarlık',
    'Şehir ve Bölge Planlama',
    'Endüstriyel Tasarım',
  ],
  'Fen Fakültesi': [
    'Matematik',
    'Fizik',
    'Kimya',
    'Moleküler Biyoloji ve Genetik',
    'Fotonik',
  ],
}

const TARGET_FACULTY_OPTIONS = Object.keys(FACULTY_DEPARTMENTS)

// Başvurular yalnızca içinde bulunulan akademik yıla (2026-2027) yapılabilir.
const ACTIVE_ACADEMIC_YEAR = '2026-2027'
const ACADEMIC_YEAR_OPTIONS = [ACTIVE_ACADEMIC_YEAR]

// Yatay geçiş yalnızca belirli yarıyıllara (3. / 5.) yapılabilir. Hedef yarıyıl
// öğrencinin YÖKSİS'teki mevcut sınıfından türetilir (1. sınıf → 3, 2. sınıf → 5).
const SEMESTER_OPTIONS = [
  { value: '3', label: '3. Yarıyıl' },
  { value: '5', label: '5. Yarıyıl' },
]

// e-Devlet/ÖSYM'den "otomatik alınan" belgeler. Başvuru oluşturulmadan önce
// önizleme ucu (GET /yoksis/documents/{type}/preview) ile görüntülenebilir.
const EGOV_DOCUMENTS = [
  { type: 'STUDENT_CERTIFICATE', label: 'Öğrenci Belgesi — e-Devlet' },
  { type: 'TRANSCRIPT', label: 'Transkript (Not Dökümü) — e-Devlet' },
  { type: 'YKS_RESULT', label: 'YKS Sonuç Belgesi — ÖSYM' },
]

const styles = {
  page: {
    fontFamily: "'DM Sans', sans-serif",
  },
  pageHeader: {
    marginBottom: 32,
  },
  card: {
    background: '#ffffff',
    borderRadius: 10,
    padding: 24,
    boxShadow: '0 4px 16px rgba(0,0,0,0.06)',
    border: '1px solid #f0f0f0',
    marginBottom: 24,
  },
  kvkkCard: {
    background: '#ffffff',
    borderRadius: 10,
    padding: 24,
    boxShadow: '0 4px 16px rgba(0,0,0,0.06)',
    border: '1px solid #f0f0f0',
    borderLeft: '4px solid #8B1A2B',
    marginBottom: 24,
  },
  cardSubtitle: {
    display: 'block',
    marginBottom: 24,
  },
  readonlyInput: {
    background: '#f5f5f5',
    cursor: 'not-allowed',
    color: 'rgba(0,0,0,0.45)',
  },
  footer: {
    display: 'flex',
    justifyContent: 'flex-end',
    marginTop: 8,
  },
}

// Zorunlu alanlarda etiketin sağında kırmızı * göster
const requiredMark = (label, { required }) => (
  <>
    {label}
    {required && <span style={{ color: '#ff4d4f', marginLeft: 2 }}>*</span>}
  </>
)

const tcknValidator = (_, value) => {
  if (!value) return Promise.reject(new Error('TC Kimlik No zorunludur.'))
  if (!/^\d{11}$/.test(value)) return Promise.reject(new Error('TC Kimlik No 11 haneli rakamdan oluşmalıdır.'))
  return Promise.resolve()
}

export default function ApplicationForm() {
  const [form] = Form.useForm()
  const [submitting, setSubmitting] = useState(false)
  const [yoksisLoading, setYoksisLoading] = useState(true)
  const navigate = useNavigate()
  const { user } = useAuth()
  const { message } = App.useApp()

  // Seçili fakülteye göre bölüm listesini filtrelemek için izliyoruz.
  const selectedFaculty = Form.useWatch('targetFaculty', form)
  const departmentOptions = FACULTY_DEPARTMENTS[selectedFaculty] ?? []

  // Kişisel bilgiler kayıt sırasında verilen değerlerle (auth /me) otomatik
  // doldurulur ve salt-okunur gösterilir; öğrenci başvuru formunda değiştiremez.
  useEffect(() => {
    if (!user) return
    const nameParts = (user.name ?? '').trim().split(/\s+/)
    form.setFieldsValue({
      firstName: user.firstName || nameParts[0] || '',
      middleName: user.middleName ?? '',
      lastName: user.lastName || nameParts.slice(1).join(' ') || '',
      email: user.email ?? '',
      tcKimlik: user.tckn ?? '',
      birthDate: user.dateOfBirth ? dayjs(user.dateOfBirth) : null,
      phone: user.phoneNumber ?? '',
    })
  }, [user, form])

  // Akademik bilgiler YÖKSİS'ten otomatik gelir ve read-only gösterilir.
  // Formda görünen değer = backend'in create sırasında kaydettiği değerdir.
  useEffect(() => {
    let active = true
    applicationsApi
      .getMyYoksisData()
      .then((data) => {
        if (!active) return
        // Hedef yarıyıl YÖKSİS'teki mevcut yarıyıldan türetilir ve değiştirilemez:
        // 2. yarıyılı (1. sınıf) tamamlayan → 3., 4. yarıyılı (2. sınıf) tamamlayan → 5.
        const targetSemester = data.semester != null ? String(data.semester + 1) : undefined
        form.setFieldsValue({
          academicYear: ACTIVE_ACADEMIC_YEAR,
          semester: targetSemester,
          currentUniversity: data.currentUniversity,
          currentDept: data.currentDepartment,
          currentYear: data.currentClass != null ? `${data.currentClass}. Sınıf` : '',
          gpa: data.gpa,
          // YKS verileri de ÖSYM'den (mock) otomatik gelir; salt-okunur gösterilir.
          yksScore: data.yksScore,
          yksRanking: data.yksRank,
        })
      })
      .catch(() => {
        if (active) message.error('YÖKSİS akademik bilgileri alınamadı.')
      })
      .finally(() => {
        if (active) setYoksisLoading(false)
      })
    return () => {
      active = false
    }
  }, [form, message])

  // e-Devlet/ÖSYM belgesini yeni sekmede önizle — başvuru henüz oluşturulmadan,
  // o anki öğrencinin YÖKSİS verisinden backend mock PDF üretir (öğrenci işleri panelindeki gibi).
  const handleViewEgovDoc = async (type) => {
    try {
      const res = await applicationsApi.previewEgovDocument(type)
      const blob = new Blob([res.data], { type: 'application/pdf' })
      const url = window.URL.createObjectURL(blob)
      window.open(url, '_blank', 'noopener,noreferrer')
      setTimeout(() => window.URL.revokeObjectURL(url), 60_000)
    } catch {
      message.error('Belge görüntülenemedi. Lütfen tekrar deneyin.')
    }
  }

  const beforeUpload = (file) => {
    if (file.type !== 'application/pdf') {
      message.error('Sadece PDF yüklenebilir.')
      return Upload.LIST_IGNORE
    }
    if (file.size > 10 * 1024 * 1024) {
      message.error("Dosya 10MB'dan büyük olamaz.")
      return Upload.LIST_IGNORE
    }
    return false
  }

  // Form doğrulaması başarısız olunca SESSİZCE kalmasın: kullanıcıya net mesaj göster
  // ve ilk hatalı alana kaydır (aksi halde "butona bastım hiçbir şey olmadı" hissi oluşuyor).
  const onFinishFailed = ({ errorFields }) => {
    const first = errorFields?.[0]?.errors?.[0]
    message.error(
      first
        ? `Eksik/hatalı alan: ${first}`
        : 'Lütfen zorunlu alanları doldurun.'
    )
  }

  const handleSubmit = async (values) => {
    setSubmitting(true)
    const {
      courseContents,
      languageCert,
      additionalDocs,
    } = values

    // Backend ApplicationCreateRequest yalnızca şu alanları bekliyor; isimleri
    // form alanlarından bilerek farklı (sayYksScore/sayYksRank) ve kvkkAccepted
    // zorunlu. currentUniversity/gpa gibi alanlar backend'de YÖKSİS'ten
    // çekildiği için gönderilmiyor.
    // Akademik alanlar (currentUniversity / currentDept / currentYear / gpa)
    // YÖKSİS'ten türetildiği için payload'a dahil EDİLMEZ; backend bunları
    // create sırasında YÖKSİS'ten çeker. semester backend'de Integer beklenir.
    const payload = {
      academicYear: values.academicYear,
      semester: Number(values.semester),
      targetFaculty: values.targetFaculty,
      targetDepartment: values.targetDepartment,
      sayYksScore: values.yksScore,
      sayYksRank: values.yksRanking,
      kvkkAccepted: values.kvkk === true,
    }

    try {
      message.loading('Başvuru oluşturuluyor...', 0)
      const created = await applicationsApi.createApplication(payload)
      message.destroy()
      const applicationId = created.id

      // NOT: Öğrenci belgesi, transkript ve YKS sonuç belgesi e-Devlet/ÖSYM'den
      // backend tarafından OTOMATİK üretilip iliştirilir; burada yüklenmez.
      // ÖNEMLİ: Her belge için BENZERSİZ documentType gönderilmeli. Backend
      // (DocumentService.deleteExistingDocumentIfAny) aynı tipte yeni belge
      // gelince eskisini siler; iki belge de 'OTHER' olursa biri kaybolur.
      const allFiles = [
        { type: 'COURSE_CONTENTS', file: courseContents?.[0]?.originFileObj },
        { type: 'LANGUAGE_CERT', file: languageCert?.[0]?.originFileObj },
        ...(additionalDocs ?? []).map((f, i) => ({ type: `OTHER_${i + 1}`, file: f.originFileObj })),
      ].filter((f) => f.file)

      let uploadedCount = 0
      for (const item of allFiles) {
        message.loading(`Belge yükleniyor ${uploadedCount + 1}/${allFiles.length}...`, 0)
        await applicationsApi.uploadDocument(applicationId, item.file, item.type)
        uploadedCount++
      }

      message.destroy()
      await applicationsApi.submitApplication(applicationId)
      message.success('Başvurunuz başarıyla oluşturuldu ve gönderildi.')
      navigate(`/student/applications/${applicationId}`)
    } catch (err) {
      message.destroy()
      // Backend iş-kuralı hatalarını { message } olarak, @Valid hatalarını
      // { errors: { alan: mesaj } } olarak döndürüyor (GlobalExceptionHandler).
      // Öğrencinin neden reddedildiğini görmesi için gerçek mesajı gösteriyoruz.
      const data = err?.response?.data
      const fieldError = data?.errors && Object.values(data.errors)[0]
      message.error(data?.message || fieldError || 'İşlem sırasında bir hata oluştu. Lütfen tekrar deneyin.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div style={styles.page}>
      <div style={styles.pageHeader}>
        <Title level={2} style={{ margin: 0, color: '#1f212b', fontWeight: 700 }}>
          Yeni Başvuru
        </Title>
        <Text type="secondary" style={{ fontSize: 14 }}>
          Yatay geçiş başvuru formunu doldurun. Zorunlu alanlar * ile işaretlenmiştir.
        </Text>
      </div>

      <Form
        form={form}
        layout="vertical"
        onFinish={handleSubmit}
        onFinishFailed={onFinishFailed}
        scrollToFirstError
        requiredMark={requiredMark}
        size="large"
        initialValues={{
          // Kişisel bilgiler (ad/soyad/e-posta/TC/doğum tarihi/telefon) useEffect
          // ile auth /me'den; currentUniversity / gpa / YKS verileri YÖKSİS/ÖSYM'den.
        }}
      >
        {/* Kişisel Bilgiler */}
        <div style={styles.card}>
          <Title level={5} style={{ marginTop: 0, marginBottom: 4 }}>Kişisel Bilgiler</Title>
          <Text type="secondary" style={styles.cardSubtitle}>
            Temel iletişim ve kimlik bilgileriniz
          </Text>
          <Row gutter={16}>
            <Col xs={24} md={12}>
              <Form.Item
                label="Ad"
                name="firstName"
                rules={[{ required: true, message: 'Ad zorunludur.' }]}
              >
                <Input placeholder="Adınız" readOnly style={styles.readonlyInput} />
              </Form.Item>
            </Col>
            <Col xs={24} md={12}>
              <Form.Item label="İkinci Ad" name="middleName">
                <Input placeholder="—" readOnly style={styles.readonlyInput} />
              </Form.Item>
            </Col>
            <Col xs={24} md={12}>
              <Form.Item
                label="Soyad"
                name="lastName"
                rules={[{ required: true, message: 'Soyad zorunludur.' }]}
              >
                <Input placeholder="Soyadınız" readOnly style={styles.readonlyInput} />
              </Form.Item>
            </Col>
            <Col xs={24} md={12}>
              <Form.Item
                label="TC Kimlik Numarası"
                name="tcKimlik"
                required
                rules={[{ validator: tcknValidator }]}
              >
                <Input placeholder="11 haneli TC Kimlik No" maxLength={11} readOnly style={styles.readonlyInput} />
              </Form.Item>
            </Col>
            <Col xs={24} md={12}>
              <Form.Item
                label="Doğum Tarihi"
                name="birthDate"
                rules={[{ required: true, message: 'Doğum tarihi zorunludur.' }]}
              >
                <DatePicker
                  style={{ width: '100%', ...styles.readonlyInput }}
                  format="DD/MM/YYYY"
                  placeholder="Seçiniz"
                  disabled
                />
              </Form.Item>
            </Col>
            <Col xs={24} md={12}>
              <Form.Item
                label="E-posta Adresi"
                name="email"
                rules={[
                  { required: true, message: 'E-posta zorunludur.' },
                  { type: 'email', message: 'Geçerli bir e-posta giriniz.' },
                ]}
              >
                <Input
                  placeholder="eposta@ogrenci.edu.tr"
                  readOnly
                  style={styles.readonlyInput}
                />
              </Form.Item>
            </Col>
            <Col xs={24} md={12}>
              <Form.Item
                label="Telefon Numarası"
                name="phone"
                rules={[{ required: true, message: 'Telefon numarası zorunludur.' }]}
              >
                <Input placeholder="(5XX) XXX-XXXX" readOnly style={styles.readonlyInput} />
              </Form.Item>
            </Col>
          </Row>
        </div>

        {/* Akademik Bilgiler */}
        <div style={styles.card}>
          <Title level={5} style={{ marginTop: 0, marginBottom: 4 }}>Akademik Bilgiler</Title>
          <Text type="secondary" style={styles.cardSubtitle}>
            Mevcut akademik durumunuz ve tercihleriniz
          </Text>
          <Row gutter={16}>
            <Col xs={24} md={12}>
              <Form.Item
                label="Akademik Yıl"
                name="academicYear"
                initialValue={ACTIVE_ACADEMIC_YEAR}
                rules={[{ required: true, message: 'Akademik yıl zorunludur.' }]}
                extra="Başvurular yalnızca içinde bulunulan akademik yıla yapılabilir."
              >
                <Select disabled>
                  {ACADEMIC_YEAR_OPTIONS.map((y) => (
                    <Option key={y} value={y}>{y}</Option>
                  ))}
                </Select>
              </Form.Item>
            </Col>
            <Col xs={24} md={12}>
              <Form.Item
                label="Başvurulan Yarıyıl"
                name="semester"
                rules={[{ required: true, message: 'Yarıyıl zorunludur.' }]}
                extra="Sınıfınıza göre YÖKSİS'ten otomatik belirlenir: 1. sınıf → 3. yarıyıl, 2. sınıf → 5. yarıyıl. Yalnızca bu geçişler kabul edilir."
              >
                <Select disabled placeholder={yoksisLoading ? 'YÖKSİS\'ten belirleniyor...' : 'Seçiniz'}>
                  {SEMESTER_OPTIONS.map((s) => (
                    <Option key={s.value} value={s.value}>{s.label}</Option>
                  ))}
                </Select>
              </Form.Item>
            </Col>
            {/* YÖKSİS'ten otomatik gelen, salt-okunur akademik alanlar. */}
            <Col xs={24}>
              <Text type="secondary" style={{ display: 'block', marginBottom: 8 }}>
                Aşağıdaki bilgiler YÖKSİS'ten otomatik alınmıştır ve düzenlenemez.
              </Text>
            </Col>
            <Col xs={24} md={12}>
              <Form.Item label="Kayıtlı Olduğunuz Üniversite" name="currentUniversity">
                <Input
                  readOnly
                  disabled={yoksisLoading}
                  placeholder={yoksisLoading ? 'YÖKSİS\'ten alınıyor...' : ''}
                  style={styles.readonlyInput}
                />
              </Form.Item>
            </Col>
            <Col xs={24} md={12}>
              <Form.Item label="Sınıf" name="currentYear">
                <Input readOnly disabled={yoksisLoading} style={styles.readonlyInput} />
              </Form.Item>
            </Col>
            <Col xs={24} md={12}>
              <Form.Item
                label="Genel Not Ortalaması (GPA)"
                name="gpa"
                extra="Yatay geçiş için minimum 2.50 ortalama gereklidir; altındaki başvurular kabul edilmez."
              >
                <InputNumber
                  style={{ width: '100%', ...styles.readonlyInput }}
                  readOnly
                  disabled={yoksisLoading}
                  precision={2}
                  placeholder="0.00"
                />
              </Form.Item>
            </Col>
            <Col xs={24} md={12}>
              <Form.Item label="Mevcut Bölümünüz" name="currentDept">
                <Input readOnly disabled={yoksisLoading} style={styles.readonlyInput} />
              </Form.Item>
            </Col>
            <Col xs={24} md={12}>
              <Form.Item
                label="Geçmek İstediğiniz Fakülte"
                name="targetFaculty"
                rules={[{ required: true, message: 'Hedef fakülte zorunludur.' }]}
              >
                <Select
                  placeholder="Seçiniz"
                  onChange={() => form.setFieldsValue({ targetDepartment: undefined })}
                >
                  {TARGET_FACULTY_OPTIONS.map((f) => (
                    <Option key={f} value={f}>{f}</Option>
                  ))}
                </Select>
              </Form.Item>
            </Col>
            <Col xs={24} md={12}>
              <Form.Item
                label="Geçmek İstediğiniz Bölüm"
                name="targetDepartment"
                rules={[{ required: true, message: 'Hedef bölüm zorunludur.' }]}
              >
                <Select
                  placeholder={selectedFaculty ? 'Seçiniz' : 'Önce fakülte seçiniz'}
                  disabled={!selectedFaculty}
                >
                  {departmentOptions.map((d) => (
                    <Option key={d} value={d}>{d}</Option>
                  ))}
                </Select>
              </Form.Item>
            </Col>
            <Col xs={24} md={12}>
              <Form.Item
                label="SAY YKS Puanı"
                name="yksScore"
                extra="ÖSYM'den otomatik alınmıştır ve düzenlenemez."
              >
                <InputNumber
                  style={{ width: '100%', ...styles.readonlyInput }}
                  readOnly
                  disabled={yoksisLoading}
                  precision={3}
                  placeholder={yoksisLoading ? 'ÖSYM\'den alınıyor...' : '0.000'}
                />
              </Form.Item>
            </Col>
            <Col xs={24} md={12}>
              <Form.Item
                label="SAY YKS Sıralaması"
                name="yksRanking"
                extra="ÖSYM'den otomatik alınmıştır ve düzenlenemez."
              >
                <InputNumber
                  style={{ width: '100%', ...styles.readonlyInput }}
                  readOnly
                  disabled={yoksisLoading}
                  placeholder={yoksisLoading ? 'ÖSYM\'den alınıyor...' : 'Sıralamanız'}
                />
              </Form.Item>
            </Col>
          </Row>
        </div>

        {/* Belge Yüklemeleri */}
        <div style={styles.card}>
          <Title level={5} style={{ marginTop: 0, marginBottom: 4 }}>Belge Yüklemeleri</Title>
          <Text type="secondary" style={styles.cardSubtitle}>
            Gerekli belgeleri yükleyin
          </Text>

          {/* e-Devlet/ÖSYM'den otomatik alınan belgeler — öğrenci yüklemez */}
          <Alert
            type="success"
            showIcon
            icon={<CheckCircleOutlined />}
            style={{ marginBottom: 20 }}
            message="e-Devlet / ÖSYM'den otomatik alınan belgeler"
            description={
              <div style={{ marginTop: 8 }}>
                {EGOV_DOCUMENTS.map((d) => (
                  <div
                    key={d.type}
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'space-between',
                      gap: 12,
                      padding: '4px 0',
                    }}
                  >
                    <Text>• {d.label}</Text>
                    <Button
                      size="small"
                      icon={<EyeOutlined />}
                      onClick={() => handleViewEgovDoc(d.type)}
                      disabled={yoksisLoading}
                    >
                      Görüntüle
                    </Button>
                  </div>
                ))}
              </div>
            }
          />

          <Form.Item
            label="Almış Olduğunuz Derslerin İçerikleri"
            name="courseContents"
            valuePropName="fileList"
            getValueFromEvent={(e) => (Array.isArray(e) ? e : e?.fileList)}
            rules={[{ required: true, message: 'Ders içerikleri belgesi zorunludur.' }]}
            extra="Yalnızca PDF, max 10MB"
          >
            <Upload accept=".pdf" maxCount={1} beforeUpload={beforeUpload} multiple={false}>
              <Button icon={<UploadOutlined />}>PDF Seç</Button>
            </Upload>
          </Form.Item>

          <Form.Item
            label="İngilizce Yeterlilik Belgesi"
            name="languageCert"
            valuePropName="fileList"
            getValueFromEvent={(e) => (Array.isArray(e) ? e : e?.fileList)}
            extra="Yalnızca PDF, max 10MB (opsiyonel)"
          >
            <Upload accept=".pdf" maxCount={1} beforeUpload={beforeUpload} multiple={false}>
              <Button icon={<UploadOutlined />}>PDF Seç</Button>
            </Upload>
          </Form.Item>

          <Form.Item
            label="Ek Belgeler"
            name="additionalDocs"
            valuePropName="fileList"
            getValueFromEvent={(e) => (Array.isArray(e) ? e : e?.fileList)}
            extra="Yalnızca PDF, max 10MB, en fazla 5 dosya (opsiyonel)"
          >
            <Upload accept=".pdf" maxCount={5} beforeUpload={beforeUpload} multiple>
              <Button icon={<UploadOutlined />}>PDF Seç</Button>
            </Upload>
          </Form.Item>
        </div>

        {/* KVKK — en sonda; formu kilitlemez, ama onay zorunlu */}
        <div style={styles.kvkkCard}>
          <Title level={5} style={{ marginTop: 0, marginBottom: 8 }}>
            Kişisel Verilerin Korunması
          </Title>
          <Text style={{ lineHeight: 1.6, display: 'block', marginBottom: 16 }}>
            Yatay geçiş başvurunuz kapsamında kişisel verilerinizin işlenmesine
            ilişkin{' '}
            <a href="/kvkk" target="_blank" rel="noopener noreferrer" style={{ color: '#8B1A2B' }}>
              Aydınlatma Metni
            </a>'ni okudum, anladım ve onaylıyorum.
          </Text>
          <Form.Item
            name="kvkk"
            valuePropName="checked"
            style={{ marginBottom: 0 }}
            rules={[{
              validator: (_, value) => value
                ? Promise.resolve()
                : Promise.reject(new Error('Devam etmek için KVKK metnini onaylamanız gerekir.'))
            }]}
          >
            <Checkbox>KVKK metnini okudum ve onaylıyorum</Checkbox>
          </Form.Item>
        </div>

        {/* Footer */}
        <div style={styles.footer}>
          <Space>
            <Button onClick={() => navigate('/student/dashboard')}>
              Geri Dön
            </Button>
            <Button
              type="primary"
              htmlType="submit"
              loading={submitting}
              style={{ background: '#8B1A2B', borderColor: '#8B1A2B', fontWeight: 600 }}
            >
              Başvuruyu Oluştur
            </Button>
          </Space>
        </div>
      </Form>
    </div>
  )
}
