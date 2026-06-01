package edu.iztech.utms.g02.utms_app.integration.yoksis.dto;

public record YoksisStudentResponse(
    String currentUniversity,
    String currentFaculty,
    String currentDepartment,
    String studentClass, // Örn: "3. Sınıf"
    Double gpa
) {}
