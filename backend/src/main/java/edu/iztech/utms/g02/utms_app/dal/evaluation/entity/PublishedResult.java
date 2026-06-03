package edu.iztech.utms.g02.utms_app.dal.evaluation.entity;

import edu.iztech.utms.g02.utms_app.dal.application.entity.Application;
import edu.iztech.utms.g02.utms_app.dal.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Yayınlanmış nihai sonuç. ÖİDB sonuçları yayınlayana kadar bu kayıt oluşmaz —
 * sonuçlar öğrenciye yalnızca burada bir satır olduğunda görünür.
 */
@Entity
@Table(name = "published_results")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublishedResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    // Nihai karar: "ACCEPTED" veya "REJECTED"
    @Column(nullable = false, length = 20)
    private String finalDecision;

    @CreationTimestamp
    private LocalDateTime publishedAt;

    @ManyToOne
    @JoinColumn(name = "published_by")
    private User publishedBy;
}
