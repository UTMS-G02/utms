package edu.iztech.utms.g02.utms_app.bl.evaluation;

import edu.iztech.utms.g02.utms_app.api.application.dto.ApplicationResponse;
import edu.iztech.utms.g02.utms_app.api.evaluation.dto.BatchForwardRequest;
import edu.iztech.utms.g02.utms_app.api.evaluation.dto.BatchForwardResponse;
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

import java.util.List;
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

    @Mock private edu.iztech.utms.g02.utms_app.bl.audit.AuditService auditService;

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
    void forwardToYgk_deanOfficeReview_sameFaculty_movesToEvaluationQueue() {
        Application app = buildApp(9, ApplicationStatus.DEAN_OFFICE_REVIEW, 10);
        stubForward(9, app, 10);

        service.forwardToYgk(9);

        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.EVALUATION_QUEUE);
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
    // TC-10.7 — TOPLU İLETİM — batchForward()
    // ==========================================

    @Test
    void batchForward_toYgk_allSameFaculty_forwardsAll() {
        Application app1 = buildApp(10, ApplicationStatus.DEAN_OFFICE_REVIEW, 5);
        Application app2 = buildApp(11, ApplicationStatus.DEAN_OFFICE_REVIEW, 5);
        Application app3 = buildApp(12, ApplicationStatus.DEAN_OFFICE_REVIEW, 5);
        stubForward(10, app1, 5);
        stubForward(11, app2, 5);
        stubForward(12, app3, 5);

        BatchForwardResponse response = service.batchForward(
                new BatchForwardRequest(List.of(10, 11, 12), BatchForwardRequest.Action.TO_YGK));

        assertThat(response.getForwarded()).isEqualTo(3);
        assertThat(app1.getStatus()).isEqualTo(ApplicationStatus.EVALUATION_QUEUE);
        assertThat(app2.getStatus()).isEqualTo(ApplicationStatus.EVALUATION_QUEUE);
        assertThat(app3.getStatus()).isEqualTo(ApplicationStatus.EVALUATION_QUEUE);
    }

    @Test
    void batchForward_toFacultyBoard_allSameFaculty_forwardsAll() {
        Application app1 = buildApp(20, ApplicationStatus.YGK_REVIEW_DONE, 5);
        Application app2 = buildApp(21, ApplicationStatus.YGK_REVIEW_DONE, 5);
        stubForward(20, app1, 5);
        stubForward(21, app2, 5);

        BatchForwardResponse response = service.batchForward(
                new BatchForwardRequest(List.of(20, 21), BatchForwardRequest.Action.TO_FACULTY_BOARD));

        assertThat(response.getForwarded()).isEqualTo(2);
        assertThat(app1.getStatus()).isEqualTo(ApplicationStatus.FACULTY_BOARD_REVIEW);
        assertThat(app2.getStatus()).isEqualTo(ApplicationStatus.FACULTY_BOARD_REVIEW);
    }

    @Test
    void batchForward_oneAppWrongStatus_throwsAndNothingForwarded() {
        Application app1 = buildApp(30, ApplicationStatus.DEAN_OFFICE_REVIEW, 5);
        Application app2 = buildApp(31, ApplicationStatus.YGK_SCORED, 5); // wrong status
        stubForward(30, app1, 5);
        when(applicationRepository.findById(31)).thenReturn(Optional.of(app2));
        when(deanIdentity.currentFacultyId()).thenReturn(5);

        assertThatThrownBy(() -> service.batchForward(
                new BatchForwardRequest(List.of(30, 31), BatchForwardRequest.Action.TO_YGK)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void batchForward_emptyList_returnsZero() {
        BatchForwardResponse response = service.batchForward(
                new BatchForwardRequest(List.of(), BatchForwardRequest.Action.TO_YGK));

        assertThat(response.getForwarded()).isEqualTo(0);
    }

    @Test
    void batchForward_toOidb_facultyBoardAccepted_forwardsAll() {
        Application app1 = buildApp(40, ApplicationStatus.FACULTY_BOARD_ACCEPTED, 5);
        Application app2 = buildApp(41, ApplicationStatus.FACULTY_BOARD_REJECTED, 5);
        stubForward(40, app1, 5);
        stubForward(41, app2, 5);

        BatchForwardResponse response = service.batchForward(
                new BatchForwardRequest(List.of(40, 41), BatchForwardRequest.Action.TO_OIDB));

        assertThat(response.getForwarded()).isEqualTo(2);
        assertThat(app1.getStatus()).isEqualTo(ApplicationStatus.OIDB_FINAL_REVIEW);
        assertThat(app2.getStatus()).isEqualTo(ApplicationStatus.REJECTED);
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
