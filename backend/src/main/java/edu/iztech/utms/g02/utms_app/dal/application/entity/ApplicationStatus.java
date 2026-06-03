package edu.iztech.utms.g02.utms_app.dal.application.entity;

public enum ApplicationStatus {
    DRAFT,                   // Öğrenci taslak oluşturdu
    SUBMITTED,               // Öğrenci başvuruyu gönderdi
    WITHDRAWN,               // Öğrenci değerlendirme dönemi başlamadan geri çekti

    OIDB_REVIEW,             // ÖİDB ön inceleme yapıyor
    REVISION_REQUESTED,      // YENİ EKLENDİ: ÖİDB belgede eksik buldu, öğrencinin düzeltmesini bekliyor
    OIDB_REJECTED,           // ÖİDB reddetti

    YDYO_REVIEW,             // YDYO dil belgesi kontrolü yapıyor
    YDYO_EXAM_PENDING,         // YENİ: Evrak yetersiz, sınava girecek
    YDYO_REJECTED,           // YDYO reddetti
    YDYO_ACCEPTED,           // YDYO onayladı, YGK değerlendirme sırasına alındı

    //OIDB_REVIEW_AFTER_YDYO, // YDYO onayından sonra ÖİDB tekrar inceleme yapıyor (OIDB_REJECTED veya DEAN_OFFICE_REVIEW)

    DEAN_OFFICE_REVIEW,      // Başvuru fakülte dekanlığına gönderildi
    YGK_REVIEW,              // YGK değerlendiriyor
    YGK_REVIEW_DONE,         // YGK değerlendirmesi tamamlandı, fakülte kurulunun onayına gönderilecek
    
    FACULTY_BOARD_REVIEW,    // Fakülte Kurulu YGK değerlendirmesini inceliyor
    //FACULTY_BOARD_RETURNED,  // Fakülte Kurulu yeniden YGK değerlendirmesi istedi
    FACULTY_BOARD_REJECTED,  // Fakülte Kurulu YGK değerlendirmesini reddetti
    FACULTY_BOARD_ACCEPTED,  // Fakülte Kurulu YGK değerlendirmesini kabul etti

    OIDB_FINAL_REVIEW,        // Fakülte Kurulu onayından sonra ÖİDB son bir inceleme yapıyor
    APPROVED,                 // Başvuru kabul edildi
    REJECTED,                 // Başvuru reddedildi

    // --- Pair 3 (Değerlendirme & Sonuç) — additive olarak eklendi ---
    // FACULTY_BOARD_REVIEW / FACULTY_BOARD_REJECTED yukarıda zaten mevcut, tekrar eklenmedi.
    EVALUATION_QUEUE,         // Pair 2'den devir noktası: YGK skorlamasına hazır
    YGK_SCORED,               // YGK compositeScore hesapladı ve sıraladı
    DEAN_REVIEW,              // Dekanlık inceliyor
    DEAN_REJECTED,            // Dekanlık reddetti
    FINAL_DEAN_REVIEW,        // Nihai Dekanlık onayı bekleniyor
    RESULT_PUBLISHED,         // ÖİDB sonucu yayınladı (öğrenciye görünür hâle geldi)
    ACCEPTED                  // Başvuru nihai olarak kabul edildi
}
