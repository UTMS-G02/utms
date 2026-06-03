package edu.iztech.utms.g02.utms_app.bl.evaluation;

import edu.iztech.utms.g02.utms_app.api.evaluation.dto.EvaluationResponse;
import edu.iztech.utms.g02.utms_app.dal.application.entity.Application;
import edu.iztech.utms.g02.utms_app.dal.application.entity.ApplicationStatus;
import edu.iztech.utms.g02.utms_app.dal.application.repository.ApplicationRepository;
import edu.iztech.utms.g02.utms_app.dal.evaluation.entity.EvaluationResult;
import edu.iztech.utms.g02.utms_app.dal.evaluation.repository.EvaluationResultRepository;
import edu.iztech.utms.g02.utms_app.dal.user.entity.Student;
import edu.iztech.utms.g02.utms_app.dal.user.entity.UserRole;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EvaluationServiceTest {

    @Mock private ApplicationRepository applicationRepository;
    @Mock private EvaluationResultRepository evaluationResultRepository;

    @InjectMocks private EvaluationService evaluationService;

    // ==========================================
    // HANDOFF + SKORLAMA
    // ==========================================

    @Test
    void scoreAll_picksUpEvaluationQueue_scoresAndMarksYgkScored() {
        // Tek giriş statüsü: EVALUATION_QUEUE. Pair 2, OİDB son onayında başvuruyu bu
        // statüye taşır (processOidbPostYdyoReview).
        Application queued = buildApplication(1, ApplicationStatus.EVALUATION_QUEUE, 3.0, 400.0);  // 0.30 + 360 = 360.30

        when(applicationRepository.findByStatus(eq(ApplicationStatus.EVALUATION_QUEUE), any(Pageable.class)))
                .thenReturn(page(queued));
        when(evaluationResultRepository.findByApplication_ApplicationId(anyInt())).thenReturn(Optional.empty());
        when(evaluationResultRepository.save(any(EvaluationResult.class))).thenAnswer(inv -> inv.getArgument(0));
        when(evaluationResultRepository.findAllByOrderByCompositeScoreDesc()).thenReturn(List.of());

        int count = evaluationService.scoreAllPendingApplications();

        assertThat(count).isEqualTo(1);
        assertThat(queued.getStatus()).isEqualTo(ApplicationStatus.YGK_SCORED);
        assertThat(queued.getCompositeScore()).isCloseTo(360.30, Offset.offset(0.001));
    }

    @Test
    void scoreAll_assignsRankingsHighestScoreFirst() {
        when(applicationRepository.findByStatus(any(ApplicationStatus.class), any(Pageable.class)))
                .thenReturn(page()); // hiç bekleyen yok; sadece sıralama mantığını test ediyoruz

        EvaluationResult high = EvaluationResult.builder().compositeScore(360.30).build();
        EvaluationResult low = EvaluationResult.builder().compositeScore(180.40).build();
        when(evaluationResultRepository.findAllByOrderByCompositeScoreDesc()).thenReturn(List.of(high, low));
        when(evaluationResultRepository.save(any(EvaluationResult.class))).thenAnswer(inv -> inv.getArgument(0));

        evaluationService.scoreAllPendingApplications();

        assertThat(high.getRanking()).isEqualTo(1);
        assertThat(low.getRanking()).isEqualTo(2);
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
        when(evaluationResultRepository.findAllByOrderByCompositeScoreDesc()).thenReturn(List.of(result));

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
    // YARDIMCI METOTLAR
    // ==========================================

    private Page<Application> page(Application... apps) {
        return new PageImpl<>(List.of(apps));
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
