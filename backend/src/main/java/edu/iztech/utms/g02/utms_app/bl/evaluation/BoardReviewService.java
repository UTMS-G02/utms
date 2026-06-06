package edu.iztech.utms.g02.utms_app.bl.evaluation;

import edu.iztech.utms.g02.utms_app.api.application.dto.ApplicationResponse;
import edu.iztech.utms.g02.utms_app.api.evaluation.dto.BoardReviewResponse;
import edu.iztech.utms.g02.utms_app.api.evaluation.dto.CommitteeDecisionSummary;
import edu.iztech.utms.g02.utms_app.api.evaluation.dto.IntibakTableResponse;
import edu.iztech.utms.g02.utms_app.bl.application.ApplicationService;
import edu.iztech.utms.g02.utms_app.dal.application.entity.Application;
import edu.iztech.utms.g02.utms_app.dal.application.repository.ApplicationRepository;
import edu.iztech.utms.g02.utms_app.dal.evaluation.entity.CommitteeDecision;
import edu.iztech.utms.g02.utms_app.dal.evaluation.entity.EvaluationResult;
import edu.iztech.utms.g02.utms_app.dal.evaluation.repository.CommitteeDecisionRepository;
import edu.iztech.utms.g02.utms_app.dal.evaluation.repository.EvaluationResultRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * UC-11 — Fakülte Kurulu / Dekanlık inceleme verisini tek çağrıda birleştirir (salt-okunur).
 *
 * <p>Karar YAZMAZ; yalnızca başvuru + YGK skoru + intibak + karar geçmişini
 * {@link BoardReviewResponse} olarak toplar. Karar yazma akışı {@link DecisionService}'tedir.
 */
@Service
@RequiredArgsConstructor
public class BoardReviewService {

    private final ApplicationService applicationService;          // Pair 2 başvuru DTO'su
    private final ApplicationRepository applicationRepository;
    private final EvaluationResultRepository evaluationResultRepository;
    private final CommitteeDecisionRepository committeeDecisionRepository;
    private final CourseEquivalencyService courseEquivalencyService;

    /** Karar geçmişi: eskiden yeniye; decidedAt boşsa sona. */
    private static final Comparator<CommitteeDecision> DECISION_ORDER =
            Comparator.comparing(CommitteeDecision::getDecidedAt,
                    Comparator.nullsLast(Comparator.<LocalDateTime>naturalOrder()));

    /** İntibak denklik eşiği; altındaysa yumuşak uyarı (DecisionService ile aynı). */
    private static final double EQUIVALENCY_THRESHOLD = 0.80;

    @Transactional(readOnly = true)
    public BoardReviewResponse getReview(Integer applicationId) {
        Application app = applicationRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new EntityNotFoundException("Başvuru bulunamadı. ID: " + applicationId));

        ApplicationResponse application = applicationService.getApplicationById(applicationId);
        IntibakTableResponse intibak = courseEquivalencyService.getTable(applicationId);

        EvaluationResult eval = evaluationResultRepository
                .findByApplication_ApplicationId(applicationId)
                .orElse(null);

        List<CommitteeDecisionSummary> decisions = committeeDecisionRepository
                .findByApplication_ApplicationId(applicationId).stream()
                .sorted(DECISION_ORDER)
                .map(this::toSummary)
                .toList();

        // Denklik oranı intibak'tan gelir (getTable hesaplar); %80 altı yumuşak uyarı.
        Double equivalencyRatio = intibak != null ? intibak.getEquivalencyRatio() : null;
        boolean belowThreshold = equivalencyRatio != null && equivalencyRatio < EQUIVALENCY_THRESHOLD;

        return BoardReviewResponse.builder()
                .application(application)
                .compositeScore(eval != null ? eval.getCompositeScore() : null)
                .ranking(eval != null ? eval.getRanking() : null)
                .conditionsMet(app.getYgkApproved())
                .generalNote(eval != null ? eval.getGeneralNote() : null)
                .calculatedAt(eval != null ? eval.getCalculatedAt() : null)
                .intibak(intibak)
                .equivalencyRatio(equivalencyRatio)
                .belowThreshold(belowThreshold)
                .decisions(decisions)
                .build();
    }

    private CommitteeDecisionSummary toSummary(CommitteeDecision d) {
        return CommitteeDecisionSummary.builder()
                .decisionBy(d.getDecisionBy())
                .decision(d.getDecision())
                .notes(d.getNotes())
                .rejectionCode(d.getRejectionCode())
                .decidedAt(d.getDecidedAt())
                .build();
    }
}
