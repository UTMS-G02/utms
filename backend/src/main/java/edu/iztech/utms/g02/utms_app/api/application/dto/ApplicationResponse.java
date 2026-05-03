package edu.iztech.utms.g02.utms_app.api.application.dto;

import edu.iztech.utms.g02.utms_app.dal.application.entity.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Data
@Builder
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationResponse {

    private Long id;
    private String studentId;
    private ApplicationStatus status;
    private String academicYear;
    private String targetFaculty;
    private String targetDept;

    // ÖİDB İnceleme Detayları
    private Boolean oidbApproved;
    private String oidbNotes;
    private Long oidbReviewedBy;
    private LocalDateTime oidbReviewedDate;

    // YDYO İnceleme Detayları
    private Boolean ydyoApproved;
    private String ydyoNotes;
    private Long ydyoReviewedBy;
    private LocalDateTime ydyoReviewedDate;
    
    // Fakülte/Dekanlık kısımlarını da buraya ekleyebilirsiniz...
}