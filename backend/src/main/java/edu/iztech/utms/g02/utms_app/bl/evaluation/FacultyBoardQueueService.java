package edu.iztech.utms.g02.utms_app.bl.evaluation;

import edu.iztech.utms.g02.utms_app.api.application.dto.ApplicationResponse;
import edu.iztech.utms.g02.utms_app.bl.application.ApplicationService;
import edu.iztech.utms.g02.utms_app.dal.application.entity.ApplicationStatus;
import edu.iztech.utms.g02.utms_app.dal.application.repository.ApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Fakülte Kurulu paneli (UC-11) — fakülte-kapsamlı, salt-okunur liste.
 *
 * <p>Kurul üyesi YALNIZCA kendi fakültesindeki başvuruları görür:
 * <ul>
 *   <li>{@code PENDING} ("Değerlendirme Bekleyenler"): {@code FACULTY_BOARD_REVIEW}</li>
 *   <li>{@code DECIDED} ("Değerlendirilenler"): {@code FACULTY_BOARD_ACCEPTED} + {@code FACULTY_BOARD_REJECTED}</li>
 * </ul>
 * Fakülte kimliği {@link DeanIdentity} (oturum açan personelin {@code facultyId}'si) üzerinden çözülür.
 */
@Service
@RequiredArgsConstructor
public class FacultyBoardQueueService {

    private final ApplicationRepository applicationRepository;
    private final ApplicationService applicationService;     // mevcut ApplicationResponse eşlemesini yeniden kullanır
    private final DeanIdentity facultyIdentity;              // oturum açan personelin facultyId'si (genel)

    /** Fakülte Kurulu sekmeleri → karşılık gelen başvuru statüleri. */
    public enum Queue {
        PENDING(List.of(ApplicationStatus.FACULTY_BOARD_REVIEW)),
        DECIDED(List.of(ApplicationStatus.FACULTY_BOARD_ACCEPTED, ApplicationStatus.FACULTY_BOARD_REJECTED));

        final List<ApplicationStatus> statuses;

        Queue(List<ApplicationStatus> statuses) {
            this.statuses = statuses;
        }
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> list(Queue queue) {
        Integer facultyId = facultyIdentity.currentFacultyId();
        return applicationRepository
                .findByStatusInAndFaculty_FacultyId(queue.statuses, facultyId, Pageable.unpaged())
                .getContent().stream()
                .map(app -> applicationService.getApplicationById(app.getApplicationId()))
                .toList();
    }
}
