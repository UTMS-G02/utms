package edu.iztech.utms.g02.utms_app.api.audit.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Denetim kaydı satırı — {@code GET /api/audit-logs} (ÖİDB).
 */
@Data
@Builder
public class AuditLogResponse {
    private Integer id;
    private String actor;
    private String action;
    private Integer applicationId;
    private String details;
    private LocalDateTime timestamp;
}
