package edu.iztech.utms.g02.utms_app.bl.evaluation;

import edu.iztech.utms.g02.utms_app.api.application.dto.ApplicationResponse;
import edu.iztech.utms.g02.utms_app.api.evaluation.dto.DecisionRequest;
import edu.iztech.utms.g02.utms_app.bl.application.ApplicationService;
import edu.iztech.utms.g02.utms_app.dal.application.entity.Application;
import edu.iztech.utms.g02.utms_app.dal.application.entity.ApplicationStatus;
import edu.iztech.utms.g02.utms_app.dal.application.repository.ApplicationRepository;
import edu.iztech.utms.g02.utms_app.dal.evaluation.entity.CommitteeDecision;
import edu.iztech.utms.g02.utms_app.dal.evaluation.repository.CommitteeDecisionRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * DecisionService — komisyon karar zinciri (Dekanlık → Fakülte Kurulu → Nihai Dekanlık).
 * PDF §6 geçiş tablosunu ve committee_decisions ekle-only davranışını doğrular.
 */
@ExtendWith(MockitoExtension.class)
class DecisionServiceTest {

    @Mock private ApplicationRepository applicationRepository;
    @Mock private CommitteeDecisionRepository committeeDecisionRepository;
    @Mock private ApplicationService applicationService;

    @InjectMocks private DecisionService decisionService;

    // ==========================================
    // DEKANLIK
    // ==========================================

    @Test
    void deanApprove_fromYgkScored_movesToFacultyBoardReview_andAppendsDecision() {
        Application app = buildApp(1, ApplicationStatus.YGK_SCORED);
        when(applicationRepository.findById(1)).thenReturn(Optional.of(app));
        when(applicationService.getApplicationById(1)).thenReturn(new ApplicationResponse());

        decisionService.recordDeanDecision(1, new DecisionRequest("APPROVED", "uygun"));

        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.FACULTY_BOARD_REVIEW);

        ArgumentCaptor<CommitteeDecision> captor = ArgumentCaptor.forClass(CommitteeDecision.class);
        verify(committeeDecisionRepository).save(captor.capture());
        CommitteeDecision saved = captor.getValue();
        assertThat(saved.getDecisionBy()).isEqualTo("DEAN");
        assertThat(saved.getDecision()).isEqualTo("APPROVED");
        assertThat(saved.getNotes()).isEqualTo("uygun");
    }

    @Test
    void deanReject_movesToDeanRejected() {
        Application app = buildApp(1, ApplicationStatus.YGK_SCORED);
        when(applicationRepository.findById(1)).thenReturn(Optional.of(app));
        when(applicationService.getApplicationById(1)).thenReturn(new ApplicationResponse());

        decisionService.recordDeanDecision(1, new DecisionRequest("REJECTED", null));

        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.DEAN_REJECTED);
    }

    // ==========================================
    // FAKÜLTE KURULU
    // ==========================================

    @Test
    void facultyApprove_fromFacultyBoardReview_movesToFinalDeanReview() {
        Application app = buildApp(1, ApplicationStatus.FACULTY_BOARD_REVIEW);
        when(applicationRepository.findById(1)).thenReturn(Optional.of(app));
        when(applicationService.getApplicationById(1)).thenReturn(new ApplicationResponse());

        decisionService.recordFacultyBoardDecision(1, new DecisionRequest("APPROVED", null));

        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.FINAL_DEAN_REVIEW);

        ArgumentCaptor<CommitteeDecision> captor = ArgumentCaptor.forClass(CommitteeDecision.class);
        verify(committeeDecisionRepository).save(captor.capture());
        assertThat(captor.getValue().getDecisionBy()).isEqualTo("FACULTY_BOARD");
    }

    @Test
    void facultyReject_movesToFacultyBoardRejected() {
        Application app = buildApp(1, ApplicationStatus.FACULTY_BOARD_REVIEW);
        when(applicationRepository.findById(1)).thenReturn(Optional.of(app));
        when(applicationService.getApplicationById(1)).thenReturn(new ApplicationResponse());

        decisionService.recordFacultyBoardDecision(1, new DecisionRequest("REJECTED", null));

        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.FACULTY_BOARD_REJECTED);
    }

    // ==========================================
    // NİHAİ DEKANLIK
    // ==========================================

    @Test
    void finalDeanApprove_fromFinalDeanReview_movesToResultPublished_andStoresFinalDeanRole() {
        Application app = buildApp(1, ApplicationStatus.FINAL_DEAN_REVIEW);
        when(applicationRepository.findById(1)).thenReturn(Optional.of(app));
        when(applicationService.getApplicationById(1)).thenReturn(new ApplicationResponse());

        // küçük harf "approved" da kabul edilmeli (normalizeDecision)
        decisionService.recordFinalDeanDecision(1, new DecisionRequest("approved", null));

        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.RESULT_PUBLISHED);

        ArgumentCaptor<CommitteeDecision> captor = ArgumentCaptor.forClass(CommitteeDecision.class);
        verify(committeeDecisionRepository).save(captor.capture());
        assertThat(captor.getValue().getDecisionBy()).isEqualTo("FINAL_DEAN");
    }

    @Test
    void finalDeanReject_movesToRejected() {
        Application app = buildApp(1, ApplicationStatus.FINAL_DEAN_REVIEW);
        when(applicationRepository.findById(1)).thenReturn(Optional.of(app));
        when(applicationService.getApplicationById(1)).thenReturn(new ApplicationResponse());

        decisionService.recordFinalDeanDecision(1, new DecisionRequest("REJECTED", null));

        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.REJECTED);
    }

    // ==========================================
    // KORUMALAR (GUARDS)
    // ==========================================

    @Test
    void deanReview_wrongStatus_throwsIllegalState_andSavesNothing() {
        Application app = buildApp(1, ApplicationStatus.SUBMITTED); // dekanlık aşamasında değil
        when(applicationRepository.findById(1)).thenReturn(Optional.of(app));

        assertThatThrownBy(() -> decisionService.recordDeanDecision(1, new DecisionRequest("APPROVED", null)))
                .isInstanceOf(IllegalStateException.class);

        verify(committeeDecisionRepository, never()).save(any());
        verify(applicationRepository, never()).save(any());
    }

    @Test
    void invalidDecision_throwsIllegalArgument_andSavesNothing() {
        Application app = buildApp(1, ApplicationStatus.YGK_SCORED);
        when(applicationRepository.findById(1)).thenReturn(Optional.of(app));

        assertThatThrownBy(() -> decisionService.recordDeanDecision(1, new DecisionRequest("MAYBE", null)))
                .isInstanceOf(IllegalArgumentException.class);

        verify(committeeDecisionRepository, never()).save(any());
    }

    @Test
    void applicationNotFound_throwsEntityNotFound() {
        when(applicationRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> decisionService.recordDeanDecision(99, new DecisionRequest("APPROVED", null)))
                .isInstanceOf(EntityNotFoundException.class);
    }

    private Application buildApp(Integer id, ApplicationStatus status) {
        return Application.builder().applicationId(id).status(status).build();
    }
}
