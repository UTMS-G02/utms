package edu.iztech.utms.g02.utms_app.api.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationCreateRequest {

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
    
    // Not: Belge yükleme işlemi ayrı bir endpoint (POST /documents) olduğu için 
    // belgeler bu create request'in içinde yer almaz.
}