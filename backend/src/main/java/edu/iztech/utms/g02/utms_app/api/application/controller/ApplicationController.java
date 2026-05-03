package edu.iztech.utms.g02.utms_app.api.application.controller;

import edu.iztech.utms.g02.utms_app.api.application.dto.ApplicationCreateRequest;
import edu.iztech.utms.g02.utms_app.api.application.dto.ApplicationResponse;
import edu.iztech.utms.g02.utms_app.api.application.dto.OidbReviewRequest;
import edu.iztech.utms.g02.utms_app.api.application.dto.YdyoReviewRequest;

import edu.iztech.utms.g02.utms_app.bl.application.ApplicationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/applications")
public class ApplicationController {

    private final ApplicationService applicationService;

    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping
    public ResponseEntity<ApplicationResponse> createApplication(@RequestBody @Valid ApplicationCreateRequest req) {
        Long currentUserId = 1L; // TODO: SecurityContext'ten alınacak
        ApplicationResponse response = applicationService.create(currentUserId, req);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('STUDENT')")
    @PatchMapping("/{id}/submit")
    public ResponseEntity<String> submitApplication(@PathVariable Long id) {
        Long currentUserId = 1L; // TODO: SecurityContext'ten alınacak
        applicationService.submit(id, currentUserId);
        return ResponseEntity.ok("Başvuru başarıyla gönderildi.");
    }

    @PreAuthorize("hasAnyRole('STUDENT', 'OIDB', 'YDYO')")
    @GetMapping
    public ResponseEntity<List<ApplicationResponse>> getAllApplications() {
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
    public ResponseEntity<String> reviewByOidb(@PathVariable Long id, @RequestBody @Valid OidbReviewRequest req) {
        applicationService.processOidbReview(id, req);
        return ResponseEntity.ok("OIDB incelemesi kaydedildi.");
    }

    @PreAuthorize("hasRole('YDYO')")
    @PatchMapping("/{id}/ydyo-review")
    public ResponseEntity<String> reviewByYdyo(@PathVariable Long id, @RequestBody @Valid YdyoReviewRequest req) {
        applicationService.processYdyoReview(id, req);
        return ResponseEntity.ok("YDYO incelemesi kaydedildi.");
    }

    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping(value = "/{id}/documents", consumes = "multipart/form-data")
    public ResponseEntity<String> uploadDocument(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        applicationService.uploadDocument(id, file);
        return ResponseEntity.ok("Belge başarıyla yüklendi.");
    }
}