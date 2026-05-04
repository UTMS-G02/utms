import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Form, Input, Button, Typography, Row, Col, Select,
  InputNumber, DatePicker, Checkbox, Space, App, Upload,
} from 'antd'
import { InboxOutlined } from '@ant-design/icons'
import { useAuth } from '../../contexts/AuthContext'
import { applicationsApi } from '../../api/applications'

const { Title, Text } = Typography
const { Option } = Select

const UNIVERSITIES = [
  'İzmir Yüksek Teknoloji Enstitüsü',
  'Ege Üniversitesi',
  'Dokuz Eylül Üniversitesi',
  'İstanbul Teknik Üniversitesi',
  'Boğaziçi Üniversitesi',
  'Diğer',
]

const YEAR_OPTIONS = ['1. Sınıf', '2. Sınıf', '3. Sınıf', '4. Sınıf']

const CURRENT_DEPT_OPTIONS = [
  'Fen Bilimleri',
  'Mühendislik',
  'Mimarlık',
  'Diğer',
]

const TARGET_DEPT_OPTIONS = [
  'Bilgisayar Mühendisliği',
  'Elektrik-Elektronik Mühendisliği',
  'Makine Mühendisliği',
  'Endüstri Mühendisliği',
  'Kimya Mühendisliği',
  'Mimarlık',
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

const tcknValidator = (_, value) => {
  if (!value) return Promise.reject(new Error('TC Kimlik No zorunludur.'))
  if (!/^\d{11}$/.test(value)) return Promise.reject(new Error('TC Kimlik No 11 haneli rakamdan oluşmalıdır.'))
  return Promise.resolve()
}

export default function ApplicationForm() {
  const [form] = Form.useForm()
  const [submitting, setSubmitting] = useState(false)
  const navigate = useNavigate()
  const { user } = useAuth()
  const { message } = App.useApp()

  const kvkkAccepted = Form.useWatch('kvkk', form)

  const nameParts = (user?.name ?? '').trim().split(/\s+/)
  const initialFirstName = user?.firstName || nameParts[0] || ''
  const initialLastName = user?.lastName || nameParts.slice(1).join(' ') || ''

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
      kvkk,
      ...formInfo
    } = values

    try {
      message.loading('Başvuru oluşturuluyor...', 0)
      const created = await applicationsApi.createApplication(formInfo)
      message.destroy()
      const applicationId = created.applicationId

      const allFiles = [
        { type: 'STUDENT_CERTIFICATE', file: studentCertificate?.[0]?.originFileObj },
        { type: 'TRANSCRIPT', file: transcript?.[0]?.originFileObj },
        { type: 'OTHER', file: yksResult?.[0]?.originFileObj },
        { type: 'OTHER', file: courseContents?.[0]?.originFileObj },
        { type: 'LANGUAGE_CERT', file: languageCert?.[0]?.originFileObj },
        ...(additionalDocs ?? []).map((f) => ({ type: 'OTHER', file: f.originFileObj })),
      ].filter((f) => f.file)

      let uploadedCount = 0
      for (const item of allFiles) {
        message.loading(`Belge yükleniyor ${uploadedCount + 1}/${allFiles.length}...`, 0)
        const fd = new FormData()
        fd.append('file', item.file)
        fd.append('docType', item.type)
        await applicationsApi.uploadDocument(applicationId, fd)
        uploadedCount++
      }

      message.destroy()
      await applicationsApi.submitApplication(applicationId)
      message.success('Başvurunuz başarıyla oluşturuldu ve gönderildi.')
      navigate(`/student/applications/${applicationId}`)
    } catch {
      message.destroy()
      message.error('İşlem sırasında bir hata oluştu. Lütfen tekrar deneyin.')
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
          Yatay geçiş başvuru formunu doldurun. Tüm alanlar zorunludur.
        </Text>
      </div>

      <Form
        form={form}
        layout="vertical"
        onFinish={handleSubmit}
        requiredMark={false}
        size="large"
        initialValues={{
          firstName: initialFirstName,
          lastName: initialLastName,
          email: user?.email ?? '',
          gpa: user?.currentGpa ?? undefined,
          yksScore: user?.yksScore ?? undefined,
          yksRanking: user?.yksRanking ?? undefined,
          currentUniversity: 'İzmir Yüksek Teknoloji Enstitüsü',
        }}
      >
        {/* KVKK */}
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

        {kvkkAccepted && (
          <>
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
                    <Input placeholder="Adınız" />
                  </Form.Item>
                </Col>
                <Col xs={24} md={12}>
                  <Form.Item
                    label="Soyad"
                    name="lastName"
                    rules={[{ required: true, message: 'Soyad zorunludur.' }]}
                  >
                    <Input placeholder="Soyadınız" />
                  </Form.Item>
                </Col>
                <Col xs={24} md={12}>
                  <Form.Item
                    label="TC Kimlik Numarası"
                    name="tcKimlik"
                    rules={[{ validator: tcknValidator }]}
                  >
                    <Input placeholder="11 haneli TC Kimlik No" maxLength={11} />
                  </Form.Item>
                </Col>
                <Col xs={24} md={12}>
                  <Form.Item
                    label="Doğum Tarihi"
                    name="birthDate"
                    rules={[{ required: true, message: 'Doğum tarihi zorunludur.' }]}
                  >
                    <DatePicker
                      style={{ width: '100%' }}
                      format="DD/MM/YYYY"
                      placeholder="Seçiniz"
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
                    <Input placeholder="(5XX) XXX-XXXX" />
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
                    label="Kayıtlı Olduğunuz Üniversite"
                    name="currentUniversity"
                    rules={[{ required: true, message: 'Üniversite zorunludur.' }]}
                  >
                    <Select placeholder="Seçiniz">
                      {UNIVERSITIES.map((u) => (
                        <Option key={u} value={u}>{u}</Option>
                      ))}
                    </Select>
                  </Form.Item>
                </Col>
                <Col xs={24} md={12}>
                  <Form.Item
                    label="Sınıf"
                    name="currentYear"
                    rules={[{ required: true, message: 'Sınıf zorunludur.' }]}
                  >
                    <Select placeholder="Seçiniz">
                      {YEAR_OPTIONS.map((y) => (
                        <Option key={y} value={y}>{y}</Option>
                      ))}
                    </Select>
                  </Form.Item>
                </Col>
                <Col xs={24} md={12}>
                  <Form.Item
                    label="Genel Not Ortalaması (GPA)"
                    name="gpa"
                    rules={[{ required: true, message: 'GPA zorunludur.' }]}
                  >
                    <InputNumber
                      style={{ width: '100%' }}
                      min={0}
                      max={4}
                      step={0.01}
                      placeholder="0.00"
                    />
                  </Form.Item>
                </Col>
                <Col xs={24} md={12}>
                  <Form.Item
                    label="Mevcut Bölümünüz"
                    name="currentDept"
                    rules={[{ required: true, message: 'Mevcut bölüm zorunludur.' }]}
                  >
                    <Select placeholder="Seçiniz">
                      {CURRENT_DEPT_OPTIONS.map((d) => (
                        <Option key={d} value={d}>{d}</Option>
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
          </>
        )}
      </Form>
    </div>
  )
}
