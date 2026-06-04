package edu.iztech.utms.g02.utms_app.api.yoksis;

import edu.iztech.utms.g02.utms_app.api.yoksis.dto.YoksisMeResponse;
import edu.iztech.utms.g02.utms_app.bl.yoksis.YoksisService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/yoksis")
@RequiredArgsConstructor
public class YoksisController {

    private final YoksisService yoksisService;

    /**
     * Returns the current student's academic data from YÖKSİS.
     * The application form renders these fields read-only.
     * GET /api/yoksis/me
     */
    @GetMapping("/me")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<YoksisMeResponse> me() {
        return ResponseEntity.ok(yoksisService.getForCurrentUser());
    }

    /**
     * "Yeni Başvuru" formunda, başvuru henüz oluşturulmadan e-Devlet/ÖSYM belgesini
     * önizlemek için mock PDF döndürür (inline). Tip: STUDENT_CERTIFICATE | TRANSCRIPT | YKS_RESULT.
     * GET /api/yoksis/documents/{type}/preview
     */
    @GetMapping("/documents/{type}/preview")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<byte[]> previewDocument(@PathVariable String type) {
        YoksisService.DocumentPreview preview = yoksisService.getDocumentPreview(type);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + preview.fileName() + "\"")
                .body(preview.content());
    }
}
