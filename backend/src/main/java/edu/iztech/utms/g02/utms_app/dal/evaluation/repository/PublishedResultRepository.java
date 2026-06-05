package edu.iztech.utms.g02.utms_app.dal.evaluation.repository;

import edu.iztech.utms.g02.utms_app.dal.evaluation.entity.PublishedResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PublishedResultRepository extends JpaRepository<PublishedResult, Integer> {

    Optional<PublishedResult> findByApplication_ApplicationId(Integer applicationId);

    boolean existsByApplication_ApplicationId(Integer applicationId);

    // Öğrenci yalnızca kendi sonuçlarını görür (e-posta üzerinden)
    List<PublishedResult> findByApplication_Student_Email(String email);
}
