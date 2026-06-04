package edu.iztech.utms.g02.utms_app.dal.evaluation.entity;

import edu.iztech.utms.g02.utms_app.dal.application.entity.Application;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "course_equivalencies")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CourseEquivalency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    // Kaynak üniversite dersi — serbest metin (dış üniversite kodları doğrulanamaz)
    @Column(nullable = false, length = 100)
    private String sourceCode;

    @Column(nullable = false, length = 200)
    private String sourceName;

    @Column(nullable = false)
    private Integer sourceCredit;

    // Hedef İYTE dersi
    @Column(nullable = false, length = 100)
    private String targetCode;

    @Column(nullable = false, length = 200)
    private String targetName;

    @Column(nullable = false)
    private Integer targetCredit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EquivalencyStatus equivalencyStatus;

    // Tablodaki satır sırasını korur
    @Column(nullable = false)
    private Integer rowOrder;
}
