package edu.iztech.utms.g02.utms_app.dal.evaluation.entity;

import edu.iztech.utms.g02.utms_app.dal.application.entity.Application;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * YGK skorlaması sonucunda her başvuru için üretilen değerlendirme kaydı.
 * Başvuru başına en fazla bir satır (application_id UNIQUE).
 */
@Entity
@Table(name = "evaluation_results")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvaluationResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne
    @JoinColumn(name = "application_id", nullable = false, unique = true)
    private Application application;

    // 0.10 × GPA + 0.90 × YKS — ScoreCalculator ile hesaplanır
    @Column(nullable = false)
    private Double compositeScore;

    // En yüksek skordan en düşüğe doğru sıralama (1 = en yüksek)
    private Integer ranking;

    @CreationTimestamp
    private LocalDateTime calculatedAt;
}
