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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
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
        stubGetById(1);

        decisionService.recordDeanDecision(1, approve("İyi değerlendirme."));

        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.FACULTY_BOARD_REVIEW);
        verify(committeeDecisionRepository).save(argThat(d ->
                "DEAN".equals(d.getDecisionBy()) && "APPROVED".equals(d.getDecision())));
    }

    @Test
    void deanRejects_ygkScored_statusBecomesDeanRejected() {
        Application app = buildApp(2, ApplicationStatus.YGK_SCORED);
        stubFind(2, app);
        stubGetById(2);

        decisionService.recordDeanDecision(2, reject("Eksik belge."));

        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.DEAN_REJECTED);
        verify(committeeDecisionRepository).save(argThat(d ->
                "DEAN".equals(d.getDecisionBy()) && "REJECTED".equals(d.getDecision())));
    }

    @Test
    void deanApproves_deanReviewStatus_alsoAllowed() {
        Application app = buildApp(3, ApplicationStatus.DEAN_REVIEW);
        stubFind(3, app);
        stubGetById(3);

        decisionService.recordDeanDecision(3, approve(null));

        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.FACULTY_BOARD_REVIEW);
    }

    @Test
    void deanDecision_wrongStatus_throwsIllegalStateException() {
        Application app = buildApp(4, ApplicationStatus.FACULTY_BOARD_REVIEW);
        stubFindOnly(4, app);

        assertThatThrownBy(() -> decisionService.recordDeanDecision(4, approve(null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("FACULTY_BOARD_REVIEW");
    }

    @Test
    void deanDecision_notesAreSaved() {
        Application app = buildApp(5, ApplicationStatus.YGK_SCORED);
        stubFind(5, app);
        stubGetById(5);

        decisionService.recordDeanDecision(5, approve("Harika başvuru."));

        verify(committeeDecisionRepository).save(argThat(d ->
                "Harika başvuru.".equals(d.getNotes())));
    }

    // ==========================================
    // FAKÜLTE KURULU KARARI — recordFacultyBoardDecision()
    // ==========================================

    @Test
    void facultyBoardApproves_statusBecomesFinalDeanReview() {
        Application app = buildApp(6, ApplicationStatus.FACULTY_BOARD_REVIEW);
        stubFind(6, app);
        stubGetById(6);

        decisionService.recordFacultyBoardDecision(6, approve(null));

        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.FINAL_DEAN_REVIEW);
        verify(committeeDecisionRepository).save(argThat(d ->
                "FACULTY_BOARD".equals(d.getDecisionBy())));
    }

    @Test
    void facultyBoardRejects_statusBecomesFacultyBoardRejected() {
        Application app = buildApp(7, ApplicationStatus.FACULTY_BOARD_REVIEW);
        stubFind(7, app);
        stubGetById(7);

        decisionService.recordFacultyBoardDecision(7, reject("%80 denklik sağlanamadı."));

        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.FACULTY_BOARD_REJECTED);
    }

    @Test
    void facultyBoardDecision_wrongStatus_throwsIllegalStateException() {
        Application app = buildApp(8, ApplicationStatus.YGK_SCORED);
        stubFindOnly(8, app);

        assertThatThrownBy(() -> decisionService.recordFacultyBoardDecision(8, approve(null)))
                .isInstanceOf(IllegalStateException.class);
    }

    // ==========================================
    // NİHAİ DEKAN KARARI — recordFinalDeanDecision()
    // ==========================================

    @Test
    void finalDeanApproves_statusBecomesResultPublished() {
        Application app = buildApp(9, ApplicationStatus.FINAL_DEAN_REVIEW);
        stubFind(9, app);
        stubGetById(9);

        decisionService.recordFinalDeanDecision(9, approve(null));

        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.RESULT_PUBLISHED);
        verify(committeeDecisionRepository).save(argThat(d ->
                "FINAL_DEAN".equals(d.getDecisionBy())));
    }

    @Test
    void finalDeanRejects_statusBecomesRejected() {
        Application app = buildApp(10, ApplicationStatus.FINAL_DEAN_REVIEW);
        stubFind(10, app);
        stubGetById(10);

        decisionService.recordFinalDeanDecision(10, reject("Nihai red."));

        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.REJECTED);
    }

    @Test
    void finalDeanDecision_wrongStatus_throwsIllegalStateException() {
        Application app = buildApp(11, ApplicationStatus.FACULTY_BOARD_REVIEW);
        stubFindOnly(11, app);

        assertThatThrownBy(() -> decisionService.recordFinalDeanDecision(11, approve(null)))
                .isInstanceOf(IllegalStateException.class);
    }

    // ==========================================
    // GİRİŞ DOĞRULAMA — normalizeDecision()
    // ==========================================

    @Test
    void nullDecision_throwsIllegalArgumentException() {
        Application app = buildApp(12, ApplicationStatus.YGK_SCORED);
        stubFindOnly(12, app);

        assertThatThrownBy(() -> decisionService.recordDeanDecision(12, new DecisionRequest(null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("boş olamaz");
    }

    @Test
    void invalidDecision_throwsIllegalArgumentException() {
        Application app = buildApp(13, ApplicationStatus.YGK_SCORED);
        stubFindOnly(13, app);

        assertThatThrownBy(() -> decisionService.recordDeanDecision(13, new DecisionRequest("MAYBE", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("MAYBE");
    }

    @Test
    void lowercaseDecision_isAccepted() {
        Application app = buildApp(14, ApplicationStatus.YGK_SCORED);
        stubFind(14, app);
        stubGetById(14);

        decisionService.recordDeanDecision(14, new DecisionRequest("approved", null));

        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.FACULTY_BOARD_REVIEW);
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
    void appendOnly_eachDecisionSavesNewRow() {
        Application app = buildApp(15, ApplicationStatus.YGK_SCORED);
        stubFind(15, app);
        stubGetById(15);

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
        return new DecisionRequest("APPROVED", notes);
    }

    private DecisionRequest reject(String notes) {
        return new DecisionRequest("REJECTED", notes);
    }

    private void stubFindOnly(Integer id, Application app) {
        when(applicationRepository.findById(id)).thenReturn(Optional.of(app));
    }

    private void stubFind(Integer id, Application app) {
        when(applicationRepository.findById(id)).thenReturn(Optional.of(app));
        when(applicationRepository.save(any())).thenReturn(app);
        when(committeeDecisionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private void stubGetById(Integer id) {
        when(applicationService.getApplicationById(id))
                .thenReturn(ApplicationResponse.builder().id(id).build());
    }
}
