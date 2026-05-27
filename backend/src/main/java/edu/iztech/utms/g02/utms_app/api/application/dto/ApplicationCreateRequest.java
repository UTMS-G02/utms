package edu.iztech.utms.g02.utms_app.api.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import lombok.*;

@Data
@Builder
@Setter // olmalı mı
@Getter // olmalı mı
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationCreateRequest {

    // studentId'yi güvenlik için JWT/SecurityContext üzerinden almanı tavsiye ederim.
    // Eğer zorunluysa burada kalabilir ama manipülasyona açıktır.

    @NotBlank(message = "Öğrenci numarası boş bırakılamaz.")
    private String studentId;

    @NotBlank(message = "Akademik yıl boş bırakılamaz (Örn: 2026-2027).")
    private String academicYear;

    @NotBlank(message = "Hedef fakülte boş bırakılamaz.")
    private String targetFaculty;

    @NotBlank(message = "Hedef bölüm boş bırakılamaz.")
    private String targetDept;

    @NotNull(message = "KVKK onayı zorunludur.")
    private Boolean kvkkAccepted;
    
    @NotNull(message = "SAY YKS puanı boş bırakılamaz.")
    @Positive(message = "Geçerli bir YKS puanı giriniz.")
    private Double sayYksScore;

    @NotNull(message = "SAY YKS sıralaması boş bırakılamaz.")
    @Positive(message = "Geçerli bir sıralama giriniz.")
    private Integer sayYksRank;
    
    // Not: Belge yükleme işlemi ayrı bir endpoint (POST /documents) olduğu için 
    // belgeler bu create request'in içinde yer almaz.
}