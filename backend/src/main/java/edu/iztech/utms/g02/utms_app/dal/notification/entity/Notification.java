package edu.iztech.utms.g02.utms_app.dal.notification.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

// Kullanıcıya ait basit bildirim kaydı (UC-15). userId entity ilişkisi değil,
// düz bir referanstır — her kullanıcı yalnızca kendi userId'sine ait kayıtları görür.
@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private Integer userId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    // ddl-auto=update mevcut satırları NOT NULL'a çevirirken patlamasın diye default false.
    @Column(nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private boolean isRead = false;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
