import { useState } from 'react'
import { useNavigate, Link, useLocation } from 'react-router-dom'
import { Form, Input, Button, Divider, Typography, App } from 'antd'
import { MailOutlined, LockOutlined, ArrowRightOutlined } from '@ant-design/icons'
import { useAuth, ROLE_HOME } from '../../contexts/AuthContext'
import iyteLogo from '../../assets/iyte_logo.png'

const { Title, Text } = Typography

// ─── Styles (scoped inline to avoid global pollution) ─────────────────────────
const styles = {
  page: {
    minHeight: '100vh',
    background: '#fafafa',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    fontFamily: "'DM Sans', sans-serif",
    position: 'relative',
    overflow: 'hidden',
  },
  decorCircle1: {
    position: 'absolute',
    top: -120,
    right: -120,
    width: 400,
    height: 400,
    borderRadius: '50%',
    background: 'radial-gradient(circle, rgba(139,26,43,0.06) 0%, transparent 70%)',
    pointerEvents: 'none',
  },
  decorCircle2: {
    position: 'absolute',
    bottom: -100,
    left: -100,
    width: 360,
    height: 360,
    borderRadius: '50%',
    background: 'radial-gradient(circle, rgba(139,26,43,0.04) 0%, transparent 70%)',
    pointerEvents: 'none',
  },
  card: {
    background: '#ffffff',
    borderRadius: 16,
    padding: '48px 44px',
    width: '100%',
    maxWidth: 440,
    boxShadow: '0 4px 32px rgba(0,0,0,0.08), 0 1px 4px rgba(0,0,0,0.04)',
    position: 'relative',
    zIndex: 1,
  },
  logoWrap: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    marginBottom: 32,
    gap: 16,
  },
  logoContainer: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
  },
}

export default function LoginPage() {
  const [form] = Form.useForm()
  const [loading, setLoading] = useState(false)
  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const { message } = App.useApp()

  const from = location.state?.from?.pathname ?? null

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
      <div style={styles.decorCircle1} />
      <div style={styles.decorCircle2} />

      <div style={styles.card}>
        {/* Logo + title */}
        <div style={styles.logoWrap}>
          <div style={styles.logoContainer}>
            <img
              src={iyteLogo}
              alt="İYTE logo"
              style={{ width: 100, height: 100, objectFit: 'contain' }}
            />
          </div>
          <div style={{ textAlign: 'center' }}>
            <Title level={4} style={{ margin: 0, color: '#1a1a2e', fontWeight: 700, letterSpacing: '-0.02em' }}>
              Yatay Geçiş Başvuru Portalı
            </Title>
            <Text type="secondary" style={{ fontSize: 16 }}>
              İzmir Yüksek Teknoloji Enstitüsü
            </Text>
          </div>
        </div>

        {/* Form */}
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
            style={{ marginBottom: 8 }}
          >
            <Input.Password
              prefix={<LockOutlined style={{ color: '#9ca3af' }} />}
              placeholder="••••••••"
              autoComplete="current-password"
            />
          </Form.Item>

          {/* Forgot password */}
          <div style={{ textAlign: 'right', marginBottom: 24 }}>
            <Link
              to="/forgot-password"
              style={{ color: '#8B1A2B', fontSize: 13, fontWeight: 500 }}
            >
              Şifremi Unuttum
            </Link>
          </div>

          <Form.Item style={{ marginBottom: 16 }}>
            <Button
              type="primary"
              htmlType="submit"
              loading={loading}
              block
              icon={!loading && <ArrowRightOutlined />}
              iconPosition="end"
              style={{
                background: '#8B1A2B',
                borderColor: '#8B1A2B',
                height: 46,
                fontWeight: 600,
                fontSize: 15,
                letterSpacing: '0.01em',
              }}
            >
              Giriş Yap
            </Button>
          </Form.Item>
        </Form>

        <Divider style={{ margin: '0 0 16px', borderColor: '#f0f0f0' }} />

        <div style={{ textAlign: 'center' }}>
          <Text type="secondary" style={{ fontSize: 13 }}>
            Hesabınız yok mu?{' '}
          </Text>
          <Link
            to="/register"
            style={{ color: '#8B1A2B', fontWeight: 600, fontSize: 13 }}
          >
            Kayıt Ol
          </Link>
        </div>
      </div>
    </div>
  )
}