package edu.iztech.utms.g02.utms_app.dal.evaluation.entity;

import edu.iztech.utms.g02.utms_app.dal.application.entity.Application;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Komisyon kararı — Dekanlık / Fakülte Kurulu kararlarının kaydı.
 *
 * <p>EKLE-ONLY (append-only): bir başvuru için birden fazla karar satırı olabilir.
 * Mevcut satırlar güncellenmez; her yeni karar yeni bir satır olarak eklenir.
 */
@Entity
@Table(name = "committee_decisions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommitteeDecision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    // Kararı veren makam: "DEAN" veya "FACULTY_BOARD"
    @Column(nullable = false)
    private String decisionBy;

    // Karar: "APPROVED" veya "REJECTED"
    @Column(nullable = false)
    private String decision;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    private LocalDateTime decidedAt;
}
