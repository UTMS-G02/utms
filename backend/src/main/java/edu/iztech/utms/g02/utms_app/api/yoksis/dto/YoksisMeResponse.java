package edu.iztech.utms.g02.utms_app.api.yoksis.dto;

// Academic data of the current student as resolved from YÖKSİS. Read-only on the
// frontend; the application form fills its academic fields from this.
public record YoksisMeResponse(
    String currentUniversity,
    String currentFaculty,
    String currentDepartment,
    Integer semester,       // current (completed) semester: 2 or 4
    Integer currentClass,   // derived year of study: semester / 2
    Double gpa,             // HER ZAMAN 4'lük sistemde (100'lük gelse bile backend çevirir)
    Double yksScore,        // ÖSYM (mock): SAY YKS puanı
    Integer yksRank         // ÖSYM (mock): SAY YKS sıralaması
) {}
