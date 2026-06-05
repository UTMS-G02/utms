package edu.iztech.utms.g02.utms_app.api.evaluation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Komisyon karar geçmişindeki tek bir satırın özeti (ekle-only kayıt).
 * Fakülte Kurulu / Dekanlık inceleme ekranında karar geçmişini gösterir.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommitteeDecisionSummary {

    private String decisionBy;      // YGK / DEAN / FACULTY_BOARD / FINAL_DEAN
    private String decision;        // APPROVED / REJECTED / CONDITIONS_MET / CONDITIONS_NOT_MET
    private String notes;
    private String rejectionCode;   // yalnızca ret kararlarında dolu
    private LocalDateTime decidedAt;
}
