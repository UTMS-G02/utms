package edu.iztech.utms.g02.utms_app.integration.yoksis.impl;

import edu.iztech.utms.g02.utms_app.integration.yoksis.YoksisIntegrationService;
import edu.iztech.utms.g02.utms_app.integration.yoksis.dto.YoksisStudentResponse;
import org.springframework.stereotype.Service;

@Service
public class MockYoksisIntegrationServiceImpl implements YoksisIntegrationService {

    @Override
    public YoksisStudentResponse fetchAcademicDataByTckn(String tckn) {
        // Burada mock bir gecikme (latency) de ekleyebilirsin:
        // Thread.sleep(500); 
        
        // TCKN'ye göre statik veya dinamik mock veri dönebiliriz.
        return new YoksisStudentResponse(
            "Ege Üniversitesi",
            "Mühendislik Fakültesi",
            "Bilgisayar Mühendisliği",
            "3",
            3.65
        );
    }
}
