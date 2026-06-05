import apiClient from './client'

// ─── Bildirim uçları (UC-15, backend hazır — her zaman gerçek) ───────────────
// İnce modül: applicationsApi tarzı. Her kullanıcı yalnızca kendi bildirimlerini
// görür/değiştirir; sahiplik kontrolü backend'de yapılır.

export const notificationsApi = {
  // GET /notifications/me → bildirim listesi (en yeni üstte)
  getMyNotifications: () =>
    apiClient.get('/notifications/me').then((res) => res.data),

  // PATCH /notifications/{id}/read → okundu işaretle, güncel kaydı döner
  markAsRead: (id) =>
    apiClient.patch(`/notifications/${id}/read`).then((res) => res.data),

  // DELETE /notifications/{id} → bildirimi sil
  deleteNotification: (id) =>
    apiClient.delete(`/notifications/${id}`).then((res) => res.data),
}
