package edu.iztech.utms.g02.utms_app.dal.department.repository;

import edu.iztech.utms.g02.utms_app.dal.department.entity.Faculty;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FacultyRepository extends JpaRepository<Faculty, Integer> {
    Optional<Faculty> findByName(String name);
}