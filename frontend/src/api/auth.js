import apiClient from './client'

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

  activateAccount: (token) =>
    apiClient.get(`/auth/activate?token=${token}`),

  getMe: () =>
    apiClient.get('/auth/me'),

  // Giriş yapmış kullanıcının şifresini değiştirir (mevcut şifre doğrulanır).
  changePassword: ({ currentPassword, newPassword }) =>
    apiClient.patch('/auth/change-password', { currentPassword, newPassword }),
}