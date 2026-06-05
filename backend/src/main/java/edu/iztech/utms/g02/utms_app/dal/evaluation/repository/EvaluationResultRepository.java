package edu.iztech.utms.g02.utms_app.dal.evaluation.repository;

import edu.iztech.utms.g02.utms_app.dal.evaluation.entity.EvaluationResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EvaluationResultRepository extends JpaRepository<EvaluationResult, Integer> {

    Optional<EvaluationResult> findByApplication_ApplicationId(Integer applicationId);

    // Skor sıralı liste (en yüksek skor ilk) — GET /api/evaluations ve sıralama için
    List<EvaluationResult> findAllByOrderByCompositeScoreDesc();
}
