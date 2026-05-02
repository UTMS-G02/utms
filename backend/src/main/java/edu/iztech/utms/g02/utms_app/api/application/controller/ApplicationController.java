package edu.iztech.utms.g02.utms_app.api.application.controller; 

import edu.iztech.utms.g02.utms_app.api.application.dto.ApplicationRequest;
import edu.iztech.utms.g02.utms_app.api.application.dto.ApplicationResponse;
import edu.iztech.utms.g02.utms_app.api.application.dto.OidbReviewRequest;
import edu.iztech.utms.g02.utms_app.api.application.dto.YdyoReviewRequest;

import edu.iztech.utms.g02.utms_app.bl.application.ApplicationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.access.prepost.PreAuthorize;      //??????????
import org.springframework.web.multipart.MultipartFile;                //??????????

import java.util.Map;

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
        applicationService.submitApplication(id);
        return ResponseEntity.ok("Başvuru başarıyla gönderildi.");
    }


    /*
    /   GET /api/applications
    */

    @PreAuthorize("hasAnyRole('STUDENT', 'OIDB', 'YDYO')") // 3 rolden biri yeterli
    @GetMapping
    public ResponseEntity<List<ApplicationResponse>> getAllApplications() {
        // Service katmanında, istek atan kişinin rolüne göre farklı listeler dönecek bir mantık kurulmalı
        List<ApplicationResponse> list = applicationService.getAllApplications();
        return ResponseEntity.ok(list);
    }


    @PreAuthorize("hasAnyRole('STUDENT', 'OIDB', 'YDYO', 'FACULTY', 'DEAN')") 
    @GetMapping("/{id}")
    public ResponseEntity<ApplicationResponse> getApplicationById(@PathVariable Long id) {
        ApplicationResponse response = applicationService.getApplicationById(id);
        return ResponseEntity.ok(response);
    }



    @PreAuthorize("hasRole('OIDB')")
    @PatchMapping("/{id}/oidb-review")
    public ResponseEntity<String> reviewByOidb(@PathVariable Long id, @RequestBody ReviewRequest req) {
        applicationService.reviewByOidb(id, req);
        return ResponseEntity.ok("OIDB incelemesi kaydedildi.");
    }



    @PreAuthorize("hasRole('YDYO')")
    @PatchMapping("/{id}/ydyo-review")
    public ResponseEntity<String> reviewByYdyo(@PathVariable Long id, @RequestBody ReviewRequest req) {
        applicationService.reviewByYdyo(id, req);
        return ResponseEntity.ok("YDYO incelemesi kaydedildi.");
    }


    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping(value = "/{id}/documents", consumes = "multipart/form-data")       // consumes ??
    public ResponseEntity<String> uploadDocument(
            @PathVariable Long id, 
            @RequestParam("file") MultipartFile file) {
        
        applicationService.uploadDocument(id, file);
        return ResponseEntity.ok("Belge başarıyla yüklendi.");
    }

}