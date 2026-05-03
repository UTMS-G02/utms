package edu.iztech.utms.g02.utms_app.dal.application.repository;

import edu.iztech.utms.g02.utms_app.dal.application.entity.Application;
import edu.iztech.utms.g02.utms_app.dal.application.entity.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

    List<Application> findByStatus(ApplicationStatus status);

    List<Application> findByStudentId(String studentId);

    List<Application> findByAcademicYear(String academicYear);

    List<Application> findByTargetDept(String targetDept);

    List<Application> findByTargetFaculty(String targetFaculty);

    List<Application> findByOidbApproved(Boolean oidbApproved);

    List<Application> findByYdyoApproved(Boolean ydyoApproved);

    // Spring Data JPA naming convention: exists + By + field names
    boolean existsByStudentIdAndTargetDeptAndAcademicYear(String studentId, String targetDept, String academicYear);
}