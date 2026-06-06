package edu.iztech.utms.g02.utms_app.api.evaluation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Red gerekçe dropdown seçeneği — {@code GET /api/faculty-board/rejection-reasons}.
 */
@Data
@AllArgsConstructor
public class RejectionReasonResponse {
    private String code;   // RejectionReason adı (DecisionRequest.rejectionCode olarak gönderilir)
    private String label;  // kullanıcıya gösterilen Türkçe metin
}
