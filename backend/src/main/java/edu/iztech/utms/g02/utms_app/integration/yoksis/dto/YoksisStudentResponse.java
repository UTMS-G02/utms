package edu.iztech.utms.g02.utms_app.integration.yoksis.dto;

public record YoksisStudentResponse(
    String currentUniversity,
    String currentFaculty,
    String currentDepartment,
    Integer semester, // GÜNCELLENDİ: String "3. Sınıf" yerine Integer 3, 4, 5 gibi net değerler gelecek
    Double gpa
) {}
