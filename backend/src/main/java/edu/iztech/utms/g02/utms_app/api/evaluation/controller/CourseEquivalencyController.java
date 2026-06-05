package edu.iztech.utms.g02.utms_app.api.evaluation.controller;

import edu.iztech.utms.g02.utms_app.api.evaluation.dto.IntibakTableRequest;
import edu.iztech.utms.g02.utms_app.api.evaluation.dto.IntibakTableResponse;
import edu.iztech.utms.g02.utms_app.bl.evaluation.CourseEquivalencyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * İntibak tablosu uçları.
 *   GET /api/applications/{id}/intibak  → mevcut tabloyu getir
 *   PUT /api/applications/{id}/intibak  → tabloyu kaydet (replace-all)
 *
 * <p>Tabloyu YALNIZCA YGK düzenler (PUT). Okuma (GET) ise YGK'ye ek olarak
 * Fakülte Kurulu (FACULTY_BOARD) ve Dekanlığa (DEAN_OFFICE) açıktır; bu makamlar
 * UC-11 incelemesinde intibak tablosunu salt-okunur görür.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/applications")
public class CourseEquivalencyController {

    private final CourseEquivalencyService courseEquivalencyService;

    @PreAuthorize("hasAnyRole('YGK', 'FACULTY_BOARD', 'DEAN_OFFICE')")
    @GetMapping("/{id}/intibak")
    public ResponseEntity<IntibakTableResponse> getTable(@PathVariable Integer id) {
        return ResponseEntity.ok(courseEquivalencyService.getTable(id));
    }

    @PreAuthorize("hasRole('YGK')")
    @PutMapping("/{id}/intibak")
    public ResponseEntity<IntibakTableResponse> saveTable(@PathVariable Integer id,
                                                          @RequestBody IntibakTableRequest req) {
        return ResponseEntity.ok(courseEquivalencyService.saveTable(id, req));
    }
}
