package edu.iztech.utms.g02.utms_app.dal.application.repository;

import edu.iztech.utms.g02.utms_app.dal.application.entity.ApplicationStatus;
import edu.iztech.utms.g02.utms_app.dal.application.entity.Application;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.List;
import java.util.Optional;

public interface ApplicationRepository extends JpaRepository<Application, Integer> {

    List<Application> findByStatus(ApplicationStatus status); 

    List<Application> findByStudentId(String studentId); 

    List<Application> findByAcademicYear(String academicYear); 

    List<Application> findByTargetDept(String targetDept); 

    List<Application> findByTargetFaculty(String targetFaculty); 

    List<Application> findByOidbApproved(boolean oidbApproved);

    List<Application> findByYdyoApproved(boolean ydyoApproved);

    Optional<Application> findByApplicationId(int applicationId);

    // Aynı öğrencinin, aynı döneme ve aynı bölüme kaydı var mı kontrolü
    boolean existsByStudentIdAndTargetDeptAndAcademicYear(String studentId, String targetDept, String academicYear);
}
