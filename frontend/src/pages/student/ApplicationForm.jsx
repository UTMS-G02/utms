import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Form, Input, Button, Typography, Row, Col, Select,
  InputNumber, DatePicker, Checkbox, Space, App, Upload,
} from 'antd'
import { InboxOutlined } from '@ant-design/icons'
import dayjs from 'dayjs'
import { useAuth } from '../../contexts/AuthContext'
import { applicationsApi } from '../../api/applications'

const { Title, Text } = Typography
const { Option } = Select

const TARGET_DEPT_OPTIONS = [
  'Bilgisayar Mühendisliği',
  'Elektrik-Elektronik Mühendisliği',
  'Makine Mühendisliği',
  'Endüstri Mühendisliği',
  'Kimya Mühendisliği',
  'Mimarlık',
]

const TARGET_FACULTY_OPTIONS = [
  'Mühendislik Fakültesi',
  'Mimarlık Fakültesi',
  'Fen Fakültesi',
]

const ACADEMIC_YEAR_OPTIONS = ['2025-2026', '2026-2027', '2027-2028']

// Yatay geçiş yalnızca belirli yarıyıllara (3. / 5.) yapılabilir.
const SEMESTER_OPTIONS = [
  { value: '3', label: '3. Yarıyıl' },
  { value: '5', label: '5. Yarıyıl' },
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
        form.setFieldsValue({
          currentUniversity: data.currentUniversity,
          currentDept: data.currentDepartment,
          currentYear: data.currentClass != null ? `${data.currentClass}. Sınıf` : '',
          gpa: data.gpa,
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

  const handleSubmit = async (values) => {
    setSubmitting(true)
    const {
      studentCertificate,
      transcript,
      yksResult,
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

      // ÖNEMLİ: Her belge için BENZERSİZ documentType gönderilmeli. Backend
      // (DocumentService.deleteExistingDocumentIfAny) aynı tipte yeni belge
      // gelince eskisini siler; iki belge de 'OTHER' olursa biri kaybolur.
      // Bu yüzden YKS sonucu / ders içerikleri ayrı tip, ek belgeler de
      // OTHER_1, OTHER_2... şeklinde numaralanır.
      const allFiles = [
        { type: 'STUDENT_CERTIFICATE', file: studentCertificate?.[0]?.originFileObj },
        { type: 'TRANSCRIPT', file: transcript?.[0]?.originFileObj },
        { type: 'YKS_RESULT', file: yksResult?.[0]?.originFileObj },
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
        requiredMark={requiredMark}
        size="large"
        initialValues={{
          yksScore: user?.yksScore ?? undefined,
          yksRanking: user?.yksRanking ?? undefined,
          // Kişisel bilgiler (ad/soyad/e-posta/TC/doğum tarihi/telefon) useEffect
          // ile auth /me'den doldurulur; currentUniversity / gpa vb. YÖKSİS'ten.
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
                rules={[{ required: true, message: 'Akademik yıl zorunludur.' }]}
              >
                <Select placeholder="Seçiniz">
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
              >
                <Select placeholder="Seçiniz">
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
                <Select placeholder="Seçiniz">
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
                <Select placeholder="Seçiniz">
                  {TARGET_DEPT_OPTIONS.map((d) => (
                    <Option key={d} value={d}>{d}</Option>
                  ))}
                </Select>
              </Form.Item>
            </Col>
            <Col xs={24} md={12}>
              <Form.Item
                label="SAY YKS Puanı"
                name="yksScore"
                rules={[{ required: true, message: 'YKS puanı zorunludur.' }]}
              >
                <InputNumber
                  style={{ width: '100%' }}
                  min={0}
                  max={500}
                  step={0.001}
                  placeholder="0.000"
                />
              </Form.Item>
            </Col>
            <Col xs={24} md={12}>
              <Form.Item
                label="SAY YKS Sıralaması"
                name="yksRanking"
                rules={[{ required: true, message: 'YKS sıralaması zorunludur.' }]}
              >
                <InputNumber
                  style={{ width: '100%' }}
                  min={1}
                  placeholder="Sıralamanız"
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

          <Form.Item
            label="Öğrenci Belgesi"
            name="studentCertificate"
            valuePropName="fileList"
            getValueFromEvent={(e) => (Array.isArray(e) ? e : e?.fileList)}
            rules={[{ required: true, message: 'Öğrenci belgesi zorunludur.' }]}
          >
            <Upload.Dragger accept=".pdf" maxCount={1} beforeUpload={beforeUpload} multiple={false}>
              <p className="ant-upload-drag-icon"><InboxOutlined /></p>
              <p className="ant-upload-text">Tıklayın veya sürükleyin</p>
              <p className="ant-upload-hint">Yalnızca PDF, max 10MB</p>
            </Upload.Dragger>
          </Form.Item>

          <Form.Item
            label="Transkript"
            name="transcript"
            valuePropName="fileList"
            getValueFromEvent={(e) => (Array.isArray(e) ? e : e?.fileList)}
            rules={[{ required: true, message: 'Transkript zorunludur.' }]}
          >
            <Upload.Dragger accept=".pdf" maxCount={1} beforeUpload={beforeUpload} multiple={false}>
              <p className="ant-upload-drag-icon"><InboxOutlined /></p>
              <p className="ant-upload-text">Tıklayın veya sürükleyin</p>
              <p className="ant-upload-hint">Yalnızca PDF, max 10MB</p>
            </Upload.Dragger>
          </Form.Item>

          <Form.Item
            label="YKS Sonuç Belgesi"
            name="yksResult"
            valuePropName="fileList"
            getValueFromEvent={(e) => (Array.isArray(e) ? e : e?.fileList)}
            rules={[{ required: true, message: 'YKS sonuç belgesi zorunludur.' }]}
          >
            <Upload.Dragger accept=".pdf" maxCount={1} beforeUpload={beforeUpload} multiple={false}>
              <p className="ant-upload-drag-icon"><InboxOutlined /></p>
              <p className="ant-upload-text">Tıklayın veya sürükleyin</p>
              <p className="ant-upload-hint">Yalnızca PDF, max 10MB</p>
            </Upload.Dragger>
          </Form.Item>

          <Form.Item
            label="Almış Olduğunuz Derslerin İçerikleri"
            name="courseContents"
            valuePropName="fileList"
            getValueFromEvent={(e) => (Array.isArray(e) ? e : e?.fileList)}
            rules={[{ required: true, message: 'Ders içerikleri belgesi zorunludur.' }]}
          >
            <Upload.Dragger accept=".pdf" maxCount={1} beforeUpload={beforeUpload} multiple={false}>
              <p className="ant-upload-drag-icon"><InboxOutlined /></p>
              <p className="ant-upload-text">Tıklayın veya sürükleyin</p>
              <p className="ant-upload-hint">Yalnızca PDF, max 10MB</p>
            </Upload.Dragger>
          </Form.Item>

          <Form.Item
            label="İngilizce Yeterlilik Belgesi"
            name="languageCert"
            valuePropName="fileList"
            getValueFromEvent={(e) => (Array.isArray(e) ? e : e?.fileList)}
          >
            <Upload.Dragger accept=".pdf" maxCount={1} beforeUpload={beforeUpload} multiple={false}>
              <p className="ant-upload-drag-icon"><InboxOutlined /></p>
              <p className="ant-upload-text">Tıklayın veya sürükleyin</p>
              <p className="ant-upload-hint">Yalnızca PDF, max 10MB (opsiyonel)</p>
            </Upload.Dragger>
          </Form.Item>

          <Form.Item
            label="Ek Belgeler"
            name="additionalDocs"
            valuePropName="fileList"
            getValueFromEvent={(e) => (Array.isArray(e) ? e : e?.fileList)}
          >
            <Upload.Dragger accept=".pdf" maxCount={5} beforeUpload={beforeUpload} multiple>
              <p className="ant-upload-drag-icon"><InboxOutlined /></p>
              <p className="ant-upload-text">Tıklayın veya sürükleyin</p>
              <p className="ant-upload-hint">Yalnızca PDF, max 10MB, en fazla 5 dosya (opsiyonel)</p>
            </Upload.Dragger>
          </Form.Item>
        </div>

        {/* KVKK — en sonda; formu kilitlemez, ama onay zorunlu */}
        <div style={styles.kvkkCard}>
          <Title level={5} style={{ marginTop: 0, marginBottom: 8 }}>
            Kişisel Verilerin Korunması
          </Title>
          <Text style={{ lineHeight: 1.6, display: 'block', marginBottom: 16 }}>
            Yatay geçiş başvurunuz kapsamında kişisel verilerinizin işlenmesine
            ilişkin Aydınlatma Metni'ni okudum, anladım ve onaylıyorum.
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
