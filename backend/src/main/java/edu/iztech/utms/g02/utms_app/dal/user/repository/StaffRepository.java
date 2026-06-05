package edu.iztech.utms.g02.utms_app.dal.user.repository;

import edu.iztech.utms.g02.utms_app.dal.user.entity.UserRole;
import edu.iztech.utms.g02.utms_app.dal.user.entity.Staff;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StaffRepository extends JpaRepository<Staff, Integer> {

    List<Staff> findByRole(UserRole role);

    // Pair 3: oturum açan personeli (dekan/YGK) e-postasından çözüp facultyId/departmentId'sini okumak için.
    Optional<Staff> findByEmail(String email);
}