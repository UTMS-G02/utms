package edu.iztech.utms.g02.utms_app.api.evaluation.controller;

import edu.iztech.utms.g02.utms_app.api.application.dto.ApplicationResponse;
import edu.iztech.utms.g02.utms_app.api.evaluation.dto.BoardReviewResponse;
import edu.iztech.utms.g02.utms_app.api.evaluation.dto.DecisionRequest;
import edu.iztech.utms.g02.utms_app.bl.evaluation.BoardReviewService;
import edu.iztech.utms.g02.utms_app.bl.evaluation.DecisionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommitteeReviewControllerTest {

    @Mock private DecisionService decisionService;
    @Mock private BoardReviewService boardReviewService;

    @InjectMocks private CommitteeReviewController controller;

    // ==========================================
    // PATCH karar uçları → DecisionService'e devreder
    // ==========================================

    @Test
    void facultyBoardReview_delegatesToDecisionService() {
        DecisionRequest req = new DecisionRequest(false, "ret", "EX-2");
        ApplicationResponse expected = ApplicationResponse.builder().id(2).build();
        when(decisionService.recordFacultyBoardDecision(eq(2), eq(req))).thenReturn(expected);

        ResponseEntity<ApplicationResponse> response = controller.facultyBoardReview(2, req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(decisionService).recordFacultyBoardDecision(2, req);
    }

    // ==========================================
    // GET /{id}/board-review (UC-11) → BoardReviewService'e devreder
    // ==========================================

    @Test
    void boardReview_delegatesToBoardReviewService() {
        BoardReviewResponse expected = BoardReviewResponse.builder()
                .application(ApplicationResponse.builder().id(5).build())
                .ranking(1)
                .build();
        when(boardReviewService.getReview(eq(5))).thenReturn(expected);

        ResponseEntity<BoardReviewResponse> response = controller.boardReview(5);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getApplication().getId()).isEqualTo(5);
        assertThat(response.getBody().getRanking()).isEqualTo(1);
        verify(boardReviewService).getReview(5);
    }
}
