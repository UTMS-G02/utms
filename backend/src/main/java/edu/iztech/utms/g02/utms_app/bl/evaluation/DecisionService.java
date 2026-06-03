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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.Set;

import static edu.iztech.utms.g02.utms_app.dal.application.entity.ApplicationStatus.*;

/**
 * Komisyon kararları: Dekanlık → Fakülte Kurulu → Nihai Dekanlık.
 *
 * <p>committee_decisions tablosu EKLE-ONLY'dir: her karar yeni bir satır olarak eklenir,
 * mevcut satırlar güncellenmez. Statü geçişleri {@link #nextStatus} ile yönetilir.
 */
@Service
@RequiredArgsConstructor
public class DecisionService {

    private static final String DECISION_APPROVED = "APPROVED";
    private static final String DECISION_REJECTED = "REJECTED";

    private final ApplicationRepository applicationRepository;
    private final CommitteeDecisionRepository committeeDecisionRepository;
    private final ApplicationService applicationService; // ApplicationResponse üretimi için (Pair 2 public API)

    @Transactional
    public ApplicationResponse recordDeanDecision(Integer applicationId, DecisionRequest req) {
        return record(applicationId, "DEAN", req, EnumSet.of(YGK_SCORED, DEAN_REVIEW));
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

        String decision = normalizeDecision(req.getDecision());

        // EKLE-ONLY: her karar yeni satır. FINAL_DEAN da makam olarak "DEAN" kaydedilir.
        committeeDecisionRepository.save(CommitteeDecision.builder()
                .application(app)
                .decisionBy("FINAL_DEAN".equals(role) ? "DEAN" : role)
                .decision(decision)
                .notes(req.getNotes())
                .build());

        app.setStatus(nextStatus(role, decision));
        applicationRepository.save(app);

        return applicationService.getApplicationById(applicationId);
    }

    private String normalizeDecision(String decision) {
        if (decision == null) {
            throw new IllegalArgumentException("Karar (decision) boş olamaz. Beklenen: APPROVED veya REJECTED.");
        }
        String normalized = decision.trim().toUpperCase();
        if (!DECISION_APPROVED.equals(normalized) && !DECISION_REJECTED.equals(normalized)) {
            throw new IllegalArgumentException("Geçersiz karar: " + decision + ". Beklenen: APPROVED veya REJECTED.");
        }
        return normalized;
    }

    /** PDF §6'daki geçiş tablosu. */
    private ApplicationStatus nextStatus(String role, String decision) {
        boolean approved = DECISION_APPROVED.equals(decision);
        return switch (role) {
            case "DEAN"          -> approved ? FACULTY_BOARD_REVIEW : DEAN_REJECTED;
            case "FACULTY_BOARD" -> approved ? FINAL_DEAN_REVIEW    : FACULTY_BOARD_REJECTED;
            case "FINAL_DEAN"    -> approved ? RESULT_PUBLISHED     : REJECTED;
            default -> throw new IllegalArgumentException("Tanınmayan rol: " + role);
        };
    }
}
