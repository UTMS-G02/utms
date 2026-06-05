package edu.iztech.utms.g02.utms_app.bl.evaluation;

import edu.iztech.utms.g02.utms_app.api.application.dto.ApplicationResponse;
import edu.iztech.utms.g02.utms_app.bl.application.ApplicationService;
import edu.iztech.utms.g02.utms_app.dal.application.entity.Application;
import edu.iztech.utms.g02.utms_app.dal.application.entity.ApplicationStatus;
import edu.iztech.utms.g02.utms_app.dal.application.repository.ApplicationRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Dekanlık Ofisi = saf YÖNLENDİRİCİ. Karar (onay/ret) vermez; başvuruyu yalnızca
 * bir sonraki makama iletir. Bu yüzden committee_decisions'a hiçbir satır yazılmaz.
 *
 * <p>İletim kuralları (router modeli):
 * <ul>
 *   <li>YGK → Fakülte Kurulu: {@code YGK_REVIEW_DONE → FACULTY_BOARD_REVIEW}</li>
 *   <li>Fakülte Kurulu → ÖİDB: YALNIZCA {@code FACULTY_BOARD_ACCEPTED → OIDB_FINAL_REVIEW}</li>
 * </ul>
 * Dekan yalnızca KENDİ fakültesindeki başvuruyu iletebilir (fakülte sahipliği zorunlu).
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DeanForwardService {

    private final ApplicationRepository applicationRepository;
    private final ApplicationService applicationService;
    private final DeanIdentity deanIdentity;

    /** YGK'dan gelen başvuruyu Fakülte Kurulu'na iletir (karar yok). */
    @Transactional
    public ApplicationResponse forwardToFacultyBoard(Integer applicationId) {
        return forward(applicationId, ApplicationStatus.YGK_REVIEW_DONE, ApplicationStatus.FACULTY_BOARD_REVIEW);
    }

    /** Fakülte Kurulu'nca KABUL edilen başvuruyu ÖİDB'ye (nihai) iletir (karar yok). */
    @Transactional
    public ApplicationResponse forwardToOidb(Integer applicationId) {
        return forward(applicationId, ApplicationStatus.FACULTY_BOARD_ACCEPTED, ApplicationStatus.OIDB_FINAL_REVIEW);
    }

    private ApplicationResponse forward(Integer applicationId, ApplicationStatus required, ApplicationStatus next) {
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new EntityNotFoundException("Başvuru bulunamadı. ID: " + applicationId));

        // Fakülte sahipliği (savunma derinliği): dekan yalnızca kendi fakültesindeki başvuruyu iletir.
        Integer deanFacultyId = deanIdentity.currentFacultyId();
        Integer appFacultyId = app.getFaculty() == null ? null : app.getFaculty().getFacultyId();
        if (!deanFacultyId.equals(appFacultyId)) {
            throw new AccessDeniedException("Bu başvuru sizin fakültenize ait değil.");
        }

        if (app.getStatus() != required) {
            throw new IllegalStateException(
                    "Başvuru bu iletim için uygun aşamada değil. Güncel statü: " + app.getStatus());
        }

        app.setStatus(next);
        applicationRepository.save(app);

        log.info("Dekanlık iletimi: applicationId={}, {} -> {}, facultyId={}",
                applicationId, required, next, deanFacultyId);

        return applicationService.getApplicationById(applicationId);
    }
}
