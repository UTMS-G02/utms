import { useState, useEffect } from 'react'
import { useNavigate, useLocation, useSearchParams } from 'react-router-dom'
import { Form, Input, Button, Typography, App, Modal, Checkbox, DatePicker, Row, Col } from 'antd'
import { MailOutlined, LockOutlined } from '@ant-design/icons'
import { useAuth, ROLE_HOME } from '../../contexts/AuthContext'
import { authApi } from '../../api/auth' // Gerçek API bağlantımız
import { tcknRule, phoneRule, dobRule } from '../../utils/validators' // Doğrulama kurallarımız
import iyteLogo from '../../assets/iyte_logo.png'
import PasswordStrengthIndicator from '../../components/PasswordStrengthIndicator'

const { Title, Text } = Typography

const styles = {
  page: {
    minHeight: '100vh',
    background: '#f2f2f7',
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    fontFamily: "'DM Sans', sans-serif",
    padding: '24px',
    transition: 'filter 0.2s ease',
  },
  pageBlur: {
    filter: 'blur(8px)',
  },
  card: {
    background: '#ffffff',
    borderRadius: 10,
    padding: 24,
    width: '100%',
    maxWidth: 480,
    boxShadow: '0 16px 40px rgba(0,0,0,0.08)',
    position: 'relative',
  },
  logoWrap: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    marginBottom: 32,
    gap: 16,
  },
  linkRow: {
    display: 'flex',
    justifyContent: 'space-between',
    marginTop: 8,
    marginLeft: 4,
    marginRight: 4,
  },
  actionText: {
    color: '#8B1A2B',
    fontSize: 15,
    fontWeight: 700,
    background: 'none',
    border: 'none',
    padding: 0,
    cursor: 'pointer',
  },
  modalContent: {
    paddingTop: 8,
  },
  checkbox: {
    display: 'flex',
    alignItems: 'center',
    boxShadow: '0 1px 4px rgba(0,0,0,0.15)',
    background: '#f9f9f9',
    padding: '8px',
    borderRadius: 6,
  },
}

export default function LoginPage({ initialModal }) {
  const [form] = Form.useForm()
  const [registerForm] = Form.useForm()
  const [forgotForm] = Form.useForm()
  const [resetForm] = Form.useForm()
  const passwordValue = Form.useWatch('password', registerForm)
  const resetPasswordValue = Form.useWatch('newPassword', resetForm)
  const [loading, setLoading] = useState(false)
  const [registerLoading, setRegisterLoading] = useState(false) // Kayıt için ayrı loading state'i
  const [forgotLoading, setForgotLoading] = useState(false)
  const [resetLoading, setResetLoading] = useState(false)
  const [modalType, setModalType] = useState(initialModal || '')
  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const { message } = App.useApp()
  const [searchParams] = useSearchParams()
  const resetToken = searchParams.get('token')

  useEffect(() => {
    if (initialModal) {
      setModalType(initialModal)
    }
  }, [initialModal])

  useEffect(() => {
    if (initialModal === 'reset' && !resetToken) {
      message.error('Geçersiz şifre sıfırlama bağlantısı.')
      navigate('/login', { replace: true })
    }
  }, [initialModal, resetToken])

  const from = location.state?.from?.pathname ?? null

  const closeModal = () => {
    setModalType('')
    if (location.pathname !== '/login') {
      navigate('/login', { replace: true })
    }
  }

  // --- GERÇEK API KAYIT İŞLEMİ ---
  const handleRegister = async (values) => {
    setRegisterLoading(true)
    try {
      const payload = {
        email: values.email,
        password: values.password,
        firstName: values.firstName,
        middleName: values.middleName || "", // Opsiyonel alan
        lastName: values.lastName,
        tckn: values.tckn,
        phoneNumber: values.phoneNumber,
        dateOfBirth: values.dateOfBirth.format('YYYY-MM-DD'), // Tarih objesini string'e çeviriyoruz
        kvkkAccepted: values.kvkk
      };

      await authApi.register(payload)
      message.success('Kayıt başarılı. Şimdi giriş yapabilirsiniz.')
      registerForm.resetFields()
      closeModal()
    } catch (error) {
      const errorMsg = error.response?.data?.message || 'Kayıt olurken bir hata oluştu.'
      message.error(errorMsg)
    } finally {
      setRegisterLoading(false)
    }
  }

  const handleForgotPassword = async ({ email }) => {
    setForgotLoading(true)
    try {
      await authApi.forgotPassword(email)
      message.success('Eğer bu e-posta sistemde kayıtlıysa, şifre sıfırlama bağlantısı gönderilmiştir.')
      forgotForm.resetFields()
      closeModal()
    } catch (error) {
      message.error(error.response?.data?.message || 'İşlem sırasında bir hata oluştu.')
    } finally {
      setForgotLoading(false)
    }
  }

  const handleResetPassword = async ({ newPassword }) => {
    if (!resetToken) {
      message.error('Geçersiz şifre sıfırlama bağlantısı.')
      return
    }
    setResetLoading(true)
    try {
      await authApi.resetPassword(resetToken, newPassword)
      message.success('Şifreniz başarıyla güncellendi. Şimdi giriş yapabilirsiniz.')
      resetForm.resetFields()
      navigate('/login', { replace: true })
      setModalType('')
    } catch (error) {
      const status = error.response?.status
      if (status === 400 || status === 401) {
        message.error('Şifre sıfırlama bağlantısı geçersiz veya süresi dolmuş.')
      } else {
        message.error('Şifre güncellenemedi. Lütfen tekrar deneyin.')
      }
    } finally {
      setResetLoading(false)
    }
  }

  const handleSubmit = async ({ email, password }) => {
    setLoading(true)
    try {
      const user = await login(email, password)
      message.success(`Hoş geldiniz, ${user.firstName}!`)
      const destination = from ?? ROLE_HOME[user.role] ?? '/'
      navigate(destination, { replace: true })
    } catch (err) {
      const status = err.response?.status
      if (status === 401) {
        message.error('E-posta veya şifre hatalı.')
      } else if (status === 403) {
        message.warning('Hesabınız henüz doğrulanmamış.')
      } else {
        message.error('Giriş yapılamadı. Lütfen tekrar deneyin.')
      }
    } finally {
      setLoading(false)
    }
  }

  return (
    <div style={styles.page}>
      <div style={styles.logoWrap}>
          <img
            src={iyteLogo}
            alt="İYTE logo"
            style={{ width: 156, height: 156, objectFit: 'contain' }}
          />
          <div style={{ textAlign: 'center' }}>
            <Title level={4} style={{ margin: 0, color: '#1f212b', fontWeight: 500 }}>
              Yatay Geçiş Başvuru Portalı
            </Title>
          </div>
        </div>
      <div style={styles.card}>

        <Form
          form={form}
          layout="vertical"
          onFinish={handleSubmit}
          requiredMark={false}
          size="large"
        >
          <Form.Item
            label="E-posta Adresi"
            name="email"
            rules={[
              { required: true, message: 'E-posta adresi zorunludur.' },
              { type: 'email', message: 'Geçerli bir e-posta adresi giriniz.' },
            ]}
          >
            <Input
              prefix={<MailOutlined style={{ color: '#9ca3af' }} />}
              placeholder="eposta@ogrenci.edu.tr"
              autoComplete="email"
            />
          </Form.Item>

          <Form.Item
            label="Şifre"
            name="password"
            rules={[{ required: true, message: 'Şifre zorunludur.' }]}
          >
            <Input.Password
              prefix={<LockOutlined style={{ color: '#9ca3af' }} />}
              placeholder="Şifrenizi girin"
              autoComplete="current-password"
            />
          </Form.Item>

          <Form.Item style={{ marginBottom: 24 }}>
            <Button
              type="primary"
              htmlType="submit"
              loading={loading}
              block
              style={{
                background: '#8B1A2B',
                borderColor: '#8B1A2B',
                height: 45,
                fontWeight: 600,
                fontSize: 15,
                letterSpacing: '0.01em',
              }}
            >
              Giriş Yap
            </Button>
          </Form.Item>
        </Form>

        <div style={styles.linkRow}>
          <button type="button" style={styles.actionText} onClick={() => setModalType('register')}>
            Kayıt Ol
          </button>
          <button type="button" style={styles.actionText} onClick={() => setModalType('forgot')}>
            Şifremi Unuttum
          </button>
        </div>
      </div>

      {/* --- KAYIT OL MODALI --- */}
      <Modal
        open={modalType === 'register'}
        title="Hesap Oluştur"
        onCancel={closeModal}
        footer={null}
        centered
        width={600} // Form genişlediği için width 500'den 600'e çıkarıldı
      >
        <div style={styles.modalContent}>
          <Text type="secondary" style={{ display: 'block', marginBottom: 16 }}>
            Yatay geçiş portalına erişmek için kayıt olun
          </Text>

          <Form
            form={registerForm}
            layout="vertical"
            onFinish={handleRegister}
            requiredMark={false}
            size="large"
          >
            {/* Ad ve İkinci Ad */}
            <Row gutter={16}>
              <Col span={12}>
                <Form.Item label="Ad *" name="firstName" rules={[{ required: true, message: 'Ad zorunludur.' }]}>
                  <Input placeholder="Örn: Ahmet" />
                </Form.Item>
              </Col>
              <Col span={12}>
                <Form.Item label="İkinci Ad (Opsiyonel)" name="middleName">
                  <Input placeholder="Örn: Can" />
                </Form.Item>
              </Col>
            </Row>

            {/* Soyad ve TCKN */}
            <Row gutter={16}>
              <Col span={12}>
                <Form.Item label="Soyad *" name="lastName" rules={[{ required: true, message: 'Soyad zorunludur.' }]}>
                  <Input placeholder="Örn: Yılmaz" />
                </Form.Item>
              </Col>
              <Col span={12}>
                <Form.Item label="TC Kimlik No *" name="tckn" rules={[tcknRule()]}>
                  <Input placeholder="11 haneli TCKN" maxLength={11} />
                </Form.Item>
              </Col>
            </Row>

            {/* Telefon ve Doğum Tarihi */}
            <Row gutter={16}>
              <Col span={12}>
                <Form.Item label="Telefon *" name="phoneNumber" rules={[phoneRule()]}>
                  <Input placeholder="Örn: 05xxxxxxxxx" />
                </Form.Item>
              </Col>
              <Col span={12}>
                <Form.Item label="Doğum Tarihi *" name="dateOfBirth" rules={[dobRule()]}>
                  <DatePicker style={{ width: '100%' }} format="DD/MM/YYYY" placeholder="Seçiniz" />
                </Form.Item>
              </Col>
            </Row>

            {/* E-posta */}
            <Form.Item
              label="E-posta Adresi *"
              name="email"
              rules={[
                { required: true, message: 'E-posta adresi zorunludur.' },
                { type: 'email', message: 'Geçerli bir e-posta adresi giriniz.' },
                { pattern: /^[^\s@]+@[^\s@]+\.edu\.tr$/i, message: 'Kurumsal e-posta (.edu.tr) giriniz.' }
              ]}
            >
              <Input placeholder="eposta@iyte.edu.tr" autoComplete="email" />
            </Form.Item>

            {/* Şifreler */}
            <Row gutter={16}>
              <Col span={12}>
                <Form.Item
                  label="Şifre *"
                  name="password"
                  rules={[
                    { required: true, message: 'Şifre zorunludur.' },
                    {
                      validator: (_, value) => {
                        if (!value) return Promise.resolve()
                        const checks = [
                          value.length >= 8,
                          /[A-Z]/.test(value),
                          /[a-z]/.test(value),
                          /[0-9]/.test(value),
                        ]
                        if (checks.every(Boolean)) return Promise.resolve()
                        return Promise.reject(new Error('Şifre kurallarını sağlamıyor.'))
                      },
                    },
                  ]}
                  hasFeedback
                >
                  <Input.Password placeholder="Şifre oluşturun" autoComplete="new-password" />
                </Form.Item>
                <PasswordStrengthIndicator value={passwordValue} />
              </Col>
              <Col span={12}>
                <Form.Item
                  label="Şifre Tekrar *"
                  name="confirm"
                  dependencies={["password"]}
                  hasFeedback
                  rules={[
                    { required: true, message: 'Şifrenizi tekrar girin.' },
                    ({ getFieldValue }) => ({
                      validator(_, value) {
                        if (!value || getFieldValue('password') === value) {
                          return Promise.resolve()
                        }
                        return Promise.reject(new Error('Şifreler eşleşmiyor.'))
                      },
                    }),
                  ]}
                >
                  <Input.Password placeholder="Şifrenizi tekrar girin" autoComplete="new-password" />
                </Form.Item>
              </Col>
            </Row>

            {/* KVKK Onayı */}
            <Form.Item
              name="kvkk"
              valuePropName="checked"
              rules={[{ validator: (_, value) => (value ? Promise.resolve() : Promise.reject('Aydınlatma Metni onayı gerekli.')) }]}
            >
              <div style={styles.checkbox}>
              <Checkbox style={{fontSize: 12}}>
                Yatay geçiş başvurum kapsamında kişisel verilerimin işlenmesine ilişkin <a href="/kvkk" target="_blank" rel="noopener noreferrer" style={{ color: '#8B1A2B' }}>Aydınlatma Metni</a>’ni okudum, anladım ve onaylıyorum.
              </Checkbox>
              </div>
            </Form.Item>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 12, marginTop: 12 }}>
              <Button onClick={closeModal} style={{ minWidth: 60 }}>
                İptal
              </Button>
              <Button type="primary" htmlType="submit" loading={registerLoading} style={{ minWidth: 60, background: '#8B1A2B', borderColor: '#8B1A2B' }}>
                Kayıt Ol
              </Button>
            </div>
          </Form>
        </div>
      </Modal>

      {/* --- ŞİFREMİ UNUTTUM MODALI --- */}
      <Modal
        open={modalType === 'forgot'}
        title="Şifre Sıfırlama"
        onCancel={closeModal}
        footer={null}
        centered
        width={550}
      >
        <div style={styles.modalContent}>
          <Text type="secondary" style={{ display: 'block'}}>
            E-posta adresinizi girin, size şifre sıfırlama talimatları gönderelim
          </Text>

          <Form
            form={forgotForm}
            layout="vertical"
            onFinish={handleForgotPassword}
            requiredMark={false}
            size="large"
            style={{ marginTop: 16 }}
          >
            <Form.Item
              label="E-posta Adresi *"
              name="email"
              rules={[
                { required: true, message: 'E-posta adresi zorunludur.' },
                { type: 'email', message: 'Geçerli bir e-posta adresi giriniz.' },
              ]}
            >
              <Input
                prefix={<MailOutlined style={{ color: '#9ca3af' }} />}
                placeholder="eposta@ogrenci.edu.tr"
                autoComplete="email"
              />
            </Form.Item>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 12, marginTop: 12 }}>
              <Button onClick={closeModal} style={{ minWidth: 60 }}>
                İptal
              </Button>
              <Button type="primary" htmlType="submit" loading={forgotLoading} style={{ minWidth: 60, background: '#8B1A2B', borderColor: '#8B1A2B' }}>
                Sıfırlama Bağlantısı Gönder
              </Button>
            </div>
          </Form>
        </div>
      </Modal>

      {/* --- ŞİFRE SIFIRLAMA MODALI --- */}
      <Modal
        open={modalType === 'reset'}
        title="Yeni Şifre Belirle"
        onCancel={() => {
          setModalType('')
          navigate('/login', { replace: true })
        }}
        footer={null}
        centered
        width={550}
        maskClosable={false}
      >
        <div style={styles.modalContent}>
          <Text type="secondary" style={{ display: 'block', marginBottom: 16 }}>
            Yeni şifrenizi belirleyin. Şifreniz güçlü olmalıdır.
          </Text>
          <Form
            form={resetForm}
            layout="vertical"
            onFinish={handleResetPassword}
            requiredMark={false}
            size="large"
          >
            <Form.Item
              label="Yeni Şifre *"
              name="newPassword"
              rules={[
                { required: true, message: 'Yeni şifre zorunludur.' },
                {
                  validator: (_, value) => {
                    if (!value) return Promise.resolve()
                    const checks = [
                      value.length >= 8,
                      /[A-Z]/.test(value),
                      /[a-z]/.test(value),
                      /[0-9]/.test(value),
                    ]
                    if (checks.every(Boolean)) return Promise.resolve()
                    return Promise.reject(new Error('Şifre kurallarını sağlamıyor.'))
                  },
                },
              ]}
              hasFeedback
            >
              <Input.Password
                prefix={<LockOutlined style={{ color: '#9ca3af' }} />}
                placeholder="Yeni şifrenizi girin"
                autoComplete="new-password"
              />
            </Form.Item>
            <PasswordStrengthIndicator value={resetPasswordValue} />
            <Form.Item
              label="Yeni Şifre (Tekrar) *"
              name="confirmNewPassword"
              dependencies={['newPassword']}
              hasFeedback
              rules={[
                { required: true, message: 'Şifrenizi tekrar girin.' },
                ({ getFieldValue }) => ({
                  validator(_, value) {
                    if (!value || getFieldValue('newPassword') === value) {
                      return Promise.resolve()
                    }
                    return Promise.reject(new Error('Şifreler eşleşmiyor.'))
                  },
                }),
              ]}
              style={{ marginTop: 16 }}
            >
              <Input.Password
                prefix={<LockOutlined style={{ color: '#9ca3af' }} />}
                placeholder="Şifrenizi tekrar girin"
                autoComplete="new-password"
              />
            </Form.Item>
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 12, marginTop: 12 }}>
              <Button
                onClick={() => {
                  setModalType('')
                  navigate('/login', { replace: true })
                }}
                style={{ minWidth: 60 }}
              >
                İptal
              </Button>
              <Button
                type="primary"
                htmlType="submit"
                loading={resetLoading}
                style={{ minWidth: 60, background: '#8B1A2B', borderColor: '#8B1A2B' }}
              >
                Şifreyi Güncelle
              </Button>
            </div>
          </Form>
        </div>
      </Modal>
    </div>
  )
}