package edu.iztech.utms.g02.utms_app.bl.evaluation;

import edu.iztech.utms.g02.utms_app.api.application.dto.ApplicationResponse;
import edu.iztech.utms.g02.utms_app.bl.application.ApplicationService;
import edu.iztech.utms.g02.utms_app.dal.application.entity.Application;
import edu.iztech.utms.g02.utms_app.dal.application.entity.ApplicationStatus;
import edu.iztech.utms.g02.utms_app.dal.application.repository.ApplicationRepository;
import edu.iztech.utms.g02.utms_app.dal.department.entity.Faculty;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Dekan = saf yönlendirici: karar yok, yalnızca statü geçişi + fakülte sahipliği kontrolü.
 */
@ExtendWith(MockitoExtension.class)
class DeanForwardServiceTest {

    @Mock private ApplicationRepository applicationRepository;
    @Mock private ApplicationService applicationService;
    @Mock private DeanIdentity deanIdentity;

    @InjectMocks private DeanForwardService service;

    @Test
    void forwardToFacultyBoard_ygkReviewDone_sameFaculty_movesToFacultyBoardReview() {
        Application app = buildApp(1, ApplicationStatus.YGK_REVIEW_DONE, 10);
        stubForward(1, app, 10);

        service.forwardToFacultyBoard(1);

        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.FACULTY_BOARD_REVIEW);
        verify(applicationRepository).save(app);
    }

    @Test
    void forwardToFacultyBoard_wrongStatus_throwsIllegalState_andSavesNothing() {
        Application app = buildApp(2, ApplicationStatus.YGK_SCORED, 10);
        when(applicationRepository.findById(2)).thenReturn(Optional.of(app));
        when(deanIdentity.currentFacultyId()).thenReturn(10);

        assertThatThrownBy(() -> service.forwardToFacultyBoard(2))
                .isInstanceOf(IllegalStateException.class);
        verify(applicationRepository, never()).save(any());
    }

    @Test
    void forward_differentFaculty_throwsAccessDenied_andSavesNothing() {
        Application app = buildApp(3, ApplicationStatus.YGK_REVIEW_DONE, 99); // başvuru fakülte 99
        when(applicationRepository.findById(3)).thenReturn(Optional.of(app));
        when(deanIdentity.currentFacultyId()).thenReturn(10);                 // dekan fakülte 10

        assertThatThrownBy(() -> service.forwardToFacultyBoard(3))
                .isInstanceOf(AccessDeniedException.class);
        verify(applicationRepository, never()).save(any());
    }

    @Test
    void forwardToOidb_facultyBoardAccepted_sameFaculty_movesToOidbFinalReview() {
        Application app = buildApp(4, ApplicationStatus.FACULTY_BOARD_ACCEPTED, 10);
        stubForward(4, app, 10);

        service.forwardToOidb(4);

        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.OIDB_FINAL_REVIEW);
    }

    @Test
    void forwardToOidb_notAccepted_throwsIllegalState() {
        Application app = buildApp(5, ApplicationStatus.FACULTY_BOARD_REVIEW, 10);
        when(applicationRepository.findById(5)).thenReturn(Optional.of(app));
        when(deanIdentity.currentFacultyId()).thenReturn(10);

        assertThatThrownBy(() -> service.forwardToOidb(5))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void forwardToOidb_facultyBoardRejected_sameFaculty_movesToRejected() {
        Application app = buildApp(6, ApplicationStatus.FACULTY_BOARD_REJECTED, 10);
        stubForward(6, app, 10);

        service.forwardToOidb(6);

        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.REJECTED);
    }

    @Test
    void applicationNotFound_throwsEntityNotFound() {
        when(applicationRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.forwardToFacultyBoard(99))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ==========================================
    // YARDIMCI METOTLAR
    // ==========================================

    private Application buildApp(Integer id, ApplicationStatus status, Integer facultyId) {
        return Application.builder()
                .applicationId(id)
                .status(status)
                .faculty(Faculty.builder().facultyId(facultyId).name("Fakülte-" + facultyId).build())
                .build();
    }

    private void stubForward(Integer id, Application app, Integer deanFacultyId) {
        when(applicationRepository.findById(id)).thenReturn(Optional.of(app));
        when(deanIdentity.currentFacultyId()).thenReturn(deanFacultyId);
        when(applicationRepository.save(any())).thenReturn(app);
        when(applicationService.getApplicationById(id)).thenReturn(ApplicationResponse.builder().id(id).build());
    }
}
