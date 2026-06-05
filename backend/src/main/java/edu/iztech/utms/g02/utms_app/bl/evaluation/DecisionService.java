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

import java.util.EnumSet;
import java.util.Set;

import static edu.iztech.utms.g02.utms_app.dal.application.entity.ApplicationStatus.*;

/**
 * Komisyon kararları: Dekanlık → Fakülte Kurulu → Nihai Dekanlık.
 *
 * <p>İstek gövdesi Pair 2 ile tutarlı: {@code approved} (boolean) + {@code notes}.
 * committee_decisions tablosu EKLE-ONLY'dir: her karar yeni bir satır olarak eklenir,
 * mevcut satırlar güncellenmez. Statü geçişleri {@link #nextStatus} ile yönetilir.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DecisionService {

    private final ApplicationRepository applicationRepository;
    private final CommitteeDecisionRepository committeeDecisionRepository;
    private final ApplicationService applicationService; // ApplicationResponse üretimi için (Pair 2 public API)

    @Transactional
    public ApplicationResponse recordDeanDecision(Integer applicationId, DecisionRequest req) {
        // YGK_REVIEW_DONE: YGK başvuru bazında değerlendirip Dekanlığa ilettiğinde (UC-8).
        // YGK_SCORED: yalnızca toplu skorlama yapıldıysa (manuel değerlendirme atlanmışsa).
        return record(applicationId, "DEAN", req, EnumSet.of(YGK_SCORED, YGK_REVIEW_DONE, DEAN_REVIEW));
    }

    @Transactional
    public ApplicationResponse recordFacultyBoardDecision(Integer applicationId, DecisionRequest req) {
        return record(applicationId, "FACULTY_BOARD", req, EnumSet.of(FACULTY_BOARD_REVIEW));
    }

    @Transactional
    public ApplicationResponse recordFinalDeanDecision(Integer applicationId, DecisionRequest req) {
        return record(applicationId, "FINAL_DEAN", req, EnumSet.of(FINAL_DEAN_REVIEW));
    }

    private ApplicationResponse record(Integer applicationId, String role,
                                       DecisionRequest req, Set<ApplicationStatus> allowedStatuses) {
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new EntityNotFoundException("Başvuru bulunamadı. ID: " + applicationId));

        if (!allowedStatuses.contains(app.getStatus())) {
            throw new IllegalStateException(
                    "Başvuru bu karar için uygun aşamada değil. Güncel Statü: " + app.getStatus());
        }

        boolean approved = resolveApproved(req);

        // Ret gerekçe kodu yalnızca ret kararında saklanır; onayda anlamsızdır.
        String rejectionCode = approved ? null : req.getRejectionCode();

        // EKLE-ONLY: her karar yeni satır. Makam role olarak saklanır: DEAN / FACULTY_BOARD / FINAL_DEAN.
        committeeDecisionRepository.save(CommitteeDecision.builder()
                .application(app)
                .decisionBy(role)
                .decision(approved ? "APPROVED" : "REJECTED")
                .notes(req.getNotes())
                .rejectionCode(rejectionCode)
                .build());

        ApplicationStatus previous = app.getStatus();
        ApplicationStatus next = nextStatus(role, approved);
        app.setStatus(next);
        applicationRepository.save(app);

        // Denetim izi: karar veren makam / kullanıcı, statü geçişi, karar ve ret kodu.
        log.info("Komisyon kararı: applicationId={}, role={}, {} -> {}, decision={}, rejectionCode={}, by={}",
                applicationId, role, previous, next, approved ? "APPROVED" : "REJECTED",
                rejectionCode, currentUser());

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

    /** PDF §6'daki geçiş tablosu. */
    private ApplicationStatus nextStatus(String role, boolean approved) {
        return switch (role) {
            case "DEAN"          -> approved ? FACULTY_BOARD_REVIEW : DEAN_REJECTED;
            case "FACULTY_BOARD" -> approved ? FINAL_DEAN_REVIEW    : FACULTY_BOARD_REJECTED;
            case "FINAL_DEAN"    -> approved ? RESULT_PUBLISHED     : REJECTED;
            default -> throw new IllegalArgumentException("Tanınmayan rol: " + role);
        };
    }
}
