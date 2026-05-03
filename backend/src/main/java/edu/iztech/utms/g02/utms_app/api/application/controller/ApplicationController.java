package edu.iztech.utms.g02.utms_app.api.application.controller; 

import edu.iztech.utms.g02.utms_app.api.application.dto.ApplicationCreateRequest;
import edu.iztech.utms.g02.utms_app.api.application.dto.ApplicationResponse;
import edu.iztech.utms.g02.utms_app.api.application.dto.OidbReviewRequest;
import edu.iztech.utms.g02.utms_app.api.application.dto.YdyoReviewRequest;

import edu.iztech.utms.g02.utms_app.bl.application.ApplicationService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.access.prepost.PreAuthorize;      //??????????

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/applications") // Tüm endpointlerin ortak başlangıcı
public class ApplicationController {

    private final ApplicationService applicationService;


    /*
    /    POST /api/applications
    */

    @PreAuthorize("hasRole('STUDENT')") // Sadece STUDENT rolü girebilir
    @PostMapping
    public ResponseEntity<ApplicationResponse> createApplication(@RequestBody ApplicationCreateRequest req) {
        // Not: Gerçek projede userId'yi SecurityContext'ten (token'dan) alırsın
        Long currentUserId = 1L; // Şimdilik temsili
        ApplicationResponse response = applicationService.create(currentUserId, req);
        return ResponseEntity.ok(response);
    }


    /*
    /   PATCH /api/applications/{id}/submit
    */

    @PreAuthorize("hasRole('STUDENT')") 
    @PatchMapping("/{id}/submit")
    public ResponseEntity<String> submitApplication(@PathVariable Long id) {
        // Öğrencinin sadece kendi başvurusunu gönderebilmesi kontrolü Service katmanında yapılır
        Long currentUserId = 1L; // Şimdilik temsili
        applicationService.submit(id, currentUserId);
        return ResponseEntity.ok("Başvuru başarıyla gönderildi.");
    }



    @PreAuthorize("hasRole('OIDB')")
    @PatchMapping("/{id}/oidb-review")
    public ResponseEntity<String> reviewByOidb(@PathVariable Long id, @RequestBody OidbReviewRequest req) {
        applicationService.processOidbReview(id, req);
        return ResponseEntity.ok("OIDB incelemesi kaydedildi.");
    }



    @PreAuthorize("hasRole('YDYO')")
    @PatchMapping("/{id}/ydyo-review")
    public ResponseEntity<String> reviewByYdyo(@PathVariable Long id, @RequestBody YdyoReviewRequest req) {
        applicationService.processYdyoReview(id, req);
        return ResponseEntity.ok("YDYO incelemesi kaydedildi.");
    }
}