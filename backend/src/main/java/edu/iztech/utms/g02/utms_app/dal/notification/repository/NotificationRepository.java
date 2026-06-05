package edu.iztech.utms.g02.utms_app.dal.notification.repository;

import edu.iztech.utms.g02.utms_app.dal.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Integer> {

    // En yeni bildirim üstte — frontend gruplamadan önce sıralı liste alır.
    List<Notification> findByUserIdOrderByCreatedAtDesc(Integer userId);

    // Seed idempotency: kullanıcının zaten bildirimi var mı?
    boolean existsByUserId(Integer userId);
}
