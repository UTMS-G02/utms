package edu.iztech.utms.g02.utms_app.api.evaluation.dto;

import edu.iztech.utms.g02.utms_app.api.application.dto.ApplicationResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * UC-11 — Fakülte Kurulu / Dekanlık inceleme ekranı için birleştirilmiş ("consolidated")
 * başvuru raporu. Makam karar vermeden önce tüm veriyi tek çağrıda görür:
 *
 * <ul>
 *   <li>{@code application}: Pair 2'nin başvuru verisi (öğrenci, hedef bölüm, belgeler...)</li>
 *   <li>YGK skoru/sıralaması ve UC-8 değerlendirmesi (conditionsMet + genel not)</li>
 *   <li>İntibak (ders denklik) tablosu</li>
 *   <li>Ekle-only komisyon karar geçmişi</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardReviewResponse {

    private ApplicationResponse application;

    // YGK aşaması
    private Double compositeScore;
    private Integer ranking;
    private Boolean conditionsMet;   // Application.ygkApproved
    private String generalNote;      // EvaluationResult.generalNote
    private LocalDateTime calculatedAt;

    // İntibak tablosu
    private IntibakTableResponse intibak;

    // Komisyon karar geçmişi (eskiden yeniye)
    private List<CommitteeDecisionSummary> decisions;
}
