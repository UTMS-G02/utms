package edu.iztech.utms.g02.utms_app.dal.audit.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Denetim (audit) kaydı — kim, ne zaman, hangi başvuruda, ne yaptı.
 *
 * <p>Kalıcı iz: kritik aksiyonlar (komisyon kararı, dekan iletimleri, YGK skorlama/değerlendirme,
 * ÖİDB yayınlama) bu tabloya tek satır olarak yazılır. {@code log.info} geçici loglarının aksine
 * sorgulanabilir/denetlenebilir kalıcı kayıttır (UC-6/8/10/11 POST-6).
 */
@Entity
@Table(name = "audit_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** Kim — oturum açan kullanıcının e-postası; bağlam yoksa "system". */
    @Column(nullable = false)
    private String actor;

    /** Ne — sabit aksiyon kodu (ör. FACULTY_BOARD_DECISION, DEAN_FORWARD, RESULTS_PUBLISHED). */
    @Column(nullable = false, length = 60)
    private String action;

    /** Hangi başvuru — toplu aksiyonlarda (ör. publish) null olabilir. */
    private Integer applicationId;

    /** Serbest metin detay (karar tipi, statü geçişi, sayı vb.). */
    @Column(columnDefinition = "TEXT")
    private String details;

    @CreationTimestamp
    private LocalDateTime timestamp;
}
