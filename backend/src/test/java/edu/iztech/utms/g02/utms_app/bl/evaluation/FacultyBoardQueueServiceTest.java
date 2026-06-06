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
 * Fakülte Kurulu listesi: yalnızca kurulun fakültesindeki başvurular + sekme→statü eşlemesi.
 */
@ExtendWith(MockitoExtension.class)
class FacultyBoardQueueServiceTest {

    @Mock private ApplicationRepository applicationRepository;
    @Mock private ApplicationService applicationService;
    @Mock private DeanIdentity facultyIdentity;

    @InjectMocks private FacultyBoardQueueService service;

    @Test
    void list_pending_scopesToFaculty_andUsesFacultyBoardReviewStatus() {
        when(facultyIdentity.currentFacultyId()).thenReturn(3);
        Application app = Application.builder().applicationId(1).build();
        when(applicationRepository.findByStatusInAndFaculty_FacultyId(
                eq(List.of(ApplicationStatus.FACULTY_BOARD_REVIEW)), eq(3), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(app)));
        when(applicationService.getApplicationById(1)).thenReturn(ApplicationResponse.builder().id(1).build());

        List<ApplicationResponse> result = service.list(FacultyBoardQueueService.Queue.PENDING);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1);
    }

    @Test
    void list_decided_includesAcceptedAndRejected() {
        when(facultyIdentity.currentFacultyId()).thenReturn(3);
        when(applicationRepository.findByStatusInAndFaculty_FacultyId(
                eq(List.of(ApplicationStatus.FACULTY_BOARD_ACCEPTED, ApplicationStatus.FACULTY_BOARD_REJECTED)),
                eq(3), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        List<ApplicationResponse> result = service.list(FacultyBoardQueueService.Queue.DECIDED);

        assertThat(result).isEmpty();
    }

    @Test
    void queueEnum_mapsToExpectedStatuses() {
        assertThat(FacultyBoardQueueService.Queue.PENDING.statuses)
                .containsExactly(ApplicationStatus.FACULTY_BOARD_REVIEW);
        assertThat(FacultyBoardQueueService.Queue.DECIDED.statuses)
                .containsExactly(ApplicationStatus.FACULTY_BOARD_ACCEPTED, ApplicationStatus.FACULTY_BOARD_REJECTED);
    }
}
