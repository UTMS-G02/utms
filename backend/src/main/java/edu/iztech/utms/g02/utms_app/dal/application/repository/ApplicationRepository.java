package edu.iztech.utms.g02.utms_app.dal.application.repository;

import edu.iztech.utms.g02.utms_app.dal.application.entity.ApplicationStatus;
import edu.iztech.utms.g02.utms_app.dal.application.entity.Application;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ApplicationRepository extends JpaRepository<Application, Integer> {

    Page<Application> findByStatus(ApplicationStatus status, Pageable pageable);

    // Pair 3: Dekanlık fakülte-kapsamlı liste — yalnızca dekanın fakültesindeki başvurular.
    Page<Application> findByStatusAndFaculty_FacultyId(ApplicationStatus status, Integer facultyId, Pageable pageable);

    // Pair 3: YGK bölüm-kapsamlı skorlama — yalnızca YGK üyesinin bölümündeki başvurular.
    Page<Application> findByStatusAndDepartment_DepartmentId(ApplicationStatus status, Integer departmentId, Pageable pageable);

    Page<Application> findByStudent_UserId(Integer userId, Pageable pageable);

    Page<Application> findByStudent_UserIdAndStatus(Integer userId, ApplicationStatus status, Pageable pageable);

    List<Application> findByAcademicYear(String academicYear);

    List<Application> findByTargetDepartment(String targetDepartment);

    List<Application> findByTargetFaculty(String targetFaculty); 

    List<Application> findByOidbApproved(boolean oidbApproved);

    List<Application> findByYdyoApproved(boolean ydyoApproved);

    Optional<Application> findByApplicationId(Integer applicationId);

    //eklendi 02.06
    List<Application> findByStudent_EmailAndStatus(String email, ApplicationStatus status);

    // Aynı öğrencinin, aynı döneme ve aynı bölüme kaydı var mı kontrolü
    boolean existsByStudent_UserIdAndTargetDepartmentAndAcademicYear(Integer userId, String targetDept, String academicYear);

    // Yalnızca "gerçek" (taslak/geri-çekilmiş olmayan) bir başvuru engel oluşturmalı.
    // statuses parametresine DRAFT ve WITHDRAWN verilerek bunlar duplicate sayılmaz.
    boolean existsByStudent_UserIdAndTargetDepartmentAndAcademicYearAndStatusNotIn(
            Integer userId, String targetDept, String academicYear, Collection<ApplicationStatus> statuses);

    // Aynı bölüm/dönem için yarım kalmış taslağı bulup yeniden kullanmak (yeni taslak üretmemek) için.
    Optional<Application> findFirstByStudent_UserIdAndTargetDepartmentAndAcademicYearAndStatus(
            Integer userId, String targetDept, String academicYear, ApplicationStatus status);

    // Öğrencinin "devam eden" (terminal olmayan) herhangi bir başvurusu var mı?
    // statuses parametresine taslak/geri-çekilmiş/reddedilmiş durumlar verilerek
    // yalnızca AKTİF (incelemede/onaylı) bir başvuru yeni başvuruyu engeller.
    // İş kuralı: bir öğrenci yalnızca TEK bir programa yatay geçiş başvurusu yapabilir.
    boolean existsByStudent_UserIdAndStatusNotIn(Integer userId, Collection<ApplicationStatus> statuses);

    // Öğrencinin yarım kalmış taslağını (bölümden bağımsız) bulup yeniden kullanmak için.
    Optional<Application> findFirstByStudent_UserIdAndStatus(Integer userId, ApplicationStatus status);
}
