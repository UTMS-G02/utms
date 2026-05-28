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

    // studentId'yi güvenlik için JWT/SecurityContext üzerinden almanı tavsiye ederim.
    // Eğer zorunluysa burada kalabilir ama manipülasyona açıktır.

    //@NotBlank(message = "Öğrenci numarası boş bırakılamaz.")
    //private String studentId;

    @NotBlank(message = "Akademik yıl boş bırakılamaz (Örn: 2026-2027).")
    private String academicYear;

    @NotBlank(message = "Hedef fakülte boş bırakılamaz.")
    private String targetFaculty;

    @NotBlank(message = "Hedef bölüm boş bırakılamaz.")
    private String targetDepartment;

    //@NotNull(message = "KVKK onayı zorunludur.")
    //private Boolean kvkkAccepted;
    
    @NotNull(message = "SAY YKS puanı boş bırakılamaz.")
    @Positive(message = "Geçerli bir YKS puanı giriniz.")
    private Double sayYksScore;

    @NotNull(message = "SAY YKS sıralaması boş bırakılamaz.")
    @Positive(message = "Geçerli bir sıralama giriniz.")
    private Integer sayYksRank;
    
    // Not: Belge yükleme işlemi ayrı bir endpoint (POST /documents) olduğu için 
    // belgeler bu create request'in içinde yer almaz.
}