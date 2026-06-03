import { useEffect } from 'react'
import { useSearchParams, useNavigate } from 'react-router-dom'
import { Spin, Typography } from 'antd'
import { authApi } from '../../api/auth'
import iyteLogo from '../../assets/iyte_logo.png'

const { Text } = Typography

export default function ActivatePage() {
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()

  useEffect(() => {
    const token = searchParams.get('token')

    if (!token) {
      navigate('/login?activationError=true', { replace: true })
      return
    }

    authApi.activateAccount(token)
      .then(() => navigate('/login?activated=true', { replace: true }))
      .catch(() => navigate('/login?activationError=true', { replace: true }))
  }, [])

  return (
    <div style={{
      minHeight: '100vh',
      background: '#f2f2f7',
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
      justifyContent: 'center',
      gap: 24,
    }}>
      <img src={iyteLogo} alt="IYTE Logo" style={{ height: 64 }} />
      <Spin size="large" />
      <Text>Hesabınız aktive ediliyor...</Text>
    </div>
  )
}