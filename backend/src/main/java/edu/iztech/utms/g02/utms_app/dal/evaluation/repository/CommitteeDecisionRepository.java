package edu.iztech.utms.g02.utms_app.dal.evaluation.repository;

import edu.iztech.utms.g02.utms_app.dal.evaluation.entity.CommitteeDecision;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommitteeDecisionRepository extends JpaRepository<CommitteeDecision, Integer> {

    List<CommitteeDecision> findByApplication_ApplicationId(Integer applicationId);
}
