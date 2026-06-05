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

    /** Dekanlık sekmeleri → karşılık gelen başvuru statüsü. */
    public enum Queue {
        FROM_OIDB(ApplicationStatus.EVALUATION_QUEUE),              // ÖİDB'den gelen (YGK'ya iletilecek — kozmetik)
        FROM_YGK(ApplicationStatus.YGK_REVIEW_DONE),                // YGK'dan gelen (intibak görüntülenebilir)
        FROM_FACULTY(ApplicationStatus.FACULTY_BOARD_ACCEPTED),     // Fakülte Kurulu KABUL (ÖİDB'ye iletilecek)
        FROM_FACULTY_REJECTED(ApplicationStatus.FACULTY_BOARD_REJECTED); // Fakülte Kurulu RED (YGK'ya geri iletilecek)

        final ApplicationStatus status;

        Queue(ApplicationStatus status) {
            this.status = status;
        }
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> list(Queue queue) {
        Integer facultyId = deanIdentity.currentFacultyId();
        return applicationRepository
                .findByStatusAndFaculty_FacultyId(queue.status, facultyId, Pageable.unpaged())
                .getContent().stream()
                .map(app -> applicationService.getApplicationById(app.getApplicationId()))
                .toList();
    }
}
