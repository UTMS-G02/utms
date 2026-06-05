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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Router modeli: TEK karar makamı Fakülte Kurulu. Dekan/nihai-dekan kararları kaldırıldığından
 * bu sınıf yalnızca {@code recordFacultyBoardDecision}'ı test eder.
 */
@ExtendWith(MockitoExtension.class)
class DecisionServiceTest {

    @Mock private ApplicationRepository applicationRepository;
    @Mock private CommitteeDecisionRepository committeeDecisionRepository;
    @Mock private ApplicationService applicationService;

    @InjectMocks private DecisionService decisionService;

    // ==========================================
    // FAKÜLTE KURULU KARARI — recordFacultyBoardDecision()
    // ==========================================

    @Test
    void facultyBoardApproves_statusBecomesFacultyBoardAccepted() {
        Application app = buildApp(6, ApplicationStatus.FACULTY_BOARD_REVIEW);
        stubFind(6, app);

        decisionService.recordFacultyBoardDecision(6, approve(null));

        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.FACULTY_BOARD_ACCEPTED);
        ArgumentCaptor<CommitteeDecision> captor = ArgumentCaptor.forClass(CommitteeDecision.class);
        verify(committeeDecisionRepository).save(captor.capture());
        assertThat(captor.getValue().getDecisionBy()).isEqualTo("FACULTY_BOARD");
        assertThat(captor.getValue().getDecision()).isEqualTo("APPROVED");
    }

    @Test
    void facultyBoardRejects_statusBecomesFacultyBoardRejected() {
        Application app = buildApp(7, ApplicationStatus.FACULTY_BOARD_REVIEW);
        stubFind(7, app);

        decisionService.recordFacultyBoardDecision(7, reject("%80 denklik sağlanamadı."));

        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.FACULTY_BOARD_REJECTED);
    }

    @Test
    void rejectionCodeIsSaved_onlyOnReject() {
        Application app = buildApp(20, ApplicationStatus.FACULTY_BOARD_REVIEW);
        stubFind(20, app);

        decisionService.recordFacultyBoardDecision(20, new DecisionRequest(false, "Eksik denklik.", "EX-2"));

        ArgumentCaptor<CommitteeDecision> captor = ArgumentCaptor.forClass(CommitteeDecision.class);
        verify(committeeDecisionRepository).save(captor.capture());
        assertThat(captor.getValue().getDecision()).isEqualTo("REJECTED");
        assertThat(captor.getValue().getRejectionCode()).isEqualTo("EX-2");
    }

    @Test
    void approve_rejectionCodeIsIgnored_storedAsNull() {
        Application app = buildApp(21, ApplicationStatus.FACULTY_BOARD_REVIEW);
        stubFind(21, app);

        decisionService.recordFacultyBoardDecision(21, new DecisionRequest(true, "Onay.", "EX-2"));

        ArgumentCaptor<CommitteeDecision> captor = ArgumentCaptor.forClass(CommitteeDecision.class);
        verify(committeeDecisionRepository).save(captor.capture());
        assertThat(captor.getValue().getDecision()).isEqualTo("APPROVED");
        assertThat(captor.getValue().getRejectionCode()).isNull();
    }

    @Test
    void notesAreSaved() {
        Application app = buildApp(5, ApplicationStatus.FACULTY_BOARD_REVIEW);
        stubFind(5, app);

        decisionService.recordFacultyBoardDecision(5, approve("Harika başvuru."));

        ArgumentCaptor<CommitteeDecision> captor = ArgumentCaptor.forClass(CommitteeDecision.class);
        verify(committeeDecisionRepository).save(captor.capture());
        assertThat(captor.getValue().getNotes()).isEqualTo("Harika başvuru.");
    }

    // ==========================================
    // BORU HATTI BÜTÜNLÜĞÜ — Fakülte Kurulu, kendisinden önceki aşamayı atlayarak karar veremez.
    // (Dekan iletimi olmadan FACULTY_BOARD_REVIEW'a gelinmez.)
    // ==========================================

    @Test
    void facultyBoardDecision_ygkScored_throwsIllegalStateException_andSavesNothing() {
        Application app = buildApp(8, ApplicationStatus.YGK_SCORED);
        stubFindOnly(8, app);

        assertThatThrownBy(() -> decisionService.recordFacultyBoardDecision(8, approve(null)))
                .isInstanceOf(IllegalStateException.class);
        verify(committeeDecisionRepository, never()).save(any());
        verify(applicationRepository, never()).save(any());
    }

    @Test
    void facultyBoardDecision_ygkReviewDone_throwsIllegalStateException() {
        Application app = buildApp(32, ApplicationStatus.YGK_REVIEW_DONE);
        stubFindOnly(32, app);

        assertThatThrownBy(() -> decisionService.recordFacultyBoardDecision(32, approve(null)))
                .isInstanceOf(IllegalStateException.class);
        verify(committeeDecisionRepository, never()).save(any());
    }

    // ==========================================
    // GİRİŞ DOĞRULAMA + BULUNAMADI
    // ==========================================

    @Test
    void nullApproved_throwsIllegalArgumentException_andSavesNothing() {
        Application app = buildApp(12, ApplicationStatus.FACULTY_BOARD_REVIEW);
        stubFindOnly(12, app);

        assertThatThrownBy(() -> decisionService.recordFacultyBoardDecision(12, new DecisionRequest(null, null)))
                .isInstanceOf(IllegalArgumentException.class);
        verify(committeeDecisionRepository, never()).save(any());
    }

    @Test
    void applicationNotFound_throwsEntityNotFoundException() {
        when(applicationRepository.findById(anyInt())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> decisionService.recordFacultyBoardDecision(99, approve(null)))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ==========================================
    // EKLE-ONLY — her karar yeni satır
    // ==========================================

    @Test
    void appendOnly_eachDecisionSavesNewCommitteeRow() {
        Application app = buildApp(15, ApplicationStatus.FACULTY_BOARD_REVIEW);
        stubFind(15, app);

        decisionService.recordFacultyBoardDecision(15, approve("İlk karar."));

        verify(committeeDecisionRepository).save(any(CommitteeDecision.class));
        verify(applicationRepository).save(app);
    }

    // ==========================================
    // YARDIMCI METOTLAR
    // ==========================================

    private Application buildApp(Integer id, ApplicationStatus status) {
        return Application.builder()
                .applicationId(id)
                .status(status)
                .gpa(3.0)
                .sayYksScore(400.0)
                .build();
    }

    private DecisionRequest approve(String notes) {
        return new DecisionRequest(true, notes);
    }

    private DecisionRequest reject(String notes) {
        return new DecisionRequest(false, notes);
    }

    private void stubFindOnly(Integer id, Application app) {
        when(applicationRepository.findById(id)).thenReturn(Optional.of(app));
    }

    private void stubFind(Integer id, Application app) {
        when(applicationRepository.findById(id)).thenReturn(Optional.of(app));
        when(applicationRepository.save(any())).thenReturn(app);
        when(committeeDecisionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(applicationService.getApplicationById(id)).thenReturn(ApplicationResponse.builder().id(id).build());
    }
}
