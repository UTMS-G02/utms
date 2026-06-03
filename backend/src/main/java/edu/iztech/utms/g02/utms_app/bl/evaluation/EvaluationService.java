package edu.iztech.utms.g02.utms_app.bl.evaluation;

import edu.iztech.utms.g02.utms_app.api.evaluation.dto.EvaluationResponse;
import edu.iztech.utms.g02.utms_app.dal.application.entity.Application;
import edu.iztech.utms.g02.utms_app.dal.application.entity.ApplicationStatus;
import edu.iztech.utms.g02.utms_app.dal.application.repository.ApplicationRepository;
import edu.iztech.utms.g02.utms_app.dal.evaluation.entity.EvaluationResult;
import edu.iztech.utms.g02.utms_app.dal.evaluation.repository.EvaluationResultRepository;
import edu.iztech.utms.g02.utms_app.dal.user.entity.Student;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * YGK aşaması: bekleyen başvuruları toplu skorlar ve sıralar.
 *
 * <ul>
 *   <li>Skor formülü {@link ScoreCalculator}'da sabittir, istekten okunmaz.</li>
 *   <li>Sıralama TEK seferde (toplu) yapılır, başvuru başına değil.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class EvaluationService {

    private final ApplicationRepository applicationRepository;
    private final EvaluationResultRepository evaluationResultRepository;

    /**
     * Devir (handoff) statüsündeki tüm başvuruları skorlar, EvaluationResult üretir,
     * statülerini YGK_SCORED yapar ve tüm değerlendirmeleri yeniden sıralar.
     *
     * <p>Giriş statüsü {@code EVALUATION_QUEUE}'dir; Pair 2 kabul edilen başvuruları
     * OİDB son onayında bu statüye taşır (ApplicationService.processOidbPostYdyoReview).
     *
     * @return skorlanan başvuru sayısı
     */
    @Transactional
    public int scoreAllPendingApplications() {
        List<Application> pending = applicationRepository
                .findByStatus(ApplicationStatus.EVALUATION_QUEUE, Pageable.unpaged())
                .getContent();

        for (Application app : pending) {
            double score = ScoreCalculator.calculate(app.getGpa(), app.getSayYksScore());

            EvaluationResult result = evaluationResultRepository
                    .findByApplication_ApplicationId(app.getApplicationId())
                    .orElseGet(() -> EvaluationResult.builder().application(app).build());
            result.setCompositeScore(score);
            evaluationResultRepository.save(result);

            // compositeScore başvuruda da saklanır (Pair 2 alanı, read-only kullanım)
            app.setCompositeScore(score);
            app.setStatus(ApplicationStatus.YGK_SCORED);
            applicationRepository.save(app);
        }

        updateRankings();
        return pending.size();
    }

    /** En yüksek skordan en düşüğe doğru sıralayıp ranking atar (1 = en yüksek). */
    private void updateRankings() {
        List<EvaluationResult> all = evaluationResultRepository.findAllByOrderByCompositeScoreDesc();
        int rank = 1;
        for (EvaluationResult result : all) {
            result.setRanking(rank++);
            evaluationResultRepository.save(result);
        }
    }

    @Transactional(readOnly = true)
    public List<EvaluationResponse> getEvaluations() {
        return evaluationResultRepository.findAllByOrderByCompositeScoreDesc().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private EvaluationResponse toResponse(EvaluationResult result) {
        Application app = result.getApplication();
        return EvaluationResponse.builder()
                .applicationId(app.getApplicationId())
                .studentName(buildFullName(app.getStudent()))
                .gpa(app.getGpa())
                .yksScore(app.getSayYksScore())
                .compositeScore(result.getCompositeScore())
                .ranking(result.getRanking())
                .status(app.getStatus())
                .calculatedAt(result.getCalculatedAt())
                .build();
    }

    private String buildFullName(Student student) {
        if (student == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        if (student.getFirstName() != null) sb.append(student.getFirstName());
        if (student.getMiddleName() != null && !student.getMiddleName().isBlank()) {
            sb.append(' ').append(student.getMiddleName());
        }
        if (student.getLastName() != null) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(student.getLastName());
        }
        return sb.toString().trim();
    }
}
