package edu.iztech.utms.g02.utms_app.api.evaluation.controller;

import edu.iztech.utms.g02.utms_app.api.application.dto.ApplicationResponse;
import edu.iztech.utms.g02.utms_app.api.evaluation.dto.BoardReviewResponse;
import edu.iztech.utms.g02.utms_app.api.evaluation.dto.DecisionRequest;
import edu.iztech.utms.g02.utms_app.bl.evaluation.BoardReviewService;
import edu.iztech.utms.g02.utms_app.bl.evaluation.DecisionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
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
    private final BoardReviewService boardReviewService;

    /**
     * UC-11 — Fakülte Kurulu / Dekanlık inceleme verisini birleştirilmiş olarak getirir
     * (salt-okunur). Makam karar vermeden önce başvuru + skor + intibak + karar geçmişini görür.
     */
    @PreAuthorize("hasAnyRole('FACULTY_BOARD', 'DEAN_OFFICE')")
    @GetMapping("/{id}/board-review")
    public ResponseEntity<BoardReviewResponse> boardReview(@PathVariable Integer id) {
        return ResponseEntity.ok(boardReviewService.getReview(id));
    }

    // Router modeli: Dekanlık karar vermez (dean-review / final-dean-review kaldırıldı).
    // Dekan iletimleri DeanController altındadır; Fakülte Kurulu kararı tek karar makamıdır.
    @PreAuthorize("hasRole('FACULTY_BOARD')")
    @PatchMapping("/{id}/faculty-board-review")
    public ResponseEntity<ApplicationResponse> facultyBoardReview(@PathVariable Integer id,
                                                                  @RequestBody DecisionRequest req) {
        return ResponseEntity.ok(decisionService.recordFacultyBoardDecision(id, req));
    }
}
