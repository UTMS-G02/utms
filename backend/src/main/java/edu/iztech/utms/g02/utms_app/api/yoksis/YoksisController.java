package edu.iztech.utms.g02.utms_app.api.yoksis;

import edu.iztech.utms.g02.utms_app.api.yoksis.dto.YoksisMeResponse;
import edu.iztech.utms.g02.utms_app.bl.yoksis.YoksisService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
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
}
