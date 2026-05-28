package edu.iztech.utms.g02.utms_app.dal.application.repository;

import edu.iztech.utms.g02.utms_app.dal.application.entity.ApplicationPeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ApplicationPeriodRepository extends JpaRepository<ApplicationPeriod, Integer> {
    
    // Sadece "active = true" olan tek bir kaydı getirir
    Optional<ApplicationPeriod> findByActiveTrue(); 
}
