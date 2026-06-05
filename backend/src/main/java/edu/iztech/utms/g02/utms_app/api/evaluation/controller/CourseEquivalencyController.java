package edu.iztech.utms.g02.utms_app.api.evaluation.controller;

import edu.iztech.utms.g02.utms_app.api.evaluation.dto.CourseEquivalencyRow;
import edu.iztech.utms.g02.utms_app.api.evaluation.dto.IntibakTableRequest;
import edu.iztech.utms.g02.utms_app.api.evaluation.dto.IntibakTableResponse;
import edu.iztech.utms.g02.utms_app.bl.evaluation.CourseEquivalencyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * İntibak tablosu uçları.
 *   GET    /api/applications/{id}/intibak            → tabloyu getir (YGK, DEAN_OFFICE, FACULTY_BOARD)
 *   PUT    /api/applications/{id}/intibak            → tüm tabloyu kaydet/değiştir (YGK)
 *   POST   /api/applications/{id}/intibak/rows       → tek satır ekle (YGK)
 *   PATCH  /api/applications/{id}/intibak/rows/{rid} → satır düzenle (YGK)
 *   DELETE /api/applications/{id}/intibak/rows/{rid} → satır sil (YGK)
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

    @PreAuthorize("hasRole('YGK')")
    @PostMapping("/{id}/intibak/rows")
    public ResponseEntity<IntibakTableResponse> addRow(@PathVariable Integer id,
                                                       @RequestBody CourseEquivalencyRow row) {
        return ResponseEntity.ok(courseEquivalencyService.addRow(id, row));
    }

    @PreAuthorize("hasRole('YGK')")
    @PatchMapping("/{id}/intibak/rows/{rowId}")
    public ResponseEntity<IntibakTableResponse> editRow(@PathVariable Integer id,
                                                        @PathVariable Integer rowId,
                                                        @RequestBody CourseEquivalencyRow row) {
        return ResponseEntity.ok(courseEquivalencyService.editRow(id, rowId, row));
    }

    @PreAuthorize("hasRole('YGK')")
    @DeleteMapping("/{id}/intibak/rows/{rowId}")
    public ResponseEntity<IntibakTableResponse> deleteRow(@PathVariable Integer id,
                                                          @PathVariable Integer rowId) {
        return ResponseEntity.ok(courseEquivalencyService.deleteRow(id, rowId));
    }
}
