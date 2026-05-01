// ─── TCKN ─────────────────────────────────────────────────────────────────────
export const validateTCKN = (tckn) => {
  if (!tckn || typeof tckn !== 'string') return false
  if (!/^\d{11}$/.test(tckn)) return false
  if (tckn[0] === '0') return false

  const digits = tckn.split('').map(Number)

  // Checksum algorithm
  const d10 =
    ((digits[0] + digits[2] + digits[4] + digits[6] + digits[8]) * 7 -
      (digits[1] + digits[3] + digits[5] + digits[7])) % 10
  if (d10 !== digits[9]) return false

  const sum11 = digits.slice(0, 10).reduce((a, b) => a + b, 0)
  if (sum11 % 10 !== digits[10]) return false

  return true
}

export const tcknRule = () => ({
  validator(_, value) {
    if (!value) return Promise.reject('TCKN zorunludur.')
    if (!validateTCKN(value))
      return Promise.reject('Geçerli bir 11 haneli TCKN giriniz.')
    return Promise.resolve()
  },
})

// ─── Email ────────────────────────────────────────────────────────────────────
export const validateInstitutionalEmail = (email) => {
  if (!email) return false
  return /^[^\s@]+@[^\s@]+\.edu\.tr$/i.test(email)
}

export const institutionalEmailRule = () => ({
  validator(_, value) {
    if (!value) return Promise.reject('E-posta adresi zorunludur.')
    if (!validateInstitutionalEmail(value))
      return Promise.reject('Kurumsal e-posta adresi giriniz (örn: isim@uni.edu.tr).')
    return Promise.resolve()
  },
})

// ─── Phone ────────────────────────────────────────────────────────────────────
export const validatePhone = (phone) => {
  if (!phone) return false
  return /^\d{10,11}$/.test(phone.replace(/\s/g, ''))
}

export const phoneRule = () => ({
  validator(_, value) {
    if (!value) return Promise.reject('Telefon numarası zorunludur.')
    if (!validatePhone(value))
      return Promise.reject('Geçerli bir telefon numarası giriniz (10-11 rakam).')
    return Promise.resolve()
  },
})

// ─── Password ─────────────────────────────────────────────────────────────────
export const getPasswordStrength = (password) => {
  if (!password) return { score: 0, label: '', color: '' }
  let score = 0
  if (password.length >= 8) score++
  if (/[A-Z]/.test(password)) score++
  if (/[a-z]/.test(password)) score++
  if (/\d/.test(password)) score++
  if (/[^A-Za-z0-9]/.test(password)) score++

  const map = [
    { score: 0, label: '', color: '' },
    { score: 1, label: 'Çok Zayıf', color: '#ff4d4f' },
    { score: 2, label: 'Zayıf', color: '#ff7a45' },
    { score: 3, label: 'Orta', color: '#faad14' },
    { score: 4, label: 'İyi', color: '#52c41a' },
    { score: 5, label: 'Güçlü', color: '#389e0d' },
  ]
  return map[score]
}

export const passwordRule = () => ({
  validator(_, value) {
    if (!value) return Promise.reject('Şifre zorunludur.')
    if (value.length < 8) return Promise.reject('Şifre en az 8 karakter olmalıdır.')
    if (!/[A-Z]/.test(value)) return Promise.reject('En az bir büyük harf içermelidir.')
    if (!/[a-z]/.test(value)) return Promise.reject('En az bir küçük harf içermelidir.')
    if (!/\d/.test(value)) return Promise.reject('En az bir rakam içermelidir.')
    if (!/[^A-Za-z0-9]/.test(value))
      return Promise.reject('En az bir özel karakter içermelidir (örn: !@#$).')
    return Promise.resolve()
  },
})

// ─── Date of Birth ────────────────────────────────────────────────────────────
export const dobRule = () => ({
  validator(_, value) {
    if (!value) return Promise.reject('Doğum tarihi zorunludur.')
    if (value.isAfter(new Date()))
      return Promise.reject('Doğum tarihi gelecekte olamaz.')
    return Promise.resolve()
  },
})

// ─── File Upload ──────────────────────────────────────────────────────────────
const MAX_FILE_SIZE_MB = 5

export const validateUploadFile = (file) => {
  const isPdf = file.type === 'application/pdf' || file.name.endsWith('.pdf')
  if (!isPdf) return { valid: false, error: 'Yalnızca PDF dosyası yükleyebilirsiniz.' }

  const sizeMb = file.size / 1024 / 1024
  if (sizeMb > MAX_FILE_SIZE_MB)
    return { valid: false, error: `Dosya boyutu ${MAX_FILE_SIZE_MB}MB'ı geçemez.` }

  return { valid: true, error: null }
}