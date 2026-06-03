package edu.iztech.utms.g02.utms_app.api.evaluation.dto;

import lombok.*;

/**
 * Komisyon kararı isteği — dean-review / faculty-board-review / final-dean-review.
 * Skor ağırlıkları ASLA buradan okunmaz; sadece karar + not taşır.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DecisionRequest {

    // "APPROVED" veya "REJECTED"
    private String decision;

    private String notes;
}
