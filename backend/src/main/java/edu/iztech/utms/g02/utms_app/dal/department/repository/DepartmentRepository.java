package edu.iztech.utms.g02.utms_app.dal.department.repository;

import edu.iztech.utms.g02.utms_app.dal.department.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<Department, Integer> {
    List<Department> findByFaculty_FacultyId(Integer facultyId);
    boolean existsByName(String name);
    Optional<Department> findByName(String name);
}