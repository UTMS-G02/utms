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

@ExtendWith(MockitoExtension.class)
class DecisionServiceTest {

    @Mock private ApplicationRepository applicationRepository;
    @Mock private CommitteeDecisionRepository committeeDecisionRepository;
    @Mock private ApplicationService applicationService;

    @InjectMocks private DecisionService decisionService;

    // ==========================================
    // DEKAN KARARI — recordDeanDecision()
    // ==========================================

    @Test
    void deanApproves_ygkScored_statusBecomesFactultyBoardReview() {
        Application app = buildApp(1, ApplicationStatus.YGK_SCORED);
        stubFind(1, app);

        decisionService.recordDeanDecision(1, approve("İyi değerlendirme."));

        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.FACULTY_BOARD_REVIEW);
        ArgumentCaptor<CommitteeDecision> captor = ArgumentCaptor.forClass(CommitteeDecision.class);
        verify(committeeDecisionRepository).save(captor.capture());
        assertThat(captor.getValue().getDecisionBy()).isEqualTo("DEAN");
        assertThat(captor.getValue().getDecision()).isEqualTo("APPROVED");
    }

    @Test
    void deanRejects_ygkScored_statusBecomesDeanRejected() {
        Application app = buildApp(2, ApplicationStatus.YGK_SCORED);
        stubFind(2, app);

        decisionService.recordDeanDecision(2, reject("Eksik belge."));

        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.DEAN_REJECTED);
    }

    @Test
    void deanApproves_deanReviewStatus_alsoAllowed() {
        Application app = buildApp(3, ApplicationStatus.DEAN_REVIEW);
        stubFind(3, app);

        decisionService.recordDeanDecision(3, approve(null));

        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.FACULTY_BOARD_REVIEW);
    }

    @Test
    void deanApproves_ygkReviewDone_alsoAllowed() {
        // UC-8: YGK başvuruyu değerlendirip Dekanlığa iletince statü YGK_REVIEW_DONE olur;
        // Dekanlık bu aşamadaki başvuruyu da inceleyebilmeli.
        Application app = buildApp(16, ApplicationStatus.YGK_REVIEW_DONE);
        stubFind(16, app);

        decisionService.recordDeanDecision(16, approve(null));

        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.FACULTY_BOARD_REVIEW);
    }

    @Test
    void deanDecision_wrongStatus_throwsIllegalStateException_andSavesNothing() {
        Application app = buildApp(4, ApplicationStatus.SUBMITTED);
        stubFindOnly(4, app);

        assertThatThrownBy(() -> decisionService.recordDeanDecision(4, approve(null)))
                .isInstanceOf(IllegalStateException.class);
        verify(committeeDecisionRepository, never()).save(any());
        verify(applicationRepository, never()).save(any());
    }

    @Test
    void deanRejects_rejectionCodeIsSaved_onlyOnReject() {
        // UC-11: ret kararında gerekçe kodu saklanır.
        Application app = buildApp(20, ApplicationStatus.YGK_SCORED);
        stubFind(20, app);

        decisionService.recordDeanDecision(20, new DecisionRequest(false, "Eksik denklik.", "EX-2"));

        ArgumentCaptor<CommitteeDecision> captor = ArgumentCaptor.forClass(CommitteeDecision.class);
        verify(committeeDecisionRepository).save(captor.capture());
        assertThat(captor.getValue().getDecision()).isEqualTo("REJECTED");
        assertThat(captor.getValue().getRejectionCode()).isEqualTo("EX-2");
    }

    @Test
    void deanApproves_rejectionCodeIsIgnored_storedAsNull() {
        // Onayda gönderilen ret kodu anlamsızdır; saklanmaz.
        Application app = buildApp(21, ApplicationStatus.YGK_SCORED);
        stubFind(21, app);

        decisionService.recordDeanDecision(21, new DecisionRequest(true, "Onay.", "EX-2"));

        ArgumentCaptor<CommitteeDecision> captor = ArgumentCaptor.forClass(CommitteeDecision.class);
        verify(committeeDecisionRepository).save(captor.capture());
        assertThat(captor.getValue().getDecision()).isEqualTo("APPROVED");
        assertThat(captor.getValue().getRejectionCode()).isNull();
    }

    @Test
    void deanDecision_notesAreSaved() {
        Application app = buildApp(5, ApplicationStatus.YGK_SCORED);
        stubFind(5, app);

        decisionService.recordDeanDecision(5, approve("Harika başvuru."));

        ArgumentCaptor<CommitteeDecision> captor = ArgumentCaptor.forClass(CommitteeDecision.class);
        verify(committeeDecisionRepository).save(captor.capture());
        assertThat(captor.getValue().getNotes()).isEqualTo("Harika başvuru.");
    }

    // ==========================================
    // FAKÜLTE KURULU KARARI — recordFacultyBoardDecision()
    // ==========================================

    @Test
    void facultyBoardApproves_statusBecomesFinalDeanReview() {
        Application app = buildApp(6, ApplicationStatus.FACULTY_BOARD_REVIEW);
        stubFind(6, app);

        decisionService.recordFacultyBoardDecision(6, approve(null));

        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.FINAL_DEAN_REVIEW);
        ArgumentCaptor<CommitteeDecision> captor = ArgumentCaptor.forClass(CommitteeDecision.class);
        verify(committeeDecisionRepository).save(captor.capture());
        assertThat(captor.getValue().getDecisionBy()).isEqualTo("FACULTY_BOARD");
    }

    @Test
    void facultyBoardRejects_statusBecomesFacultyBoardRejected() {
        Application app = buildApp(7, ApplicationStatus.FACULTY_BOARD_REVIEW);
        stubFind(7, app);

        decisionService.recordFacultyBoardDecision(7, reject("%80 denklik sağlanamadı."));

        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.FACULTY_BOARD_REJECTED);
    }

    @Test
    void facultyBoardDecision_wrongStatus_throwsIllegalStateException_andSavesNothing() {
        Application app = buildApp(8, ApplicationStatus.YGK_SCORED);
        stubFindOnly(8, app);

        assertThatThrownBy(() -> decisionService.recordFacultyBoardDecision(8, approve(null)))
                .isInstanceOf(IllegalStateException.class);
        verify(committeeDecisionRepository, never()).save(any());
    }

    // ==========================================
    // NİHAİ DEKAN KARARI — recordFinalDeanDecision()
    // ==========================================

    @Test
    void finalDeanApproves_statusBecomesResultPublished() {
        Application app = buildApp(9, ApplicationStatus.FINAL_DEAN_REVIEW);
        stubFind(9, app);

        decisionService.recordFinalDeanDecision(9, approve(null));

        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.RESULT_PUBLISHED);
        ArgumentCaptor<CommitteeDecision> captor = ArgumentCaptor.forClass(CommitteeDecision.class);
        verify(committeeDecisionRepository).save(captor.capture());
        assertThat(captor.getValue().getDecisionBy()).isEqualTo("FINAL_DEAN");
    }

    @Test
    void finalDeanRejects_statusBecomesRejected() {
        Application app = buildApp(10, ApplicationStatus.FINAL_DEAN_REVIEW);
        stubFind(10, app);

        decisionService.recordFinalDeanDecision(10, reject("Nihai red."));

        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.REJECTED);
    }

    @Test
    void finalDeanDecision_wrongStatus_throwsIllegalStateException_andSavesNothing() {
        Application app = buildApp(11, ApplicationStatus.FACULTY_BOARD_REVIEW);
        stubFindOnly(11, app);

        assertThatThrownBy(() -> decisionService.recordFinalDeanDecision(11, approve(null)))
                .isInstanceOf(IllegalStateException.class);
        verify(committeeDecisionRepository, never()).save(any());
    }

    // ==========================================
    // TC-10.6 — BORU HATTI BÜTÜNLÜĞÜ (Pipeline Integrity)
    // Dekan, YGK adımını atlayarak doğrudan Fakülte Kuruluna yönlendiremez.
    // Fakülte Kurulu, Dekan onayı olmadan karar veremez.
    // ==========================================

    @Test
    void tc10_6_deanDecision_evaluationQueueStatus_throwsIllegalStateException() {
        // ÖİDB'den yeni gelen başvuru henüz YGK tarafından puanlanmamış;
        // Dekan bu aşamada karar veremez.
        Application app = buildApp(30, ApplicationStatus.EVALUATION_QUEUE);
        stubFindOnly(30, app);

        assertThatThrownBy(() -> decisionService.recordDeanDecision(30, approve(null)))
                .isInstanceOf(IllegalStateException.class);
        verify(committeeDecisionRepository, never()).save(any());
        verify(applicationRepository, never()).save(any());
    }

    @Test
    void tc10_6_facultyBoardDecision_skippingDean_ygkScoredStatus_throwsIllegalStateException() {
        // YGK puanlamış ama Dekan henüz karar vermemiş;
        // Fakülte Kurulu bu aşamayı atlayarak karar veremez.
        Application app = buildApp(31, ApplicationStatus.YGK_SCORED);
        stubFindOnly(31, app);

        assertThatThrownBy(() -> decisionService.recordFacultyBoardDecision(31, approve(null)))
                .isInstanceOf(IllegalStateException.class);
        verify(committeeDecisionRepository, never()).save(any());
        verify(applicationRepository, never()).save(any());
    }

    @Test
    void tc10_6_facultyBoardDecision_skippingDean_ygkReviewDoneStatus_throwsIllegalStateException() {
        // YGK UC-8 değerlendirmesini tamamlamış ama Dekan henüz karar vermemiş;
        // Fakülte Kurulu bu aşamayı da atlayarak karar veremez.
        Application app = buildApp(32, ApplicationStatus.YGK_REVIEW_DONE);
        stubFindOnly(32, app);

        assertThatThrownBy(() -> decisionService.recordFacultyBoardDecision(32, approve(null)))
                .isInstanceOf(IllegalStateException.class);
        verify(committeeDecisionRepository, never()).save(any());
        verify(applicationRepository, never()).save(any());
    }

    @Test
    void tc10_6_finalDeanDecision_skippingFacultyBoard_throwsIllegalStateException() {
        // Dekan onaylamış ama Fakülte Kurulu henüz karar vermemiş;
        // Nihai Dekan bu aşamayı atlayarak karar veremez.
        Application app = buildApp(33, ApplicationStatus.FACULTY_BOARD_REVIEW);
        stubFindOnly(33, app);

        assertThatThrownBy(() -> decisionService.recordFinalDeanDecision(33, approve(null)))
                .isInstanceOf(IllegalStateException.class);
        verify(committeeDecisionRepository, never()).save(any());
        verify(applicationRepository, never()).save(any());
    }

    // ==========================================
    // GİRİŞ DOĞRULAMA
    // ==========================================

    @Test
    void nullApproved_throwsIllegalArgumentException_andSavesNothing() {
        Application app = buildApp(12, ApplicationStatus.YGK_SCORED);
        stubFindOnly(12, app);

        assertThatThrownBy(() -> decisionService.recordDeanDecision(12, new DecisionRequest(null, null)))
                .isInstanceOf(IllegalArgumentException.class);
        verify(committeeDecisionRepository, never()).save(any());
    }

    // ==========================================
    // BAŞVURU BULUNAMADI
    // ==========================================

    @Test
    void applicationNotFound_throwsEntityNotFoundException() {
        when(applicationRepository.findById(anyInt())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> decisionService.recordDeanDecision(99, approve(null)))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ==========================================
    // EKLE-ONLY — her karar yeni satır
    // ==========================================

    @Test
    void appendOnly_eachDecisionSavesNewCommitteeRow() {
        Application app = buildApp(15, ApplicationStatus.YGK_SCORED);
        stubFind(15, app);

        decisionService.recordDeanDecision(15, approve("İlk karar."));

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
