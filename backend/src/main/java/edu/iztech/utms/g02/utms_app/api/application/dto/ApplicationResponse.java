package edu.iztech.utms.g02.utms_app.api.application.dto;

import edu.iztech.utms.g02.utms_app.dal.application.entity.ApplicationStatus;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;



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
    private String semester;          // hedef yarıyıl (3/5) — başvuru takibi detayında gösterilir
    private String targetFaculty;
    private String targetDepartment;

    // Öğrenci kimlik/iletişim bilgileri (YDYO değerlendirme paneli bunlara dayanır)
    private String studentName;
    private String tckn;
    private String email;
    private String phoneNumber;
    private LocalDate dateOfBirth;    // başvuru takibi detayında kişisel bilgiler için
    private LocalDate submissionDate;

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

    // YDYO sınav alanları
    //  - requiresExam: entity'de saklanmaz; ydyoApproved'tan türetilir (onaylı=muaf→sınav yok)
    //  - examScore: ydyoExamScore
    //  - examPassed: statüden türetilir (REJECTED→false, sınavla ACCEPTED→true)
    private Boolean requiresExam;
    private Double examScore;
    private Boolean examPassed;

    // YDYO kararı kesinleştikten sonra değiştirildiyse → "değişiklik yapılmıştır" işareti.
    // Frontend bu alanları (modified/modifiedBy/modifiedAt) doğrudan eşler.
    private boolean modified;
    private Integer modifiedBy;
    private LocalDateTime modifiedAt;

    //EKLENDI: YÖKSİS ve YKS verileri
    private String currentUniversity;
    private String currentFaculty;
    private String currentDepartment;
    private Double gpa;
    private Double sayYksScore;       // ÖSYM (mock) SAY YKS puanı
    private Integer sayYksRank;       // ÖSYM (mock) SAY YKS sıralaması

    // Belgeler — YDYO detay ekranı için hafif özet (indirme linki documentId ile kurulur)
    private List<DocumentSummary> documents;

    // Fakülte/Dekanlık kısımlarını da buraya ekleyebilirsiniz...

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentSummary {
        private Integer documentId;
        private String documentType;
        private String fileName;
    }
}