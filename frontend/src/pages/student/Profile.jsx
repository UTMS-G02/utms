import { useState, useEffect } from 'react'
import { Form, Input, Button, Typography, Tag, Descriptions, App } from 'antd'
import { useAuth } from '../../contexts/AuthContext'
import { authApi } from '../../api/auth'
import { applicationsApi } from '../../api/applications'

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
  const [academic, setAcademic] = useState(null)
  const { user } = useAuth()
  const { message } = App.useApp()

  // Akademik bilgiler (bölüm, GNO, YKS) YÖKSİS/ÖSYM'den (mock) gelir; başvuru
  // formuyla AYNI kaynak (GET /api/yoksis/me) kullanılır, böylece tutarlı kalır.
  useEffect(() => {
    let active = true
    applicationsApi
      .getMyYoksisData()
      .then((data) => { if (active) setAcademic(data) })
      .catch(() => { /* sessiz geç: alanlar '—' kalır */ })
    return () => { active = false }
  }, [])

  // Ad + (varsa) ikinci ad + soyad — ikinci ad profilde de görünsün diye parçalardan kurulur.
  // (user.name / fullName ikinci adı içermeyebilir, bu yüzden önce parçalar denenir.)
  const displayName = [user?.firstName, user?.middleName, user?.lastName]
    .filter(Boolean).join(' ') || user?.name || '—'

  const handleChangePassword = async ({ currentPassword, newPassword }) => {
    setLoading(true)
    try {
      await authApi.changePassword({ currentPassword, newPassword })
      message.success('Şifreniz başarıyla güncellendi.')
      form.resetFields()
    } catch (error) {
      // Backend iş kuralı hatalarını { message } olarak döndürür (örn. "Mevcut şifreniz hatalı.").
      message.error(
        error?.response?.data?.message ?? 'Şifre güncellenirken bir hata oluştu.'
      )
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
      key: 'email',
      label: 'E-posta',
      children: user?.email ?? '—',
    },
    {
      key: 'university',
      label: 'Kayıtlı Olduğu Üniversite',
      children: academic?.currentUniversity ?? '—',
    },
    {
      key: 'faculty',
      label: 'Fakülte',
      children: academic?.currentFaculty ?? '—',
    },
    {
      key: 'department',
      label: 'Bölüm',
      children: academic?.currentDepartment ?? '—',
    },
    {
      key: 'currentGpa',
      label: 'Genel Not Ortalaması',
      children: academic?.gpa != null ? Number(academic.gpa).toFixed(2) : '—',
    },
    {
      key: 'yksScore',
      label: 'YKS Puanı',
      children: academic?.yksScore != null ? Number(academic.yksScore).toFixed(3) : '—',
    },
    {
      key: 'yksRanking',
      label: 'YKS Sıralaması',
      children: academic?.yksRank != null ? Number(academic.yksRank).toLocaleString('tr-TR') : '—',
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
