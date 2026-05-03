package edu.iztech.utms.g02.utms_app.dal.application.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;


@Entity
@Builder
@Table(name = "documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Document { // abstract mı olacak 

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer documentId;

    @Column(nullable = false, unique = true)
    private Integer applicationId;

    @Column(nullable = false)
    private String documentType;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String filePath;

    @Column(nullable = false)
    private boolean ydyoApproved;

    @Column(nullable = false)
    private LocalDate documentUploadDate;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;
}