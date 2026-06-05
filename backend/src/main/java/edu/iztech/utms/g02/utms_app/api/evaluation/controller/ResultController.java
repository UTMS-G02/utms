package edu.iztech.utms.g02.utms_app.api.evaluation.controller;

import edu.iztech.utms.g02.utms_app.api.evaluation.dto.PublishedResultResponse;
import edu.iztech.utms.g02.utms_app.bl.evaluation.ResultService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Sonuç yayınlama ve görüntüleme uçları.
 *   POST /api/results/publish  → ÖİDB sonuçları yayınlar
 *   GET  /api/results          → STUDENT (kendi) / OIDB (hepsi)
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/results")
public class ResultController {

    private final ResultService resultService;

    @PreAuthorize("hasRole('OIDB')")
    @PostMapping("/publish")
    public ResponseEntity<String> publish() {
        int published = resultService.publishResults();
        return ResponseEntity.ok(published + " sonuç yayınlandı.");
    }

    @PreAuthorize("hasAnyRole('STUDENT', 'OIDB')")
    @GetMapping
    public ResponseEntity<List<PublishedResultResponse>> getResults() {
        return ResponseEntity.ok(resultService.getResults());
    }
}
