package edu.iztech.utms.g02.utms_app.dal.application.entity;

import edu.iztech.utms.g02.utms_app.dal.user.entity.Staff;
import edu.iztech.utms.g02.utms_app.dal.user.entity.Student;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

@Entity
@Table(name = "applications", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "academic_year", "semester"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer applicationId;

    // Her başvuru bir öğrenciye ait — başvuru oluşturulurken zorunlu
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private Student student;

    // Başvuru oluşturulurken zorunlu alanlar
    @Column(nullable = false)
    private String targetDept;

    @Column(nullable = false)
    private String targetFaculty;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ApplicationStatus status = ApplicationStatus.DRAFT;

    @Column(nullable = false)
    private String academicYear;

    @Column(nullable = false)
    private String semester;

    @Column(nullable = false)
    private Double sayYksScore;

    @Column(nullable = false)
    private Integer sayYksRank;

    @Column(nullable = false)
    private String currentUniversity;

    @Column(nullable = false)
    private String currentFaculty;

    @Column(nullable = false)
    private String currentDepartment;

    @Column(nullable = false)
    private Double gpa;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Süreç ilerledikçe dolacak alanlar — başlangıçta boş olabilir
    private LocalDateTime updatedAt;

    private LocalDate submissionDate;

    private LocalDate acceptedDate;

    // ÖİDB incelemesi — ancak ÖİDB incelediğinde dolar
    private String oidbNotes;

    private Boolean oidbApproved;

    @ManyToOne
    @JoinColumn(name = "oidb_reviewed_by")
    private Staff oidbReviewedBy;

    private LocalDateTime oidbReviewedDate;

    // YDYO incelemesi — ancak YDYO incelediğinde dolar
    private String ydyoNotes;

    private Boolean ydyoApproved;

    @ManyToOne
    @JoinColumn(name = "ydyo_reviewed_by")
    private Staff ydyoReviewedBy;

    private LocalDateTime ydyoReviewedDate;

    // YGK incelemesi — ancak YGK incelediğinde dolar
    private String ygkNotes;

    private Boolean ygkApproved;

    @ManyToOne
    @JoinColumn(name = "ygk_reviewed_by")
    private Staff ygkReviewedBy;

    private LocalDateTime ygkReviewedDate;

    // 0.10 × GPA + 0.90 × YKS — YGK aşamasında hesaplanıp saklanır, read-only
    private Double compositeScore;

    // Dekanlık ve Fakülte Kurulu — sürecin sonlarında dolar
    private String deanOfficeNotes;

    private String facultyNotes;

    private Boolean facultyBoardApproved;

    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Document> documents = new ArrayList<>();
}