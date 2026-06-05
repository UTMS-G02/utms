package edu.iztech.utms.g02.utms_app.bl.notification;

import edu.iztech.utms.g02.utms_app.api.notification.dto.NotificationResponse;
import edu.iztech.utms.g02.utms_app.dal.notification.entity.Notification;
import edu.iztech.utms.g02.utms_app.dal.notification.repository.NotificationRepository;
import edu.iztech.utms.g02.utms_app.dal.user.entity.User;
import edu.iztech.utms.g02.utms_app.dal.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// UC-15: Öğrenci/kullanıcı bildirim kutusu iş mantığı.
// Her kullanıcı yalnızca kendi bildirimleri üzerinde işlem yapabilir; sahiplik
// her mutasyonda kontrol edilir.
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    // JWT'den gelen e-posta üzerinden o anki kullanıcıyı çözer.
    private User currentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Kullanıcı bulunamadı."));
    }

    // Sistemin bir kullanıcıya bildirim üretmesi için yardımcı (örn. ÖİDB aksiyonları).
    // Çağıran zaten transaction içindeyse ona katılır.
    @Transactional
    public void create(Integer userId, String title, String message) {
        notificationRepository.save(Notification.builder()
                .userId(userId)
                .title(title)
                .message(message)
                .build());
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getMyNotifications() {
        Integer userId = currentUser().getUserId();
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(NotificationResponse::from)
                .toList();
    }

    @Transactional
    public NotificationResponse markAsRead(Integer id) {
        Notification notification = loadOwned(id);
        notification.setRead(true);
        return NotificationResponse.from(notificationRepository.save(notification));
    }

    @Transactional
    public void delete(Integer id) {
        Notification notification = loadOwned(id);
        notificationRepository.delete(notification);
    }

    // Bildirimi yükler ve o anki kullanıcıya ait olduğunu doğrular.
    private Notification loadOwned(Integer id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Bildirim bulunamadı."));
        if (!notification.getUserId().equals(currentUser().getUserId())) {
            throw new AccessDeniedException("Bu bildirim üzerinde işlem yapma yetkiniz bulunmuyor.");
        }
        return notification;
    }
}
