import React, { createContext, useContext, useState, useEffect, useCallback } from 'react'
import { message } from 'antd'
import apiClient from '../api/client'
import { authApi } from '../api/auth'

const AuthContext = createContext(null)

// Role constants – must match backend enum values
export const ROLES = {
  STUDENT: 'STUDENT',
  OIDB: 'OIDB',
  YDYO: 'YDYO',
  YGK: 'YGK',
  DEAN_OFFICE: 'DEAN_OFFICE',
  FACULTY_BOARD: 'FACULTY_BOARD',
}

// Role → default redirect path
export const ROLE_HOME = {
  [ROLES.STUDENT]: '/student/dashboard',
  [ROLES.OIDB]: '/oidb/dashboard',
  [ROLES.YDYO]: '/ydyo/dashboard',
  [ROLES.YGK]: '/ygk/dashboard',
  [ROLES.DEAN_OFFICE]: '/dean/dashboard',
  [ROLES.FACULTY_BOARD]: '/faculty-board/dashboard',
}

// Backend MeResponse (GET /auth/me) → frontend user nesnesi. tckn / dateOfBirth /
// phoneNumber yalnızca öğrenci hesaplarında dolu gelir; başvuru formu bunları
// salt-okunur olarak otomatik doldurmak için kullanır.
function mapMeToUser(m) {
  const parts = (m.fullName ?? '').trim().split(/\s+/)
  return {
    id: m.userId,
    name: m.fullName,
    firstName: parts[0] || '',
    middleName: m.middleName ?? '',
    lastName: parts.slice(1).join(' ') || '',
    email: m.email,
    role: m.role,
    tckn: m.tckn ?? '',
    dateOfBirth: m.dateOfBirth ?? null,
    phoneNumber: m.phoneNumber ?? '',
  }
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [token, setToken] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const storedToken = localStorage.getItem('utms_token')
    if (!storedToken) {
      setLoading(false)
      return
    }

    apiClient.defaults.headers.common['Authorization'] = `Bearer ${storedToken}`
    setToken(storedToken)

    authApi.getMe()
      .then((response) => {
        const userData = mapMeToUser(response.data)
        setUser(userData)
        localStorage.setItem('utms_user', JSON.stringify(userData))
      })
      .catch(() => {
        setToken(null)
        setUser(null)
        delete apiClient.defaults.headers.common['Authorization']
        localStorage.removeItem('utms_token')
        localStorage.removeItem('utms_user')
      })
      .finally(() => setLoading(false))
  }, [])

  const login = useCallback(async (email, password) => {
    try {
      // Önce gerçek API'ye istek atmayı dener
      const response = await apiClient.post('/auth/login', { email, password });
      const { token: jwt, userId, firstName, lastName, role } = response.data;

      setToken(jwt);
      apiClient.defaults.headers.common['Authorization'] = `Bearer ${jwt}`;
      localStorage.setItem('utms_token', jwt);

      // Login response'ta tckn/doğum tarihi/telefon yok; başvuru formunun bu
      // kayıt bilgilerini otomatik doldurabilmesi için /me ile zenginleştiriyoruz.
      let userData = {
        id: userId, userId, firstName, lastName,
        name: `${firstName} ${lastName}`.trim(), role,
      };
      try {
        const me = await authApi.getMe();
        userData = { ...mapMeToUser(me.data), firstName, lastName };
      } catch {
        // /me başarısız olursa temel bilgilerle devam et
      }

      setUser(userData);
      localStorage.setItem('utms_user', JSON.stringify(userData));
      return userData;

    } catch (error) {
      // Eğer backend yoksa veya sunucuya ulaşılamıyorsa (Mock Fallback - Geliştirme aşaması için)
        if (import.meta.env.DEV && (error.code === 'ERR_NETWORK' || !error.response || error.response.status >= 500)) {
        console.warn("Backend ulaşılamadı. Test (Mock) verisi ile giriş yapılıyor...");
        await new Promise((r) => setTimeout(r, 500));
        
        // Backend yokken rolü e-postadaki anahtar kelimeden türet — böylece
        // her panel test edilebilir (örn. ydyo@... → YDYO paneli).
        const lower = email.toLowerCase();
        const role =
          lower.includes('ydyo')    ? ROLES.YDYO :
          lower.includes('oidb')    ? ROLES.OIDB :
          lower.includes('ygk')     ? ROLES.YGK :
          lower.includes('faculty') ? ROLES.FACULTY_BOARD :
          lower.includes('admin') || lower.includes('dean') ? ROLES.DEAN_OFFICE_OFFICE :
          ROLES.STUDENT;
        const userData = { id: 1, name: email.split('@')[0].toUpperCase(), email, role };
        const jwt = 'mock-jwt-token-123';
        
        setToken(jwt);
        setUser(userData);
        localStorage.setItem('utms_token', jwt);
        localStorage.setItem('utms_user', JSON.stringify(userData));
        return userData;
      }
      
      throw error;
    }
  }, []);

  const verifyOtp = useCallback(async (email, otp) => {
    if (!apiClient) {
      await new Promise((r) => setTimeout(r, 300))
      const userData = { id: 1, name: email.split('@')[0], email, role: ROLES.STUDENT }
      const jwt = 'mock-jwt-otp'
      setToken(jwt)
      setUser(userData)
      localStorage.setItem('utms_token', jwt)
      localStorage.setItem('utms_user', JSON.stringify(userData))
      return userData
    }
    const response = await apiClient.post('/auth/verify-otp', { email, otp })
    const { token: jwt, user: userData } = response.data
    setToken(jwt)
    setUser(userData)
    apiClient.defaults.headers.common['Authorization'] = `Bearer ${jwt}`
    localStorage.setItem('utms_token', jwt)
    localStorage.setItem('utms_user', JSON.stringify(userData))
    return userData
  }, [])

  const logout = useCallback(() => {
    setToken(null)
    setUser(null)
    if (apiClient) delete apiClient.defaults.headers.common['Authorization']
    localStorage.removeItem('utms_token')
    localStorage.removeItem('utms_user')
    message.success('Başarıyla çıkış yapıldı.')
  }, [])

  const value = {
    user,
    token,
    loading,
    isAuthenticated: !!token,
    login,
    verifyOtp,
    logout,
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}

export default AuthProvider