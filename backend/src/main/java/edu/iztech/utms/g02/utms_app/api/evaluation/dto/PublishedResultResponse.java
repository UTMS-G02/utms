package edu.iztech.utms.g02.utms_app.api.evaluation.dto;

import lombok.*;

import java.time.LocalDateTime;

/**
 * Yayınlanmış sonuç satırı — GET /api/results.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublishedResultResponse {

    private Integer applicationId;
    private String studentName;
    private String finalDecision;
    private LocalDateTime publishedAt;
}
