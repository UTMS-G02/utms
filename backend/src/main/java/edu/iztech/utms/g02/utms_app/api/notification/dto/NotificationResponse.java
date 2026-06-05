package edu.iztech.utms.g02.utms_app.api.notification.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import edu.iztech.utms.g02.utms_app.dal.notification.entity.Notification;
import lombok.*;

import java.time.LocalDateTime;

// Bildirim dışarıya gönderilirken kullanılan paket (userId sızdırılmaz).
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

    private Integer id;
    private String title;
    private String message;

    // JSON anahtarı kesin "isRead" olsun (Lombok boolean adlandırma sürprizini önler).
    @JsonProperty("isRead")
    private boolean isRead;

    private LocalDateTime createdAt;

    public static NotificationResponse from(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .title(n.getTitle())
                .message(n.getMessage())
                .isRead(n.isRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
