package edu.iztech.utms.g02.utms_app.integration.yoksis.dto;

public record YoksisStudentResponse(
    String currentUniversity,
    String currentFaculty,
    String currentDepartment,
    Integer semester, // GÜNCELLENDİ: String "3. Sınıf" yerine Integer 3, 4, 5 gibi net değerler gelecek
    Double gpa,
    Double yksScore,  // ÖSYM'den (mock) gelen SAY YKS puanı
    Integer yksRank,  // ÖSYM'den (mock) gelen SAY YKS sıralaması
    Integer gpaScale  // YENİ: GPA hangi sistemde — 4 (4'lük) veya 100 (100'lük). Baraj buna göre uygulanır.
) {
    // Geriye dönük uyumluluk: gpaScale belirtilmeyen eski çağrılar 4'lük sistemi varsayar.
    public YoksisStudentResponse(String currentUniversity, String currentFaculty,
                                 String currentDepartment, Integer semester,
                                 Double gpa, Double yksScore, Integer yksRank) {
        this(currentUniversity, currentFaculty, currentDepartment, semester, gpa, yksScore, yksRank, 4);
    }
}
