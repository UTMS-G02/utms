package edu.iztech.utms.g02.utms_app.dal.evaluation.repository;

import edu.iztech.utms.g02.utms_app.dal.evaluation.entity.CourseEquivalency;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CourseEquivalencyRepository extends JpaRepository<CourseEquivalency, Integer> {

    List<CourseEquivalency> findByApplication_ApplicationIdOrderByRowOrderAsc(Integer applicationId);

    void deleteByApplication_ApplicationId(Integer applicationId);

    Optional<CourseEquivalency> findByIdAndApplication_ApplicationId(Integer id, Integer applicationId);

    int countByApplication_ApplicationId(Integer applicationId);
}
