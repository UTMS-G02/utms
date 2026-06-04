package edu.iztech.utms.g02.utms_app.api.evaluation.controller;

import edu.iztech.utms.g02.utms_app.api.evaluation.dto.EvaluationResponse;
import edu.iztech.utms.g02.utms_app.bl.evaluation.EvaluationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * YGK değerlendirme uçları.
 *   GET  /api/evaluations            → skor sıralı liste
 *   POST /api/evaluations/score-all  → bekleyenleri toplu skorla
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/evaluations")
public class EvaluationController {

    private final EvaluationService evaluationService;

    @PreAuthorize("hasRole('YGK')")
    @GetMapping
    public ResponseEntity<List<EvaluationResponse>> getEvaluations() {
        return ResponseEntity.ok(evaluationService.getEvaluations());
    }

    @PreAuthorize("hasRole('YGK')")
    @PostMapping("/score-all")
    public ResponseEntity<String> scoreAll() {
        int scored = evaluationService.scoreAllPendingApplications();
        return ResponseEntity.ok(scored + " başvuru skorlandı ve sıralandı.");
    }
}
