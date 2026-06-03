package edu.iztech.utms.g02.utms_app.api.evaluation.dto;

import lombok.*;

/**
 * Komisyon kararı isteği — dean-review / faculty-board-review / final-dean-review.
 *
 * <p>Pair 2'nin inceleme uçlarıyla (oidb-review, ydyo-review) tutarlı şekilde
 * {@code approved} (boolean) + {@code notes} alır. Skor ağırlıkları ASLA buradan okunmaz.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DecisionRequest {

    // true = onay (APPROVED), false = ret (REJECTED). Zorunlu.
    private Boolean approved;

    private String notes;
}
