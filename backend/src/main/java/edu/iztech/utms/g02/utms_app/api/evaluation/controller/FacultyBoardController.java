package edu.iztech.utms.g02.utms_app.api.evaluation.controller;

import edu.iztech.utms.g02.utms_app.api.application.dto.ApplicationResponse;
import edu.iztech.utms.g02.utms_app.bl.evaluation.FacultyBoardQueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Fakülte Kurulu paneli listesi (UC-11) — fakülte-kapsamlı.
 *
 * <p>'Değerlendirme Bekleyenler' (PENDING) ve 'Değerlendirilenler' (DECIDED) sekmelerini besler.
 * Karar/inceleme uçları {@code /api/applications/{id}/board-review} ve {@code .../faculty-board-review}'dadır.
 */
@RestController
@RequestMapping("/api/faculty-board")
@RequiredArgsConstructor
public class FacultyBoardController {

    private final FacultyBoardQueueService facultyBoardQueueService;

    @PreAuthorize("hasRole('FACULTY_BOARD')")
    @GetMapping("/applications")
    public ResponseEntity<List<ApplicationResponse>> queue(@RequestParam FacultyBoardQueueService.Queue queue) {
        return ResponseEntity.ok(facultyBoardQueueService.list(queue));
    }
}
