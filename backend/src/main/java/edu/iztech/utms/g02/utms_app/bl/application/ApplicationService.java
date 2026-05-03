



package edu.iztech.utms.g02.utms_app.bl.application;

import edu.iztech.utms.g02.utms_app.api.application.dto.ApplicationCreateRequest;
import edu.iztech.utms.g02.utms_app.api.application.dto.ApplicationResponse;
import edu.iztech.utms.g02.utms_app.api.application.dto.OidbReviewRequest;
import edu.iztech.utms.g02.utms_app.api.application.dto.YdyoReviewRequest;

import edu.iztech.utms.g02.utms_app.dal.application.entity.Application;
import edu.iztech.utms.g02.utms_app.dal.application.entity.ApplicationStatus;
import edu.iztech.utms.g02.utms_app.dal.application.repository.ApplicationRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;     //?

import java.time.LocalDateTime;                                                                             


@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
//  private final ApplicationMapper applicationMapper; // DTO<->Entity dönüşümleri için??????????

    @Transactional
    public ApplicationResponse create(Long userId, ApplicationCreateRequest req) { 

        // 1. İŞ KURALI: Öğrenci aynı bölüme aynı dönemde birden fazla başvuru yapamaz
        boolean alreadyApplied = applicationRepository.existsByStudentIdAndTargetDeptAndAcademicYear(
                req.getStudentId(), // String dönüyordur diye varsayıyoruz
                req.getTargetDept(), 
                req.getAcademicYear()
        );


        // TODO: Exception isimleri değişecek:

        if (alreadyApplied) {                               
            throw new IllegalArgumentException("Bir öğrenci aynı bölüme, aynı akademik dönemde birden fazla başvuru yapamaz.");
        }

        // 2. Diğer geçerlilik kontrolleri
        if (!Boolean.TRUE.equals(req.getKvkkAccepted())) {
            throw new IllegalArgumentException("KVKK onayı zorunludur.");
        }


        // 3. Application objesini oluşturma (Çift yazılmıştı, teke düşürüldü)
        Application app = Application.builder()
                .studentId(req.getStudentId())
                .status(ApplicationStatus.DRAFT) // İlk oluşumda durumu genelde DRAFT (Taslak) olur
                .academicYear(req.getAcademicYear())
                .targetDept(req.getTargetDept())
                .targetFaculty(req.getTargetFaculty())
                .build();
        
        // 4. Veritabanına kaydet
        app = applicationRepository.save(app);

        // 5. Response olarak dön
        return toResponse(app);
    }


    @Transactional
    public ApplicationResponse submit(Long applicationId, Long userId) {
        
        // 1. Başvuruyu bul
        Integer appId = Math.toIntExact(applicationId);
        Application app = applicationRepository.findById(appId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found"));
        
        // 2. DRAFT değilse IllegalStateException fırlat
        if (app.getStatus() != ApplicationStatus.DRAFT) {
            throw new IllegalStateException("Application is not in DRAFT state");
        }
        
        // 3. Durumu güncelle ve kaydet
        app.setStatus(ApplicationStatus.SUBMITTED);
        app = applicationRepository.save(app);

        return toResponse(app);
    }

    @Transactional
    public ApplicationResponse processOidbReview(Long id, OidbReviewRequest req) {
        Integer appId = Math.toIntExact(id);
        Application app = applicationRepository.findById(appId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found"));

        // OIDB Onayladıysa YDYO'ya gider, reddettiyse OIDB_REJECTED olur
        app.setStatus(req.isApproved() ? ApplicationStatus.YDYO_REVIEW : ApplicationStatus.OIDB_REJECTED);
        
        // İnceleme detaylarını kaydet
        app.setOidbApproved(req.isApproved());
        app.setOidbNotes(req.getNotes());
        app.setOidbReviewedBy(req.getReviewerId()); // Req objende bu alanlar olduğunu varsayıyoruz
        app.setOidbReviewedDate(LocalDateTime.now());

        app = applicationRepository.save(app);
        
        return toResponse(app);
    }


    // Bu metot sınıfın dışında kalmıştı, içeri alındı
    @Transactional
    public ApplicationResponse processYdyoReview(Long id, YdyoReviewRequest req) {
        Integer appId = Math.toIntExact(id);
        Application app = applicationRepository.findById(appId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found"));

        // YDYO Onayladıysa Kurul Değerlendirmesine (EVALUATION_QUEUE) gider, reddettiyse YDYO_REJECTED olur
        app.setStatus(req.isApproved() ? ApplicationStatus.EVALUATION_QUEUE : ApplicationStatus.YDYO_REJECTED);
        
        // İnceleme detaylarını kaydet
        app.setYdyoApproved(req.isApproved());
        app.setYdyoNotes(req.getNotes());
        app.setYdyoReviewedBy(req.getReviewerId()); 
        app.setYdyoReviewedDate(LocalDateTime.now());

        app = applicationRepository.save(app);
        
        return toResponse(app);
    }

    // --- HELPER METHOD ---
    // Eğer Mapper kullanmıyorsanız bu metot Entity'yi Response DTO'ya çevirir.
    private ApplicationResponse toResponse(Application app) {
        ApplicationResponse response = new ApplicationResponse();
        if (app.getApplicationId() != null) {
            response.setId(app.getApplicationId().longValue());
        }
        response.setStudentId(app.getStudentId());
        response.setStatus(app.getStatus());
        response.setAcademicYear(app.getAcademicYear());
        response.setTargetDept(app.getTargetDept());
        // ... diğer alanlarınızı (get/set) ihtiyaca göre buraya ekleyebilirsiniz.
        return response;
    }

}
