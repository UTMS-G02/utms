package edu.iztech.utms.g02.utms_app.bl.audit;

import edu.iztech.utms.g02.utms_app.dal.audit.entity.AuditLog;
import edu.iztech.utms.g02.utms_app.dal.audit.repository.AuditLogRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock private AuditLogRepository auditLogRepository;

    @InjectMocks private AuditService auditService;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void record_savesEntryWithCurrentActor() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("ygk@iyte.edu.tr", "pw", List.of()));

        auditService.record("YGK_SCORE_ALL", 5, "5 başvuru skorlandı");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog saved = captor.getValue();
        assertThat(saved.getActor()).isEqualTo("ygk@iyte.edu.tr");
        assertThat(saved.getAction()).isEqualTo("YGK_SCORE_ALL");
        assertThat(saved.getApplicationId()).isEqualTo(5);
        assertThat(saved.getDetails()).isEqualTo("5 başvuru skorlandı");
    }

    @Test
    void record_noSecurityContext_actorIsSystem() {
        auditService.record("RESULTS_PUBLISHED", null, "3 sonuç yayınlandı");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getActor()).isEqualTo("system");
        assertThat(captor.getValue().getApplicationId()).isNull();
    }
}
