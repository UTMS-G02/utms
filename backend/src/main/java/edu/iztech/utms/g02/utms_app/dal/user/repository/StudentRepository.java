package edu.iztech.utms.g02.utms_app.dal.user.repository;

import edu.iztech.utms.g02.utms_app.dal.user.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Integer> {

    Optional<Student> findByTckn(String tckn);
}