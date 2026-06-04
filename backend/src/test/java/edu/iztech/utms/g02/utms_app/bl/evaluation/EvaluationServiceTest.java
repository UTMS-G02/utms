package edu.iztech.utms.g02.utms_app.bl.evaluation;

import edu.iztech.utms.g02.utms_app.api.evaluation.dto.EvaluationResponse;
import edu.iztech.utms.g02.utms_app.api.evaluation.dto.YgkEvaluationRequest;
import edu.iztech.utms.g02.utms_app.dal.application.entity.Application;
import edu.iztech.utms.g02.utms_app.dal.application.entity.ApplicationStatus;
import edu.iztech.utms.g02.utms_app.dal.application.repository.ApplicationRepository;
import edu.iztech.utms.g02.utms_app.dal.evaluation.entity.CommitteeDecision;
import edu.iztech.utms.g02.utms_app.dal.evaluation.entity.EvaluationResult;
import edu.iztech.utms.g02.utms_app.dal.evaluation.repository.CommitteeDecisionRepository;
import edu.iztech.utms.g02.utms_app.dal.evaluation.repository.EvaluationResultRepository;
import edu.iztech.utms.g02.utms_app.dal.user.entity.Student;
import edu.iztech.utms.g02.utms_app.dal.user.entity.UserRole;
import jakarta.persistence.EntityNotFoundException;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EvaluationServiceTest {

    @Mock private ApplicationRepository applicationRepository;
    @Mock private EvaluationResultRepository evaluationResultRepository;
    @Mock private CommitteeDecisionRepository committeeDecisionRepository;

    @InjectMocks private EvaluationService evaluationService;

    // ==========================================
    // HANDOFF + SKORLAMA
    // ==========================================

    @Test
    void scoreAll_picksUpEvaluationQueue_scoresAndMarksYgkScored() {
        Application queued = buildApplication(1, ApplicationStatus.EVALUATION_QUEUE, 3.0, 400.0); // 0.30 + 360 = 360.30

        when(applicationRepository.findByStatus(eq(ApplicationStatus.EVALUATION_QUEUE), any(Pageable.class)))
                .thenReturn(page(queued));
        when(evaluationResultRepository.findByApplication_ApplicationId(anyInt())).thenReturn(Optional.empty());
        when(evaluationResultRepository.save(any(EvaluationResult.class))).thenAnswer(inv -> inv.getArgument(0));
        when(evaluationResultRepository.findAll()).thenReturn(List.of());

        int count = evaluationService.scoreAllPendingApplications();

        assertThat(count).isEqualTo(1);
        assertThat(queued.getStatus()).isEqualTo(ApplicationStatus.YGK_SCORED);
        assertThat(queued.getCompositeScore()).isCloseTo(360.30, Offset.offset(0.001));
    }

    @Test
    void scoreAll_assignsRankingsHighestScoreFirst() {
        when(applicationRepository.findByStatus(any(ApplicationStatus.class), any(Pageable.class)))
                .thenReturn(page()); // hiç bekleyen yok; sadece sıralama mantığını test ediyoruz

        EvaluationResult high = result(buildApplication(1, ApplicationStatus.YGK_SCORED, 0, 0), 360.30);
        EvaluationResult low = result(buildApplication(2, ApplicationStatus.YGK_SCORED, 0, 0), 180.40);
        when(evaluationResultRepository.findAll()).thenReturn(List.of(low, high)); // "yanlış" sırada
        when(evaluationResultRepository.save(any(EvaluationResult.class))).thenAnswer(inv -> inv.getArgument(0));

        evaluationService.scoreAllPendingApplications();

        assertThat(high.getRanking()).isEqualTo(1);
        assertThat(low.getRanking()).isEqualTo(2);
    }

    // ==========================================
    // TIE-BREAK (eşit skor) — TestReport PRE-1
    // ==========================================

    @Test
    void scoreAll_tieOnScore_brokenByEarlierCreatedAt_overridesApplicationId() {
        // Aynı skor, submissionDate yok. Erken createdAt önce gelmeli — id sırasına RAĞMEN.
        Application earlier = buildApplication(2, ApplicationStatus.YGK_SCORED, 3.0, 400.0); // büyük id
        earlier.setCreatedAt(LocalDateTime.of(2026, 5, 1, 9, 0));
        Application later = buildApplication(1, ApplicationStatus.YGK_SCORED, 3.0, 400.0);   // küçük id
        later.setCreatedAt(LocalDateTime.of(2026, 5, 10, 9, 0));

        EvaluationResult rEarlier = result(earlier, 360.30);
        EvaluationResult rLater = result(later, 360.30);

        when(applicationRepository.findByStatus(any(ApplicationStatus.class), any(Pageable.class))).thenReturn(page());
        when(evaluationResultRepository.findAll()).thenReturn(List.of(rLater, rEarlier));
        when(evaluationResultRepository.save(any(EvaluationResult.class))).thenAnswer(inv -> inv.getArgument(0));

        evaluationService.scoreAllPendingApplications();

        assertThat(rEarlier.getRanking()).isEqualTo(1); // erken başvuran önce
        assertThat(rLater.getRanking()).isEqualTo(2);
    }

    @Test
    void scoreAll_tieOnScore_submissionDateTakesPrecedenceOverCreatedAt() {
        // submissionDate spec alanı; createdAt'ten önceliklidir.
        Application a = buildApplication(1, ApplicationStatus.YGK_SCORED, 3.0, 400.0);
        a.setSubmissionDate(LocalDate.of(2026, 5, 1));   // erken submission
        a.setCreatedAt(LocalDateTime.of(2026, 5, 10, 9, 0)); // ama geç created
        Application b = buildApplication(2, ApplicationStatus.YGK_SCORED, 3.0, 400.0);
        b.setSubmissionDate(LocalDate.of(2026, 5, 5));
        b.setCreatedAt(LocalDateTime.of(2026, 5, 1, 9, 0));

        EvaluationResult ra = result(a, 360.30);
        EvaluationResult rb = result(b, 360.30);

        when(applicationRepository.findByStatus(any(ApplicationStatus.class), any(Pageable.class))).thenReturn(page());
        when(evaluationResultRepository.findAll()).thenReturn(List.of(rb, ra));
        when(evaluationResultRepository.save(any(EvaluationResult.class))).thenAnswer(inv -> inv.getArgument(0));

        evaluationService.scoreAllPendingApplications();

        assertThat(ra.getRanking()).isEqualTo(1); // erken submissionDate kazanır
        assertThat(rb.getRanking()).isEqualTo(2);
    }

    // ==========================================
    // LİSTELEME
    // ==========================================

    @Test
    void getEvaluations_mapsResultToResponse() {
        Application app = buildApplication(7, ApplicationStatus.YGK_SCORED, 3.5, 400.0);
        app.setStudent(buildStudent());
        EvaluationResult result = EvaluationResult.builder()
                .application(app)
                .compositeScore(360.35)
                .ranking(1)
                .build();
        when(evaluationResultRepository.findAll()).thenReturn(List.of(result));

        List<EvaluationResponse> responses = evaluationService.getEvaluations();

        assertThat(responses).hasSize(1);
        EvaluationResponse response = responses.get(0);
        assertThat(response.getApplicationId()).isEqualTo(7);
        assertThat(response.getStudentName()).isEqualTo("Test Student");
        assertThat(response.getCompositeScore()).isEqualTo(360.35);
        assertThat(response.getRanking()).isEqualTo(1);
        assertThat(response.getStatus()).isEqualTo(ApplicationStatus.YGK_SCORED);
    }

    // ==========================================
    // UC-8 — TEK BAŞVURU DEĞERLENDİRME (submitEvaluation)
    // ==========================================

    @Test
    void submit_ygkScored_setsReviewDone_storesGeneralNote_andAuditRow() {
        Application app = buildApplication(20, ApplicationStatus.YGK_SCORED, 3.5, 400.0);
        app.setStudent(buildStudent());
        EvaluationResult result = EvaluationResult.builder()
                .application(app).compositeScore(360.35).ranking(1).build();

        when(applicationRepository.findByApplicationId(20)).thenReturn(Optional.of(app));
        when(evaluationResultRepository.findByApplication_ApplicationId(20)).thenReturn(Optional.of(result));
        when(evaluationResultRepository.save(any(EvaluationResult.class))).thenAnswer(inv -> inv.getArgument(0));
        when(applicationRepository.save(any(Application.class))).thenReturn(app);
        when(committeeDecisionRepository.save(any(CommitteeDecision.class))).thenAnswer(inv -> inv.getArgument(0));

        EvaluationResponse response =
                evaluationService.submitEvaluation(20, new YgkEvaluationRequest(true, "Başarılı öğrenci."));

        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.YGK_REVIEW_DONE);
        assertThat(app.getYgkApproved()).isTrue();
        assertThat(app.getYgkReviewedDate()).isNotNull();
        assertThat(result.getGeneralNote()).isEqualTo("Başarılı öğrenci.");
        assertThat(response.getConditionsMet()).isTrue();
        assertThat(response.getGeneralNote()).isEqualTo("Başarılı öğrenci.");

        ArgumentCaptor<CommitteeDecision> captor = ArgumentCaptor.forClass(CommitteeDecision.class);
        verify(committeeDecisionRepository).save(captor.capture());
        assertThat(captor.getValue().getDecisionBy()).isEqualTo("YGK");
        assertThat(captor.getValue().getDecision()).isEqualTo("CONDITIONS_MET");
        assertThat(captor.getValue().getNotes()).isEqualTo("Başarılı öğrenci.");
    }

    @Test
    void submit_wrongStatus_throwsIllegalState_andSavesNothing() {
        Application app = buildApplication(21, ApplicationStatus.EVALUATION_QUEUE, 3.0, 400.0);
        when(applicationRepository.findByApplicationId(21)).thenReturn(Optional.of(app));

        assertThatThrownBy(() ->
                evaluationService.submitEvaluation(21, new YgkEvaluationRequest(true, "Not.")))
                .isInstanceOf(IllegalStateException.class);

        verify(evaluationResultRepository, never()).save(any());
        verify(committeeDecisionRepository, never()).save(any());
    }

    @Test
    void submit_applicationNotFound_throwsEntityNotFound() {
        when(applicationRepository.findByApplicationId(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                evaluationService.submitEvaluation(99, new YgkEvaluationRequest(true, "Not.")))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void submit_nullConditionsMet_throwsIllegalArgument() {
        assertThatThrownBy(() ->
                evaluationService.submitEvaluation(1, new YgkEvaluationRequest(null, "Not.")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void submit_blankGeneralNote_throwsIllegalArgument() {
        assertThatThrownBy(() ->
                evaluationService.submitEvaluation(1, new YgkEvaluationRequest(true, "   ")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ==========================================
    // YARDIMCI METOTLAR
    // ==========================================

    private Page<Application> page(Application... apps) {
        return new PageImpl<>(List.of(apps));
    }

    private EvaluationResult result(Application app, double score) {
        return EvaluationResult.builder().application(app).compositeScore(score).build();
    }

    private Application buildApplication(Integer id, ApplicationStatus status, double gpa, double yks) {
        return Application.builder()
                .applicationId(id)
                .status(status)
                .gpa(gpa)
                .sayYksScore(yks)
                .build();
    }

    private Student buildStudent() {
        Student student = Student.builder()
                .email("student@iyte.edu.tr")
                .firstName("Test")
                .lastName("Student")
                .role(UserRole.STUDENT)
                .build();
        student.setUserId(7);
        return student;
    }
}
