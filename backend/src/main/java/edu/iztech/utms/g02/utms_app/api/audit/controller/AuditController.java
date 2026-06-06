package edu.iztech.utms.g02.utms_app.api.audit.controller;

import edu.iztech.utms.g02.utms_app.api.audit.dto.AuditLogResponse;
import edu.iztech.utms.g02.utms_app.bl.audit.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Denetim izi görüntüleme (ÖİDB). Tüm kayıtlar (en yeni önce) veya {@code ?applicationId=} ile
 * tek başvurunun izi. Yazım servislerin içinde otomatik yapılır; bu yalnızca okuma ucudur.
 */
@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;

    @PreAuthorize("hasRole('OIDB')")
    @GetMapping
    public ResponseEntity<List<AuditLogResponse>> list(@RequestParam(required = false) Integer applicationId) {
        return ResponseEntity.ok(
                applicationId != null ? auditService.forApplication(applicationId) : auditService.recent());
    }
}
