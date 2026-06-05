package edu.iztech.utms.g02.utms_app.bl.evaluation;

import edu.iztech.utms.g02.utms_app.api.application.dto.ApplicationResponse;
import edu.iztech.utms.g02.utms_app.bl.application.ApplicationService;
import edu.iztech.utms.g02.utms_app.dal.application.entity.Application;
import edu.iztech.utms.g02.utms_app.dal.application.entity.ApplicationStatus;
import edu.iztech.utms.g02.utms_app.dal.application.repository.ApplicationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Dekan listesi: yalnızca dekanın fakültesindeki başvurular + kuyruk→statü eşlemesi.
 */
@ExtendWith(MockitoExtension.class)
class DeanQueueServiceTest {

    @Mock private ApplicationRepository applicationRepository;
    @Mock private ApplicationService applicationService;
    @Mock private DeanIdentity deanIdentity;

    @InjectMocks private DeanQueueService service;

    @Test
    void list_fromOidb_scopesToDeanFaculty_andUsesEvaluationQueueStatus() {
        when(deanIdentity.currentFacultyId()).thenReturn(10);
        Application app = Application.builder().applicationId(1).build();
        when(applicationRepository.findByStatusAndFaculty_FacultyId(
                eq(ApplicationStatus.EVALUATION_QUEUE), eq(10), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(app)));
        when(applicationService.getApplicationById(1)).thenReturn(ApplicationResponse.builder().id(1).build());

        List<ApplicationResponse> result = service.list(DeanQueueService.Queue.FROM_OIDB);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1);
    }

    @Test
    void list_fromFaculty_usesFacultyBoardAcceptedStatus() {
        when(deanIdentity.currentFacultyId()).thenReturn(7);
        when(applicationRepository.findByStatusAndFaculty_FacultyId(
                eq(ApplicationStatus.FACULTY_BOARD_ACCEPTED), eq(7), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        List<ApplicationResponse> result = service.list(DeanQueueService.Queue.FROM_FACULTY);

        assertThat(result).isEmpty();
    }

    @Test
    void queueEnum_mapsToExpectedStatuses() {
        assertThat(DeanQueueService.Queue.FROM_OIDB.status).isEqualTo(ApplicationStatus.EVALUATION_QUEUE);
        assertThat(DeanQueueService.Queue.FROM_YGK.status).isEqualTo(ApplicationStatus.YGK_REVIEW_DONE);
        assertThat(DeanQueueService.Queue.FROM_FACULTY.status).isEqualTo(ApplicationStatus.FACULTY_BOARD_ACCEPTED);
    }
}
