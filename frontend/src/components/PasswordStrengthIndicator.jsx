import { Space } from 'antd'
import { CheckCircleFilled, MinusCircleFilled } from '@ant-design/icons'

const RULES = [
  { label: 'En az 8 karakter', test: (v) => v.length >= 8 },
  { label: 'En az 1 büyük harf (A-Z)', test: (v) => /[A-Z]/.test(v) },
  { label: 'En az 1 küçük harf (a-z)', test: (v) => /[a-z]/.test(v) },
  { label: 'En az 1 rakam (0-9)', test: (v) => /[0-9]/.test(v) },
]

export default function PasswordStrengthIndicator({ value }) {
  if (!value) return null

  const results = RULES.map((r) => ({ label: r.label, passed: r.test(value) }))
  const allPassed = results.every((r) => r.passed)

  return (
    <Space direction="vertical" size={4} style={{ marginTop: 4 }}>
      {results.map((r) => (
        <span
          key={r.label}
          style={{ fontSize: 12, color: r.passed ? '#1f7a3f' : '#666', display: 'flex', alignItems: 'center', gap: 6 }}
        >
          {r.passed
            ? <CheckCircleFilled style={{ color: '#52c41a' }} />
            : <MinusCircleFilled style={{ color: '#d9d9d9' }} />}
          {r.label}
        </span>
      ))}
      {allPassed && (
        <span style={{ fontSize: 12, color: '#1f7a3f', fontWeight: 600 }}>
          Şifre güvenli ✓
        </span>
      )}
    </Space>
  )
}
