import { useState } from 'react'
import { Form, Input, Button, Typography, Tag, Descriptions, App } from 'antd'
import { useAuth } from '../../contexts/AuthContext'
import { authApi } from '../../api/auth'

const { Title, Text } = Typography

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
  cardSubtitle: {
    display: 'block',
    marginBottom: 24,
  },
}

export default function Profile() {
  const [form] = Form.useForm()
  const [loading, setLoading] = useState(false)
  const { user } = useAuth()
  const { message } = App.useApp()

  const displayName = user?.name
    ?? ([user?.firstName, user?.lastName].filter(Boolean).join(' ') || '—')

  const handleChangePassword = async ({ currentPassword, newPassword }) => {
    setLoading(true)
    try {
      await authApi.changePassword({ currentPassword, newPassword })
      message.success('Şifreniz başarıyla güncellendi.')
      form.resetFields()
    } catch (error) {
      message.error(error.message ?? 'Şifre güncellenirken bir hata oluştu.')
    } finally {
      setLoading(false)
    }
  }

  const infoItems = [
    {
      key: 'name',
      label: 'Ad Soyad',
      children: displayName,
    },
    {
      key: 'studentNumber',
      label: 'Öğrenci Numarası',
      children: user?.studentNumber ?? '—',
    },
    {
      key: 'email',
      label: 'E-posta',
      children: user?.email ?? '—',
    },
    {
      key: 'department',
      label: 'Bölüm',
      children: user?.department ?? '—',
    },
    {
      key: 'currentGpa',
      label: 'Genel Not Ortalaması',
      children: user?.currentGpa ?? '—',
    },
    {
      key: 'yksScore',
      label: 'YKS Puanı',
      children: user?.yksScore ?? '—',
    },
    {
      key: 'yksRanking',
      label: 'YKS Sıralaması',
      children: user?.yksRanking ?? '—',
    },
    {
      key: 'role',
      label: 'Rol',
      children: <Tag color="blue">{user?.role ?? '—'}</Tag>,
    },
  ]

  return (
    <div style={styles.page}>
      <div style={styles.pageHeader}>
        <Title level={2} style={{ margin: 0, color: '#1f212b', fontWeight: 700 }}>
          Profil
        </Title>
        <Text type="secondary" style={{ fontSize: 14 }}>
          Hesap bilgilerinizi görüntüleyin ve şifrenizi güncelleyin
        </Text>
      </div>

      {/* Kişisel Bilgiler */}
      <div style={styles.card}>
        <Title level={5} style={{ marginTop: 0, marginBottom: 4 }}>Kişisel Bilgiler</Title>
        <Text type="secondary" style={styles.cardSubtitle}>
          Bu bilgiler kurumsal kayıtlardan gelir ve düzenlenemez.
        </Text>
        <Descriptions
          column={2}
          layout="vertical"
          colon={false}
          items={infoItems}
        />
      </div>

      {/* Şifre Değiştir */}
      <div style={styles.card}>
        <Title level={5} style={{ marginTop: 0, marginBottom: 4 }}>Şifre Değiştir</Title>
        <Text type="secondary" style={styles.cardSubtitle}>
          Hesabınızın güvenliği için düzenli olarak şifrenizi güncellemenizi öneririz.
        </Text>
        <Form
          form={form}
          layout="vertical"
          onFinish={handleChangePassword}
          requiredMark={false}
          size="large"
          style={{ maxWidth: 480 }}
        >
          <Form.Item
            label="Mevcut Şifre"
            name="currentPassword"
            rules={[{ required: true, message: 'Mevcut şifre zorunludur.' }]}
          >
            <Input.Password placeholder="Mevcut şifrenizi girin" />
          </Form.Item>

          <Form.Item
            label="Yeni Şifre"
            name="newPassword"
            rules={[
              { required: true, message: 'Yeni şifre zorunludur.' },
              { min: 8, message: 'En az 8 karakter olmalıdır.' },
            ]}
            hasFeedback
          >
            <Input.Password placeholder="Yeni şifrenizi girin" />
          </Form.Item>

          <Form.Item
            label="Yeni Şifre (Tekrar)"
            name="confirmPassword"
            dependencies={['newPassword']}
            hasFeedback
            rules={[
              { required: true, message: 'Şifre tekrarı zorunludur.' },
              ({ getFieldValue }) => ({
                validator(_, value) {
                  if (!value || getFieldValue('newPassword') === value) {
                    return Promise.resolve()
                  }
                  return Promise.reject(new Error('Şifreler eşleşmiyor.'))
                },
              }),
            ]}
          >
            <Input.Password placeholder="Yeni şifrenizi tekrar girin" />
          </Form.Item>

          <Form.Item style={{ marginBottom: 0 }}>
            <Button
              type="primary"
              htmlType="submit"
              loading={loading}
              style={{ background: '#8B1A2B', borderColor: '#8B1A2B', fontWeight: 600 }}
            >
              Şifreyi Güncelle
            </Button>
          </Form.Item>
        </Form>
      </div>
    </div>
  )
}
