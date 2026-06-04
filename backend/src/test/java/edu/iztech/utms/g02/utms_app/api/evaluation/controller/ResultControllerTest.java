package edu.iztech.utms.g02.utms_app.api.evaluation.controller;

import edu.iztech.utms.g02.utms_app.api.evaluation.dto.PublishedResultResponse;
import edu.iztech.utms.g02.utms_app.bl.evaluation.ResultService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResultControllerTest {

    @Mock private ResultService resultService;

    @InjectMocks private ResultController resultController;

    // ==========================================
    // POST /api/results/publish
    // ==========================================

    @Test
    void publish_delegatesToService_returnsCountMessage() {
        when(resultService.publishResults()).thenReturn(5);

        ResponseEntity<String> response = resultController.publish();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("5");
        assertThat(response.getBody()).contains("sonuç yayınlandı");
        verify(resultService).publishResults();
    }

    @Test
    void publish_zeroResults_returnsZeroInMessage() {
        when(resultService.publishResults()).thenReturn(0);

        ResponseEntity<String> response = resultController.publish();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).startsWith("0");
    }

    @Test
    void publish_multipleResults_countReflectedInMessage() {
        when(resultService.publishResults()).thenReturn(42);

        ResponseEntity<String> response = resultController.publish();

        assertThat(response.getBody()).contains("42");
    }

    // ==========================================
    // GET /api/results
    // ==========================================

    @Test
    void getResults_delegatesToService_returnsList() {
        List<PublishedResultResponse> results = List.of(
                PublishedResultResponse.builder().applicationId(1).finalDecision("ACCEPTED").build(),
                PublishedResultResponse.builder().applicationId(2).finalDecision("REJECTED").build()
        );
        when(resultService.getResults()).thenReturn(results);

        ResponseEntity<List<PublishedResultResponse>> response = resultController.getResults();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
        assertThat(response.getBody().get(0).getFinalDecision()).isEqualTo("ACCEPTED");
        assertThat(response.getBody().get(1).getFinalDecision()).isEqualTo("REJECTED");
        verify(resultService).getResults();
    }

    @Test
    void getResults_emptyList_returnsEmptyListWith200() {
        when(resultService.getResults()).thenReturn(List.of());

        ResponseEntity<List<PublishedResultResponse>> response = resultController.getResults();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }
}
