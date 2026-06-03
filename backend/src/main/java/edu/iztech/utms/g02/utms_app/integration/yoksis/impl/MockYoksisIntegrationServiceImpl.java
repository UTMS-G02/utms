package edu.iztech.utms.g02.utms_app.integration.yoksis.impl;

import edu.iztech.utms.g02.utms_app.integration.yoksis.YoksisIntegrationService;
import edu.iztech.utms.g02.utms_app.integration.yoksis.dto.YoksisStudentResponse;
import org.springframework.stereotype.Service;

@Service
public class MockYoksisIntegrationServiceImpl implements YoksisIntegrationService {

    // Small realistic pool: each row is {university, faculty, department}.
    private static final String[][] ACADEMIC_POOL = {
        {"Ege Üniversitesi", "Mühendislik Fakültesi", "Bilgisayar Mühendisliği"},
        {"Dokuz Eylül Üniversitesi", "Mühendislik Fakültesi", "Elektrik-Elektronik Mühendisliği"},
        {"İstanbul Teknik Üniversitesi", "Makine Fakültesi", "Makine Mühendisliği"},
        {"Orta Doğu Teknik Üniversitesi", "Mühendislik Fakültesi", "Endüstri Mühendisliği"},
        {"Boğaziçi Üniversitesi", "Mühendislik Fakültesi", "Kimya Mühendisliği"},
    };

    // Central mock source. Deterministic: the same TCKN always yields the same
    // academic data, so the data shown on the form (GET /api/yoksis/me) matches
    // exactly what ApplicationService.create persists. No real YÖKSİS yet.
    @Override
    public YoksisStudentResponse fetchAcademicDataByTckn(String tckn) {
        int seed = (tckn == null ? "" : tckn).hashCode() & 0x7fffffff;

        String[] row = ACADEMIC_POOL[seed % ACADEMIC_POOL.length];
        double gpa = 2.50 + (seed % 141) / 100.0;   // 2.50 – 3.90 (always above the 2.50 threshold)
        int semester = (seed % 2 == 0) ? 2 : 4;      // only eligible semesters (2 -> apply for 3, 4 -> apply for 5)

        return new YoksisStudentResponse(row[0], row[1], row[2], semester, gpa);
    }
}
