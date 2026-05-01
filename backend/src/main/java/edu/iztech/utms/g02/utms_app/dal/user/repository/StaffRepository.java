package edu.iztech.utms.g02.utms_app.dal.user.repository;

import edu.iztech.utms.g02.utms_app.dal.user.entity.UserRole;
import edu.iztech.utms.g02.utms_app.dal.user.entity.Staff;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StaffRepository extends JpaRepository<Staff, Integer> {

    List<Staff> findByRole(UserRole role);
}