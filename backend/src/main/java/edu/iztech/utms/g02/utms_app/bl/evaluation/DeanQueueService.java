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
 * Dekanlık Ofisi "router" görünümü — fakülte-kapsamlı, salt-okunur.
 *
 * <p>Dekanlık Ofisi karar VERMEZ; yalnızca başvuruları bir sonraki makama iletir. Bu servis,
 * panelin üç sekmesini (ÖİDB'den / YGK'dan / Fakülte Kurulu'ndan gelen) besleyen listeleri
 * döndürür ve YALNIZCA oturum açan dekanın {@code facultyId}'sine ait başvuruları gösterir.
 */
@Service
@RequiredArgsConstructor
public class DeanQueueService {

    private final ApplicationRepository applicationRepository;
    private final ApplicationService applicationService; // mevcut ApplicationResponse eşlemesini yeniden kullanır
    private final DeanIdentity deanIdentity;

    /** Dekanlık sekmeleri → karşılık gelen başvuru statüleri (TC-10.x). */
    public enum Queue {
        FROM_OIDB(List.of(ApplicationStatus.EVALUATION_QUEUE)),   // ÖİDB'den gelen (YGK'ya iletilecek — kozmetik)
        FROM_YGK(List.of(ApplicationStatus.YGK_REVIEW_DONE)),     // YGK'dan gelen (intibak görüntülenebilir)
        // Fakülte Kurulu'ndan gelen: hem KABUL hem RED tek sekmede; ikisi de ÖİDB'ye iletilir (TC-10.3/10.4).
        FROM_FACULTY(List.of(ApplicationStatus.FACULTY_BOARD_ACCEPTED, ApplicationStatus.FACULTY_BOARD_REJECTED));

        final List<ApplicationStatus> statuses;

        Queue(List<ApplicationStatus> statuses) {
            this.statuses = statuses;
        }
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> list(Queue queue) {
        Integer facultyId = deanIdentity.currentFacultyId();
        return applicationRepository
                .findByStatusInAndFaculty_FacultyId(queue.statuses, facultyId, Pageable.unpaged())
                .getContent().stream()
                .map(app -> applicationService.getApplicationById(app.getApplicationId()))
                .toList();
    }
}
