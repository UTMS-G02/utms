package edu.iztech.utms.g02.utms_app.api.notification.controller;

import edu.iztech.utms.g02.utms_app.api.notification.dto.NotificationResponse;
import edu.iztech.utms.g02.utms_app.bl.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// UC-15: Bildirim kutusu uçları. Tümü authenticated; her kullanıcı yalnızca
// kendi bildirimlerini görür/değiştirir/silebilir (sahiplik Service'te kontrol edilir).
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    // GET /api/notifications/me — giriş yapan kullanıcının bildirimleri
    @GetMapping("/me")
    public ResponseEntity<List<NotificationResponse>> getMyNotifications() {
        return ResponseEntity.ok(notificationService.getMyNotifications());
    }

    // PATCH /api/notifications/{id}/read — okundu olarak işaretle
    @PatchMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markAsRead(@PathVariable Integer id) {
        return ResponseEntity.ok(notificationService.markAsRead(id));
    }

    // DELETE /api/notifications/{id} — bildirimi kalıcı olarak sil
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(@PathVariable Integer id) {
        notificationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
