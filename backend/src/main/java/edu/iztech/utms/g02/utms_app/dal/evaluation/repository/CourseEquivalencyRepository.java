package edu.iztech.utms.g02.utms_app.dal.evaluation.repository;

import edu.iztech.utms.g02.utms_app.dal.evaluation.entity.CourseEquivalency;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseEquivalencyRepository extends JpaRepository<CourseEquivalency, Integer> {

    List<CourseEquivalency> findByApplication_ApplicationIdOrderByRowOrderAsc(Integer applicationId);

    void deleteByApplication_ApplicationId(Integer applicationId);
}
