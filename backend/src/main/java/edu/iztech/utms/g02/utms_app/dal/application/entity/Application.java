package edu.iztech.utms.g02.utms_app.dal.application.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;


import java.util.List;
import java.util.ArrayList;

// bunlar eklenecek mi??????????????

//    @ManyToOne @JoinColumn(name = "student_id", nullable = false) private Student student;

//    private Boolean ydyoApproved;

@Entity
@Table(name = "applications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Application { // abstract mı olacak ??

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
    private Boolean ydyoApproved;

    @Column(nullable = false)
    private Boolean oidbApproved;

    @Column(nullable = false)
    private Long OidbReviewedBy;

    @Column(nullable = false)
    private LocalDateTime OidbReviewedDate;

    
    @Column(nullable = false)
    private LocalDateTime YdyoReviewedDate;

    @Column(nullable = false)
    private Long YdyoReviewedBy;

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