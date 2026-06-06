package edu.iztech.utms.g02.utms_app.dal.audit.repository;

import edu.iztech.utms.g02.utms_app.dal.audit.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Integer> {

    List<AuditLog> findByApplicationIdOrderByTimestampAsc(Integer applicationId);

    List<AuditLog> findAllByOrderByTimestampDesc();
}
