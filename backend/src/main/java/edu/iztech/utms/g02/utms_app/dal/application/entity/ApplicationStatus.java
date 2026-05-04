package edu.iztech.utms.g02.utms_app.dal.application.entity;

public enum ApplicationStatus {
    DRAFT,           // Öğrenci taslak oluşturdu
    SUBMITTED,       // Öğrenci başvuruyu gönderdi
    OIDB_REVIEW,     // ÖİDB inceliyor
    OIDB_REJECTED,   // ÖİDB reddetti
    YDYO_REVIEW,     // YDYO dil kontrolü bekliyor
    YDYO_REJECTED,   // YDYO reddetti
    EVALUATION_QUEUE // Değerlendirme kuyruğuna alındı (Pair 3'e geçiş)
}
