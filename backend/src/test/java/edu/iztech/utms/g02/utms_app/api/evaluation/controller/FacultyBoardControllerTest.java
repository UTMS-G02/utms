package edu.iztech.utms.g02.utms_app.api.evaluation.controller;

import edu.iztech.utms.g02.utms_app.api.evaluation.dto.RejectionReason;
import edu.iztech.utms.g02.utms_app.api.evaluation.dto.RejectionReasonResponse;
import edu.iztech.utms.g02.utms_app.bl.evaluation.FacultyBoardQueueService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class FacultyBoardControllerTest {

    @Mock private FacultyBoardQueueService facultyBoardQueueService;

    @InjectMocks private FacultyBoardController controller;

    @Test
    void rejectionReasons_returnsAllPredefinedReasons() {
        var response = controller.rejectionReasons();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<RejectionReasonResponse> body = response.getBody();
        assertThat(body).hasSize(RejectionReason.values().length);
        assertThat(body.stream().map(RejectionReasonResponse::getCode))
                .contains("EQUIVALENCY_BELOW_80", "OTHER");
        assertThat(body.get(0).getLabel()).isNotBlank();
    }
}
