package edu.iztech.utms.g02.utms_app.dal.application.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "application_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Doğrudan Application entity'sine ManyToOne ilişki de kurulabilir, 
    // ancak Notification sınıfında yaptığınız gibi düz ID referansı kullanmak 
    // loose coupling (gevşek bağlılık) açısından gayet sağlıklıdır.
    @Column(nullable = false)
    private Integer applicationId;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime changedAt = LocalDateTime.now();

    // Opsiyonel: Durum değişirken eklenen ekstra notlar 
    // (Örn: "Öğrenci belgesi eksik olduğu için YDYO'ya gönderilemedi")
    @Column(columnDefinition = "TEXT")
    private String note;
    
    // Opsiyonel: Bu statüyü kim değiştirdi? (İleride loglama için çok işinize yarar)
    @Column(name = "changed_by_user_id")
    private Integer changedByUserId;
}
