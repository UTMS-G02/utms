package edu.iztech.utms.g02.utms_app.bl.app;

import edu.iztech.utms.g02.utms_app.api.application.controller.*;
import edu.iztech.utms.g02.utms_app.api.application.dto.*;

import edu.iztech.utms.g02.utms_app.dal.application.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;


@SpringBootTest @AutoConfigureMockMvc

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    @Mock private ApplicationRepository appRepository;
    @Mock private DocumentRepository docRepository;


    // ─── submit application ────────────────────────────────────────────────────────────

    @Test
    void submitApplication_changesStatusToSubmitted() {
        Application app = new Application();
        app.setStatus(ApplicationStatus.DRAFT);
        // kurulum...
        applicationService.submit(app.getId(), studentUserId);
        assertEquals(ApplicationStatus.SUBMITTED,
            applicationRepository.findById(app.getId()).get().getStatus());
    }






    
}

