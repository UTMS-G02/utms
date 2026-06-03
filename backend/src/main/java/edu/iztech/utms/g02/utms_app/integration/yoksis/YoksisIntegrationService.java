package edu.iztech.utms.g02.utms_app.integration.yoksis;

import edu.iztech.utms.g02.utms_app.integration.yoksis.dto.YoksisStudentResponse;

public interface YoksisIntegrationService {
    YoksisStudentResponse fetchAcademicDataByTckn(String tckn);
}
