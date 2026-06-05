package edu.iztech.utms.g02.utms_app.api.evaluation.dto;

import edu.iztech.utms.g02.utms_app.api.application.dto.ApplicationResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * UC-11 Fakülte Kurulu karar yanıtı.
 * Onay kararında denklik oranı %80'in altındaysa {@code belowThreshold=true} döner;
 * karar her halükarda kaydedilir (soft warning).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FacultyBoardDecisionResponse {

    private ApplicationResponse application;
    // null if conditionsMet=false or intibak table is empty
    private Double equivalencyRatio;
    // true if approved=true and ratio < 0.80; false otherwise
    private Boolean belowThreshold;
}
