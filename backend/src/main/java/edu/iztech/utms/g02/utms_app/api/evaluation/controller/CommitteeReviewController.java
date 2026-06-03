package edu.iztech.utms.g02.utms_app.api.evaluation.controller;

import edu.iztech.utms.g02.utms_app.api.application.dto.ApplicationResponse;
import edu.iztech.utms.g02.utms_app.api.evaluation.dto.DecisionRequest;
import edu.iztech.utms.g02.utms_app.bl.evaluation.DecisionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Komisyon karar uçları. Pair 2'nin ApplicationController'ına dokunmadan,
 * aynı /api/applications tabanı altında ek PATCH uçları sağlar (yol çakışması yok).
 *
 * <p>DEAN rolü kod tabanında {@code DEAN_OFFICE} olarak tanımlıdır (ROLE_DEAN_OFFICE).
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/applications")
public class CommitteeReviewController {

    private final DecisionService decisionService;

    @PreAuthorize("hasRole('DEAN_OFFICE')")
    @PatchMapping("/{id}/dean-review")
    public ResponseEntity<ApplicationResponse> deanReview(@PathVariable Integer id,
                                                          @RequestBody DecisionRequest req) {
        return ResponseEntity.ok(decisionService.recordDeanDecision(id, req));
    }

    @PreAuthorize("hasRole('FACULTY_BOARD')")
    @PatchMapping("/{id}/faculty-board-review")
    public ResponseEntity<ApplicationResponse> facultyBoardReview(@PathVariable Integer id,
                                                                  @RequestBody DecisionRequest req) {
        return ResponseEntity.ok(decisionService.recordFacultyBoardDecision(id, req));
    }

    @PreAuthorize("hasRole('DEAN_OFFICE')")
    @PatchMapping("/{id}/final-dean-review")
    public ResponseEntity<ApplicationResponse> finalDeanReview(@PathVariable Integer id,
                                                               @RequestBody DecisionRequest req) {
        return ResponseEntity.ok(decisionService.recordFinalDeanDecision(id, req));
    }
}
