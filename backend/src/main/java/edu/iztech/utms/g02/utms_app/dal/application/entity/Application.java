package edu.iztech.utms.g02.utms_app.dal.application.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "applications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String studentId;

    @Column(nullable = false)
    private String targetDept;

    @Column(nullable = false)
    private String targetFaculty;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ApplicationStatus status = ApplicationStatus.DRAFT;

    // ÖİDB inceleme alanları
    private Boolean oidbApproved;
    private String oidbNotes;
    private Long oidbReviewedBy;
    private LocalDateTime oidbReviewedDate;

    // YDYO inceleme alanları
    private Boolean ydyoApproved;
    private String ydyoNotes;
    private Long ydyoReviewedBy;
    private LocalDateTime ydyoReviewedDate;

    // Diğer notlar
    private String facultyNotes;
    private String deanOfficeNotes;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private LocalDate submissionDate;
    private LocalDate acceptedDate;

    @Column(nullable = false)
    private String academicYear;

    private String semester;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;
}