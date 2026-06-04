package edu.iztech.utms.g02.utms_app.api.evaluation.dto;

import edu.iztech.utms.g02.utms_app.dal.application.entity.ApplicationStatus;
import lombok.*;

import java.time.LocalDateTime;

/**
 * YGK değerlendirme listesi için satır — GET /api/evaluations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationResponse {

    private Integer applicationId;
    private String studentName;
    private Double gpa;
    private Double yksScore;
    private Double compositeScore;
    private Integer ranking;
    private ApplicationStatus status;
    private LocalDateTime calculatedAt;

    // UC-8 YGK değerlendirmesi — submit sonrası dolar
    private Boolean conditionsMet;   // Application.ygkApproved
    private String generalNote;      // EvaluationResult.generalNote
}
