package edu.iztech.utms.g02.utms_app.api.evaluation.dto;

/**
 * Fakülte Kurulu reddi için önceden tanımlı gerekçe kodları (TC-11.2 — zorunlu dropdown).
 *
 * <p>Frontend bu listeyi {@code GET /api/faculty-board/rejection-reasons}'tan çeker; kurul red
 * kararında bir kodu seçer ve {@code DecisionRequest.rejectionCode} olarak gönderir.
 */
public enum RejectionReason {

    EQUIVALENCY_BELOW_80("Ders denkliği %80 eşiğinin altında"),
    MISSING_PREREQUISITE("Ön koşul dersleri tamamlanmamış"),
    QUOTA_FULL("Bölüm kontenjanı dolmuştur"),
    INSUFFICIENT_DOCUMENTS("Belgeler yetersiz veya eksik"),
    OTHER("Diğer");

    private final String label;

    RejectionReason(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
