import React, { createContext, useContext, useState, useEffect, useCallback } from 'react'
import { message } from 'antd'
import apiClient from '../api/client'

const AuthContext = createContext(null)

// Role constants – must match backend enum values
export const ROLES = {
  STUDENT: 'STUDENT',
  OIDB: 'OIDB',
  YDYO: 'YDYO',
  YGK: 'YGK',
  DEAN: 'DEAN',
  FACULTY_BOARD: 'FACULTY_BOARD',
}

// Role → default redirect path
export const ROLE_HOME = {
  [ROLES.STUDENT]: '/student/dashboard',
  [ROLES.OIDB]: '/oidb/dashboard',
  [ROLES.YDYO]: '/ydyo/dashboard',
  [ROLES.YGK]: '/ygk/dashboard',
  [ROLES.DEAN]: '/dean/dashboard',
  [ROLES.FACULTY_BOARD]: '/faculty-board/dashboard',
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [token, setToken] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const storedToken = localStorage.getItem('utms_token')
    const storedUser = localStorage.getItem('utms_user')
    if (storedToken && storedUser) {
      try {
        const parsedUser = JSON.parse(storedUser)
        setToken(storedToken)
        setUser(parsedUser)
        apiClient.defaults.headers.common['Authorization'] = `Bearer ${storedToken}`
      } catch {
        localStorage.removeItem('utms_token')
        localStorage.removeItem('utms_user')
      }
    }
    setLoading(false)
  }, [])

  const login = useCallback(async (email, password) => {
    try {
      // Önce gerçek API'ye istek atmayı dener
      const response = await apiClient.post('/auth/login', { email, password });
      const { token: jwt, user: userData } = response.data;
      
      setToken(jwt);
      setUser(userData);
      apiClient.defaults.headers.common['Authorization'] = `Bearer ${jwt}`;
      localStorage.setItem('utms_token', jwt);
      localStorage.setItem('utms_user', JSON.stringify(userData));
      return userData;

    } catch (error) {
      // Eğer backend yoksa veya sunucuya ulaşılamıyorsa (Mock Fallback - Geliştirme aşaması için)
      if (error.code === 'ERR_NETWORK' || !error.response) {
        console.warn("Backend ulaşılamadı. Test (Mock) verisi ile giriş yapılıyor...");
        await new Promise((r) => setTimeout(r, 500));
        
        const role = email.includes('admin') ? ROLES.DEAN : ROLES.STUDENT;
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