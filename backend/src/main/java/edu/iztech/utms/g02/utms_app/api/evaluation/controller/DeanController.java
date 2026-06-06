package edu.iztech.utms.g02.utms_app.api.evaluation.controller;

import edu.iztech.utms.g02.utms_app.api.application.dto.ApplicationResponse;
import edu.iztech.utms.g02.utms_app.api.evaluation.dto.BatchForwardRequest;
import edu.iztech.utms.g02.utms_app.api.evaluation.dto.BatchForwardResponse;
import edu.iztech.utms.g02.utms_app.bl.evaluation.DeanForwardService;
import edu.iztech.utms.g02.utms_app.bl.evaluation.DeanQueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Dekanlık Ofisi paneli — router görünümü (fakülte-kapsamlı, salt-okunur listeleme).
 *
 * <p>Dekanlık Ofisi karar vermez; sekme listelerini buradan alır, iletim (forward)
 * işlemlerini ayrı forward uçlarından yapar. Liste YALNIZCA oturum açan dekanın
 * fakültesindeki başvuruları döndürür.
 */
@RestController
@RequestMapping("/api/dean")
@RequiredArgsConstructor
public class DeanController {

    private final DeanQueueService deanQueueService;
    private final DeanForwardService deanForwardService;

    @PreAuthorize("hasRole('DEAN_OFFICE')")
    @GetMapping("/applications")
    public ResponseEntity<List<ApplicationResponse>> queue(@RequestParam DeanQueueService.Queue queue) {
        return ResponseEntity.ok(deanQueueService.list(queue));
    }

    /** ÖİDB'den gelen başvuruyu ilgili bölümün YGK'sına iletir (TC-10.0, fakülte-kapsamlı). */
    @PreAuthorize("hasRole('DEAN_OFFICE')")
    @PatchMapping("/applications/{id}/forward-to-ygk")
    public ResponseEntity<ApplicationResponse> forwardToYgk(@PathVariable Integer id) {
        return ResponseEntity.ok(deanForwardService.forwardToYgk(id));
    }

    /** YGK'dan gelen başvuruyu Fakülte Kurulu'na iletir (karar yok, fakülte-kapsamlı). */
    @PreAuthorize("hasRole('DEAN_OFFICE')")
    @PatchMapping("/applications/{id}/forward-to-faculty-board")
    public ResponseEntity<ApplicationResponse> forwardToFacultyBoard(@PathVariable Integer id) {
        return ResponseEntity.ok(deanForwardService.forwardToFacultyBoard(id));
    }

    /** Fakülte Kurulu kararını (kabul VEYA red) ÖİDB'ye iletir (TC-10.3 / TC-10.4). */
    @PreAuthorize("hasRole('DEAN_OFFICE')")
    @PatchMapping("/applications/{id}/forward-to-oidb")
    public ResponseEntity<ApplicationResponse> forwardToOidb(@PathVariable Integer id) {
        return ResponseEntity.ok(deanForwardService.forwardToOidb(id));
    }

    /** TC-10.7: Birden fazla başvuruyu tek işlemde iletir (toplu seçim). */
    @PreAuthorize("hasRole('DEAN_OFFICE')")
    @PostMapping("/applications/batch-forward")
    public ResponseEntity<BatchForwardResponse> batchForward(@RequestBody BatchForwardRequest req) {
        return ResponseEntity.ok(deanForwardService.batchForward(req));
    }
}
