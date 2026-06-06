package edu.iztech.utms.g02.utms_app.bl.evaluation;

import edu.iztech.utms.g02.utms_app.api.application.dto.ApplicationResponse;
import edu.iztech.utms.g02.utms_app.api.evaluation.dto.BoardReviewResponse;
import edu.iztech.utms.g02.utms_app.api.evaluation.dto.IntibakTableResponse;
import edu.iztech.utms.g02.utms_app.bl.application.ApplicationService;
import edu.iztech.utms.g02.utms_app.dal.application.entity.Application;
import edu.iztech.utms.g02.utms_app.dal.application.entity.ApplicationStatus;
import edu.iztech.utms.g02.utms_app.dal.application.repository.ApplicationRepository;
import edu.iztech.utms.g02.utms_app.dal.department.entity.Department;
import edu.iztech.utms.g02.utms_app.dal.evaluation.entity.CommitteeDecision;
import edu.iztech.utms.g02.utms_app.dal.evaluation.entity.EvaluationResult;
import edu.iztech.utms.g02.utms_app.dal.evaluation.repository.CommitteeDecisionRepository;
import edu.iztech.utms.g02.utms_app.dal.evaluation.repository.EvaluationResultRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoardReviewServiceTest {

    @Mock private ApplicationService applicationService;
    @Mock private ApplicationRepository applicationRepository;
    @Mock private EvaluationResultRepository evaluationResultRepository;
    @Mock private CommitteeDecisionRepository committeeDecisionRepository;
    @Mock private CourseEquivalencyService courseEquivalencyService;

    @InjectMocks private BoardReviewService boardReviewService;

    @Test
    void getReview_aggregatesApplicationScoreIntibakAndDecisionHistory() {
        Application app = Application.builder()
                .applicationId(5)
                .status(ApplicationStatus.FACULTY_BOARD_REVIEW)
                .ygkApproved(true)
                .build();
        when(applicationRepository.findByApplicationId(5)).thenReturn(Optional.of(app));
        when(applicationService.getApplicationById(5))
                .thenReturn(ApplicationResponse.builder().id(5).studentName("Ali Veli").build());
        when(courseEquivalencyService.getTable(5))
                .thenReturn(IntibakTableResponse.builder()
                        .applicationId(5).conditionsMet(true).equivalencyRatio(0.86).build());
        when(evaluationResultRepository.findByApplication_ApplicationId(5))
                .thenReturn(Optional.of(EvaluationResult.builder()
                        .compositeScore(371.59)
                        .ranking(1)
                        .generalNote("Uygun.")
                        .build()));
        when(committeeDecisionRepository.findByApplication_ApplicationId(5))
                .thenReturn(List.of(
                        decision("YGK", "CONDITIONS_MET", LocalDateTime.of(2026, 6, 1, 9, 0)),
                        decision("DEAN", "APPROVED", LocalDateTime.of(2026, 6, 2, 9, 0))));

        BoardReviewResponse result = boardReviewService.getReview(5);

        assertThat(result.getApplication().getStudentName()).isEqualTo("Ali Veli");
        assertThat(result.getCompositeScore()).isEqualTo(371.59);
        assertThat(result.getRanking()).isEqualTo(1);
        assertThat(result.getConditionsMet()).isTrue();
        assertThat(result.getGeneralNote()).isEqualTo("Uygun.");
        assertThat(result.getIntibak().getApplicationId()).isEqualTo(5);
        // Denklik oranı karardan önce görünür; %80 üstü → eşik altı DEĞİL
        assertThat(result.getEquivalencyRatio()).isEqualTo(0.86);
        assertThat(result.getBelowThreshold()).isFalse();
        // Karar geçmişi eskiden yeniye sıralı
        assertThat(result.getDecisions()).hasSize(2);
        assertThat(result.getDecisions().get(0).getDecisionBy()).isEqualTo("YGK");
        assertThat(result.getDecisions().get(1).getDecisionBy()).isEqualTo("DEAN");
    }

    @Test
    void getReview_noEvaluationYet_scoreFieldsAreNull_butStillReturnsReport() {
        Application app = Application.builder()
                .applicationId(6)
                .status(ApplicationStatus.YGK_SCORED)
                .build();
        when(applicationRepository.findByApplicationId(6)).thenReturn(Optional.of(app));
        when(applicationService.getApplicationById(6)).thenReturn(ApplicationResponse.builder().id(6).build());
        when(courseEquivalencyService.getTable(6)).thenReturn(IntibakTableResponse.builder().applicationId(6).build());
        when(evaluationResultRepository.findByApplication_ApplicationId(6)).thenReturn(Optional.empty());
        when(committeeDecisionRepository.findByApplication_ApplicationId(6)).thenReturn(List.of());

        BoardReviewResponse result = boardReviewService.getReview(6);

        assertThat(result.getCompositeScore()).isNull();
        assertThat(result.getRanking()).isNull();
        assertThat(result.getDecisions()).isEmpty();
        assertThat(result.getApplication().getId()).isEqualTo(6);
    }

    @Test
    void getReview_equivalencyBelow80_flagsBelowThreshold() {
        Application app = Application.builder()
                .applicationId(7)
                .status(ApplicationStatus.FACULTY_BOARD_REVIEW)
                .ygkApproved(true)
                .build();
        when(applicationRepository.findByApplicationId(7)).thenReturn(Optional.of(app));
        when(applicationService.getApplicationById(7)).thenReturn(ApplicationResponse.builder().id(7).build());
        when(courseEquivalencyService.getTable(7))
                .thenReturn(IntibakTableResponse.builder()
                        .applicationId(7).conditionsMet(true).equivalencyRatio(0.50).build());
        when(evaluationResultRepository.findByApplication_ApplicationId(7)).thenReturn(Optional.empty());
        when(committeeDecisionRepository.findByApplication_ApplicationId(7)).thenReturn(List.of());

        BoardReviewResponse result = boardReviewService.getReview(7);

        assertThat(result.getEquivalencyRatio()).isEqualTo(0.50);
        assertThat(result.getBelowThreshold()).isTrue();
    }

    @Test
    void getReview_setsProvisionalWaitlist_whenRankingExceedsQuota() {
        Department dept = Department.builder().departmentId(9).name("Bilgisayar").quota(1).build();
        Application app = Application.builder()
                .applicationId(8)
                .status(ApplicationStatus.FACULTY_BOARD_REVIEW)
                .ygkApproved(true)
                .department(dept)
                .build();
        when(applicationRepository.findByApplicationId(8)).thenReturn(Optional.of(app));
        when(applicationService.getApplicationById(8)).thenReturn(ApplicationResponse.builder().id(8).build());
        when(courseEquivalencyService.getTable(8)).thenReturn(IntibakTableResponse.builder().applicationId(8).build());
        when(evaluationResultRepository.findByApplication_ApplicationId(8))
                .thenReturn(Optional.of(EvaluationResult.builder().ranking(2).build()));
        when(committeeDecisionRepository.findByApplication_ApplicationId(8)).thenReturn(List.of());

        BoardReviewResponse result = boardReviewService.getReview(8);

        assertThat(result.getProvisionalListType()).isEqualTo("WAITLIST");   // ranking 2 > kontenjan 1 → yedek
    }

    @Test
    void getReview_applicationNotFound_throwsEntityNotFound() {
        when(applicationRepository.findByApplicationId(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> boardReviewService.getReview(99))
                .isInstanceOf(EntityNotFoundException.class);
    }

    private CommitteeDecision decision(String by, String decision, LocalDateTime at) {
        CommitteeDecision d = CommitteeDecision.builder()
                .decisionBy(by)
                .decision(decision)
                .build();
        d.setDecidedAt(at);
        return d;
    }
}
