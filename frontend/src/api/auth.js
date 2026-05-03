import apiClient from './client'
import { delay } from './mock'

export const authApi = {
  login: (email, password) =>
    apiClient.post('/auth/login', { email, password }),

  verifyOtp: (email, otp) =>
    apiClient.post('/auth/verify-otp', { email, otp }),

  register: (payload) =>
    apiClient.post('/auth/register', payload),

  forgotPassword: (email) =>
    apiClient.post('/auth/forgot-password', { email }),

  resetPassword: (token, newPassword) =>
    apiClient.post('/auth/reset-password', { token, newPassword }),

  getMe: () =>
    apiClient.get('/auth/me'),

  // TODO: replace with real API call (PATCH /users/me/password)
  changePassword: async ({ currentPassword, newPassword }) => {
    await delay(500)
    if (currentPassword === 'wrong') {
      throw new Error('Mevcut şifre hatalı')
    }
    return { success: true, message: 'Şifre başarıyla güncellendi' }
  },
}