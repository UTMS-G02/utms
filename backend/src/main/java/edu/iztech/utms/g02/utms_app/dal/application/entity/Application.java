package edu.iztech.utms.g02.utms_app.dal.application.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Builder;
import java.util.List;
import java.util.ArrayList;

// bunlar eklenecek mi??????????????

//    @ManyToOne @JoinColumn(name = "student_id", nullable = false) private Student student;

//    private Boolean ydyoApproved;






@Entity
@Builder
@Table(name = "applications")
@Getter
@Setter
@NoArgsConstructor
public abstract class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer applicationId;

    @Column(nullable = false, unique = true)
    private String studentId;

    @Column(nullable = false)
    private String targetDept;

    @Column(nullable = false)
    private String targetFaculty;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ApplicationStatus status = ApplicationStatus.DRAFT;

    @Column(nullable = false)
    private String oidbNotes;

    @Column(nullable = false)
    private String ydyoNotes;

    @Column(nullable = false)
    private String facultyNotes;

    @Column(nullable = false)
    private String deanOfficeNotes;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDate submissionDate;

    @Column(nullable = false)
    private LocalDate acceptedDate;

    @Column(nullable = false)
    private String academicYear;

    @Column(nullable = false)
    private String semester;

    @Column(nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Document> documents = new ArrayList<>();

}