package edu.iztech.utms.g02.utms_app.bl.evaluation;

import edu.iztech.utms.g02.utms_app.api.application.dto.ApplicationResponse;
import edu.iztech.utms.g02.utms_app.api.evaluation.dto.BatchForwardRequest;
import edu.iztech.utms.g02.utms_app.api.evaluation.dto.BatchForwardResponse;
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
 * Dekanlık Ofisi = saf YÖNLENDİRİCİ. Karar (onay/ret) vermez; başvuruyu bir sonraki makama iletir.
 *
 * <p>İletim kuralları (test raporu UC-10):
 * <ul>
 *   <li>YGK → Fakülte Kurulu (TC-10.2): {@code YGK_REVIEW_DONE → FACULTY_BOARD_REVIEW}</li>
 *   <li>Fakülte Kurulu → ÖİDB (TC-10.3 / TC-10.4): KABUL {@code FACULTY_BOARD_ACCEPTED → OIDB_FINAL_REVIEW},
 *       RED {@code FACULTY_BOARD_REJECTED → REJECTED}. Red TERMINAL'dir (YGK'ya geri DÖNMEZ); öğrenci
 *       ÖİDB yayınıyla bilgilendirilir.</li>
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

    /** ÖİDB'den gelen başvuruyu (ilgili bölümün) YGK'sına iletir — TC-10.0 (karar yok). */
    @Transactional
    public ApplicationResponse forwardToYgk(Integer applicationId) {
        return forward(applicationId, ApplicationStatus.DEAN_OFFICE_REVIEW, ApplicationStatus.EVALUATION_QUEUE);
    }

    /** YGK'dan gelen başvuruyu Fakülte Kurulu'na iletir (karar yok). */
    @Transactional
    public ApplicationResponse forwardToFacultyBoard(Integer applicationId) {
        return forward(applicationId, ApplicationStatus.YGK_REVIEW_DONE, ApplicationStatus.FACULTY_BOARD_REVIEW);
    }

    /**
     * Fakülte Kurulu kararını ÖİDB'ye (nihai) iletir — hem kabul hem red için (TC-10.3 / TC-10.4):
     * KABUL → {@code OIDB_FINAL_REVIEW} (ÖİDB ACCEPTED yayınlar); RED → {@code REJECTED} (ÖİDB REJECTED yayınlar).
     */
    @Transactional
    public ApplicationResponse forwardToOidb(Integer applicationId) {
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new EntityNotFoundException("Başvuru bulunamadı. ID: " + applicationId));
        requireSameFaculty(app);

        ApplicationStatus next = switch (app.getStatus()) {
            case FACULTY_BOARD_ACCEPTED -> ApplicationStatus.OIDB_FINAL_REVIEW;
            case FACULTY_BOARD_REJECTED -> ApplicationStatus.REJECTED;
            default -> throw new IllegalStateException(
                    "Başvuru ÖİDB'ye iletim için uygun aşamada değil. Güncel statü: " + app.getStatus());
        };
        return apply(app, next);
    }

    private ApplicationResponse forward(Integer applicationId, ApplicationStatus required, ApplicationStatus next) {
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new EntityNotFoundException("Başvuru bulunamadı. ID: " + applicationId));
        requireSameFaculty(app);
        if (app.getStatus() != required) {
            throw new IllegalStateException(
                    "Başvuru bu iletim için uygun aşamada değil. Güncel statü: " + app.getStatus());
        }
        return apply(app, next);
    }

    /**
     * TC-10.7: Toplu iletim — tüm ID'ler tek transactionda işlenir (all-or-nothing).
     * Herhangi bir başvuru yanlış statüde veya farklı fakültedeyse tüm batch geri alınır.
     */
    @Transactional
    public BatchForwardResponse batchForward(BatchForwardRequest req) {
        if (req.getIds() == null || req.getIds().isEmpty()) {
            return new BatchForwardResponse(0);
        }
        for (Integer id : req.getIds()) {
            switch (req.getAction()) {
                case TO_YGK -> forwardToYgk(id);
                case TO_FACULTY_BOARD -> forwardToFacultyBoard(id);
                case TO_OIDB -> forwardToOidb(id);
            }
        }
        return new BatchForwardResponse(req.getIds().size());
    }

    /** Statü geçişini uygular + denetim logu yazar. */
    private ApplicationResponse apply(Application app, ApplicationStatus next) {
        ApplicationStatus previous = app.getStatus();
        app.setStatus(next);
        applicationRepository.save(app);
        log.info("Dekanlık iletimi: applicationId={}, {} -> {}", app.getApplicationId(), previous, next);
        return applicationService.getApplicationById(app.getApplicationId());
    }

    /** Fakülte sahipliği: dekan yalnızca kendi fakültesindeki başvuruyu iletebilir. */
    private void requireSameFaculty(Application app) {
        Integer deanFacultyId = deanIdentity.currentFacultyId();
        Integer appFacultyId = app.getFaculty() == null ? null : app.getFaculty().getFacultyId();
        if (!deanFacultyId.equals(appFacultyId)) {
            throw new AccessDeniedException("Bu başvuru sizin fakültenize ait değil.");
        }
    }
}
