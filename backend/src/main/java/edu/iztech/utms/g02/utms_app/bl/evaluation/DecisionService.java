package edu.iztech.utms.g02.utms_app.bl.evaluation;

import edu.iztech.utms.g02.utms_app.api.application.dto.ApplicationResponse;
import edu.iztech.utms.g02.utms_app.api.evaluation.dto.DecisionRequest;
import edu.iztech.utms.g02.utms_app.bl.application.ApplicationService;
import edu.iztech.utms.g02.utms_app.dal.application.entity.Application;
import edu.iztech.utms.g02.utms_app.dal.application.entity.ApplicationStatus;
import edu.iztech.utms.g02.utms_app.dal.application.repository.ApplicationRepository;
import edu.iztech.utms.g02.utms_app.dal.evaluation.entity.CommitteeDecision;
import edu.iztech.utms.g02.utms_app.dal.evaluation.repository.CommitteeDecisionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static edu.iztech.utms.g02.utms_app.dal.application.entity.ApplicationStatus.FACULTY_BOARD_ACCEPTED;
import static edu.iztech.utms.g02.utms_app.dal.application.entity.ApplicationStatus.FACULTY_BOARD_REJECTED;
import static edu.iztech.utms.g02.utms_app.dal.application.entity.ApplicationStatus.FACULTY_BOARD_REVIEW;

/**
 * Komisyon kararı. Router modelinde TEK karar makamı Fakülte Kurulu'dur.
 *
 * <p>Dekanlık Ofisi karar VERMEZ (yalnızca iletir → {@link DeanForwardService}), bu yüzden
 * eski dekan / nihai-dekan karar akışları kaldırılmıştır. committee_decisions EKLE-ONLY'dir.
 * Geçiş: {@code FACULTY_BOARD_REVIEW} → onay: {@code FACULTY_BOARD_ACCEPTED} | ret: {@code FACULTY_BOARD_REJECTED}.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DecisionService {

    private final ApplicationRepository applicationRepository;
    private final CommitteeDecisionRepository committeeDecisionRepository;
    private final ApplicationService applicationService; // ApplicationResponse üretimi için (Pair 2 public API)

    @Transactional
    public ApplicationResponse recordFacultyBoardDecision(Integer applicationId, DecisionRequest req) {
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new EntityNotFoundException("Başvuru bulunamadı. ID: " + applicationId));

        if (app.getStatus() != FACULTY_BOARD_REVIEW) {
            throw new IllegalStateException(
                    "Başvuru bu karar için uygun aşamada değil. Güncel Statü: " + app.getStatus());
        }

        boolean approved = resolveApproved(req);

        // Ret gerekçe kodu yalnızca ret kararında saklanır; onayda anlamsızdır.
        String rejectionCode = approved ? null : req.getRejectionCode();

        // EKLE-ONLY: her karar yeni satır.
        committeeDecisionRepository.save(CommitteeDecision.builder()
                .application(app)
                .decisionBy("FACULTY_BOARD")
                .decision(approved ? "APPROVED" : "REJECTED")
                .notes(req.getNotes())
                .rejectionCode(rejectionCode)
                .build());

        ApplicationStatus previous = app.getStatus();
        ApplicationStatus next = approved ? FACULTY_BOARD_ACCEPTED : FACULTY_BOARD_REJECTED;
        app.setStatus(next);
        applicationRepository.save(app);

        log.info("Fakülte Kurulu kararı: applicationId={}, {} -> {}, decision={}, rejectionCode={}, by={}",
                applicationId, previous, next, approved ? "APPROVED" : "REJECTED", rejectionCode, currentUser());

        return applicationService.getApplicationById(applicationId);
    }

    /** Denetim logu için aktif kullanıcının e-postası; güvenlik bağlamı yoksa "anonymous". */
    private String currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.getName() != null) ? auth.getName() : "anonymous";
    }

    private boolean resolveApproved(DecisionRequest req) {
        if (req.getApproved() == null) {
            throw new IllegalArgumentException("Karar (approved) belirtilmelidir: true (onay) veya false (ret).");
        }
        return req.getApproved();
    }
}
