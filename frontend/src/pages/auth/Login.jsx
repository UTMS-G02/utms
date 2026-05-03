import { useState, useEffect } from 'react'
import { useNavigate, useLocation } from 'react-router-dom'
import { Form, Input, Button, Typography, App, Modal, Checkbox } from 'antd'
import { MailOutlined, LockOutlined } from '@ant-design/icons'
import { useAuth, ROLE_HOME } from '../../contexts/AuthContext'
import iyteLogo from '../../assets/iyte_logo.png'

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
  const [loading, setLoading] = useState(false)
  const [modalType, setModalType] = useState(initialModal || '')
  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const { message } = App.useApp()

  useEffect(() => {
    if (initialModal) {
      setModalType(initialModal)
    }
  }, [initialModal])

  const from = location.state?.from?.pathname ?? null

  const closeModal = () => {
    setModalType('')
    if (location.pathname !== '/login') {
      navigate('/login', { replace: true })
    }
  }

  const handleRegister = async (values) => {
    await new Promise((r) => setTimeout(r, 1000))
    message.success('Kayıt başarılı. Giriş sayfasına yönlendiriliyorsunuz.')
    registerForm.resetFields()
    closeModal()
  }

  const handleForgotPassword = async ({ email }) => {
    await new Promise((r) => setTimeout(r, 1000))
    message.success('Şifre sıfırlama bağlantısı e-posta adresinize gönderildi.')
    forgotForm.resetFields()
    closeModal()
  }

  const handleSubmit = async ({ email, password }) => {
    setLoading(true)
    try {
      const user = await login(email, password)
      message.success(`Hoş geldiniz, ${user.name}!`)
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

      <Modal
        open={modalType === 'register'}
        title="Hesap Oluştur"
        onCancel={closeModal}
        footer={null}
        centered
        width={500}
      >
        <div style={styles.modalContent}>
          <Text type="secondary" style={{ display: 'block' }}>
            Yatay geçiş portalına erişmek için kayıt olun
          </Text>

          <Form
            form={registerForm}
            layout="vertical"
            onFinish={handleRegister}
            requiredMark={false}
            size="large"
          >
            <Form.Item
              label="E-posta Adresi *"
              name="email"
              rules={[
                { required: true, message: 'E-posta adresi zorunludur.' },
                { type: 'email', message: 'Geçerli bir e-posta adresi giriniz.' },
              ]}
            >
              <Input placeholder="eposta@ogrenci.edu.tr" autoComplete="email" />
            </Form.Item>

            <Form.Item
              label="Şifre *"
              name="password"
              rules={[
                { required: true, message: 'Şifre zorunludur.' },
                { min: 8, message: 'Şifre en az 8 karakter olmalıdır.' },
              ]}
              hasFeedback
            >
              <Input.Password placeholder="Şifre oluşturun" autoComplete="new-password" />
            </Form.Item>

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
              <Button type="primary" htmlType="submit" style={{ minWidth: 60, background: '#8B1A2B', borderColor: '#8B1A2B' }}>
                Kayıt Ol
              </Button>
            </div>
          </Form>
        </div>
      </Modal>

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
              <Button type="primary" htmlType="submit" style={{ minWidth: 60, background: '#8B1A2B', borderColor: '#8B1A2B' }}>
                Sıfırlama Bağlantısı Gönder
              </Button>
            </div>
          </Form>
        </div>
      </Modal>
    </div>
  )
}
