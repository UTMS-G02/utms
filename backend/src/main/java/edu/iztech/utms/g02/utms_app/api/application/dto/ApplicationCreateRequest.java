package edu.iztech.utms.g02.utms_app.api.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import lombok.*;

/*
// ApplicationCreateRequest.java
// Öğrenci başvuru oluştururken gönderdiği JSON paketinin şablonu.

// - Örn: targetDepartment, targetFaculty alanlarını taşır
// - Controller bu nesneyi @RequestBody ile doğrudan JSON'dan oluşturur
// - Sadece veri taşır — içinde kod yok, iş kuralı yok
// - getter/setterlar olmayacak diye anladım ama emin değilim
*/

@Data
@Builder
@Setter 
@Getter 
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationCreateRequest {

    @NotBlank(message = "Akademik yıl boş bırakılamaz (Örn: 2026-2027).")
    private String academicYear;

    @NotBlank(message = "Hedef fakülte boş bırakılamaz.")
    private String targetFaculty;

    @NotBlank(message = "Hedef bölüm boş bırakılamaz.")
    private String targetDepartment;

    // Öğrencinin başvurduğu hedef yarıyıl (3 veya 5). YÖKSİS'teki mevcut yarıyıl
    // ile tutarlılığı ApplicationService.create içinde doğrulanır.
    @NotNull(message = "Başvurulan yarıyıl boş bırakılamaz (3 veya 5).")
    private Integer semester;

    //@NotNull(message = "KVKK onayı zorunludur.")
    private Boolean kvkkAccepted;
    
    // YKS puanı/sıralaması artık ÖSYM'den (mock) backend tarafında çekilir; öğrenci
    // formda elle giremez. Bu yüzden opsiyonel — gelirse de ApplicationService
    // bunları yok sayıp ÖSYM verisini kullanır.
    private Double sayYksScore;

    private Integer sayYksRank;

    // Not: Belge yükleme işlemi ayrı bir endpoint (POST /documents) olduğu için
    // belgeler bu create request'in içinde yer almaz.
}