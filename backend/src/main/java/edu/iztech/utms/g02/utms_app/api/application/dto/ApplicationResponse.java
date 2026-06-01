package edu.iztech.utms.g02.utms_app.api.application.dto;

import edu.iztech.utms.g02.utms_app.dal.application.entity.ApplicationStatus;
import lombok.*;
import java.time.LocalDateTime;



/*
// ApplicationResponse.java
// Başvuru bilgisi dışarıya gönderilirken kullanılan paket.

// - Application entity'sinin doğrudan dışarıya verilmesi güvenli değil (hassas alanlar olabilir)
// - Bu DTO sadece frontend'in görmesi gereken alanları içerir
// - Service, entity'yi bu nesneye dönüştürür
// - 
*/


@Data
@Builder
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationResponse {

    private Integer id;
    //private Integer studentId;
    private ApplicationStatus status;
    private String academicYear;
    private String targetFaculty;
    private String targetDepartment;

    // ÖİDB İnceleme Detayları
    private Boolean oidbApproved;
    private String oidbNotes;
    private Integer oidbReviewedBy;
    private LocalDateTime oidbReviewedDate;

    // YDYO İnceleme Detayları
    private Boolean ydyoApproved;
    private String ydyoNotes;
    private Integer ydyoReviewedBy;
    private LocalDateTime ydyoReviewedDate;

    //EKLENDI: YÖKSİS ve YKS verileri
    private String currentUniversity;
    private String currentFaculty;
    private String currentDepartment;
    private Double gpa;
    
    // Fakülte/Dekanlık kısımlarını da buraya ekleyebilirsiniz...
}