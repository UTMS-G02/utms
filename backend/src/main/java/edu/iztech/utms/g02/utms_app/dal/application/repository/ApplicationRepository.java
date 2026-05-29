package edu.iztech.utms.g02.utms_app.dal.application.repository;

import edu.iztech.utms.g02.utms_app.dal.application.entity.ApplicationStatus;
import edu.iztech.utms.g02.utms_app.dal.application.entity.Application;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


import java.util.List;
import java.util.Optional;

public interface ApplicationRepository extends JpaRepository<Application, Integer> {

    Page<Application> findByStatus(ApplicationStatus status, Pageable pageable);

    Page<Application> findByStudentId(Integer studentId, Pageable pageable);

    Page<Application> findByStudentIdAndStatus(Integer studentId, ApplicationStatus status, Pageable pageable);

    List<Application> findByAcademicYear(String academicYear); 

    List<Application> findByTargetDept(String targetDept); 

    List<Application> findByTargetFaculty(String targetFaculty); 

    List<Application> findByOidbApproved(boolean oidbApproved);

    List<Application> findByYdyoApproved(boolean ydyoApproved);

    Optional<Application> findByApplicationId(Integer applicationId);

    // Aynı öğrencinin, aynı döneme ve aynı bölüme kaydı var mı kontrolü
    boolean existsByStudentIdAndTargetDeptAndAcademicYear(Integer studentId, String targetDept, String academicYear);
}
