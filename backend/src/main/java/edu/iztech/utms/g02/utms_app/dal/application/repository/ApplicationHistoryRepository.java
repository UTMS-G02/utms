package edu.iztech.utms.g02.utms_app.dal.application.repository;

import edu.iztech.utms.g02.utms_app.dal.application.entity.ApplicationHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApplicationHistoryRepository extends JpaRepository<ApplicationHistory, Integer> {

    // Frontend'deki Timeline bileşeni için en yeni statü en üstte (veya en altta) 
    // olacak şekilde sıralı çekiyoruz. Desc (Azalan) = En yeni tarih ilk sırada.
    List<ApplicationHistory> findByApplicationIdOrderByChangedAtDesc(Integer applicationId);
    
    // Eğer frontend en eski statüden en yeniye doğru (yukarıdan aşağı) bir timeline 
    // çiziyorsa Asc kullanabilirsiniz:
    // List<ApplicationHistory> findByApplicationIdOrderByChangedAtAsc(Integer applicationId);
}
