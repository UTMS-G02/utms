package edu.iztech.utms.g02.utms_app.api.evaluation.controller;

import edu.iztech.utms.g02.utms_app.api.evaluation.dto.EvaluationResponse;
import edu.iztech.utms.g02.utms_app.api.evaluation.dto.YgkEvaluationRequest;
import edu.iztech.utms.g02.utms_app.bl.evaluation.EvaluationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EvaluationControllerTest {

    @Mock private EvaluationService evaluationService;

    @InjectMocks private EvaluationController evaluationController;

    // ==========================================
    // GET /api/evaluations
    // ==========================================

    @Test
    void getEvaluations_delegatesToService_returnsList() {
        List<EvaluationResponse> rows = List.of(
                EvaluationResponse.builder().applicationId(1).ranking(1).build(),
                EvaluationResponse.builder().applicationId(2).ranking(2).build()
        );
        when(evaluationService.getEvaluations()).thenReturn(rows);

        ResponseEntity<List<EvaluationResponse>> response = evaluationController.getEvaluations();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
        assertThat(response.getBody().get(0).getRanking()).isEqualTo(1);
        verify(evaluationService).getEvaluations();
    }

    @Test
    void getEvaluations_emptyList_returns200() {
        when(evaluationService.getEvaluations()).thenReturn(List.of());

        ResponseEntity<List<EvaluationResponse>> response = evaluationController.getEvaluations();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    // ==========================================
    // POST /api/evaluations/score-all
    // ==========================================

    @Test
    void scoreAll_returnsCountInTurkishMessage() {
        when(evaluationService.scoreAllPendingApplications()).thenReturn(3);

        ResponseEntity<String> response = evaluationController.scoreAll();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("3");
        assertThat(response.getBody()).contains("skorlandı ve sıralandı");
        verify(evaluationService).scoreAllPendingApplications();
    }

    @Test
    void scoreAll_zero_returnsZeroInMessage() {
        when(evaluationService.scoreAllPendingApplications()).thenReturn(0);

        ResponseEntity<String> response = evaluationController.scoreAll();

        assertThat(response.getBody()).startsWith("0");
    }

    // ==========================================
    // POST /api/evaluations/{id}/submit  (UC-8)
    // ==========================================

    @Test
    void submit_delegatesToService_withIdAndBody() {
        YgkEvaluationRequest req = new YgkEvaluationRequest();
        EvaluationResponse expected = EvaluationResponse.builder().applicationId(7).build();
        when(evaluationService.submitEvaluation(eq(7), eq(req))).thenReturn(expected);

        ResponseEntity<EvaluationResponse> response = evaluationController.submitEvaluation(7, req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getApplicationId()).isEqualTo(7);
        verify(evaluationService).submitEvaluation(7, req);
    }
}
