package edu.iztech.utms.g02.utms_app.api.evaluation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * YGK'nin tek bir başvuru için değerlendirmesi — POST /api/evaluations/{id}/submit (UC-8).
 *
 * <ul>
 *   <li>{@code conditionsMet}: bölüme özel koşullar karşılanıyor mu? (Application.ygkApproved)</li>
 *   <li>{@code generalNote}:  "Genel Değerlendirme Notu" — intibak shortcoming notundan ayrıdır.</li>
 * </ul>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class YgkEvaluationRequest {

    private Boolean conditionsMet;

    private String generalNote;
}
