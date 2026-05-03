package edu.iztech.utms.g02.utms_app.dal.application.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import lombok.Builder;

@Entity
@Builder
@Table(name = "documents")
@Getter
@Setter
@NoArgsConstructor
public abstract class Document {

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