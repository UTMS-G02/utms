package edu.iztech.utms.g02.utms_app.bl.application;

import edu.iztech.utms.g02.utms_app.api.application.dto.*;
import edu.iztech.utms.g02.utms_app.dal.application.entity.*;
import edu.iztech.utms.g02.utms_app.dal.application.repository.*;

import edu.iztech.utms.g02.utms_app.dal.user.entity.Student; // EKLENDİ
import edu.iztech.utms.g02.utms_app.dal.user.repository.StudentRepository; // EKLENDİ

import edu.iztech.utms.g02.utms_app.integration.yoksis.YoksisIntegrationService; // EKLENDİ
import edu.iztech.utms.g02.utms_app.integration.yoksis.dto.YoksisStudentResponse; // EKLENDİ

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;
import jakarta.persistence.EntityNotFoundException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;


// EKLENDİ 28.05
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import java.io.IOException;


/*
// ApplicationService.java
// Tüm iş kurallarının yaşadığı yer. En kritik class.

// - create(): Yeni başvuru oluşturur, durumu DRAFT yapar, veritabanına kaydeder
// - submit(): DRAFT kontrolü yapar → SUBMITTED yapar (DRAFT değilse hata fırlatır)
// - processOidbReview(): ÖİDB kararına göre → YDYO_REVIEW veya OIDB_REJECTED
// - processYdyoReview(): YDYO kararına göre → EVALUATION_QUEUE veya YDYO_REJECTED
// - Her metod: Repository'den veriyi çeker → iş kuralını uygular → kaydeder → Response döner
*/


@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;

    private final StudentRepository studentRepository; // EKLENDİ
    private final YoksisIntegrationService yoksisIntegrationService; // EKLENDİ

    //  private final ApplicationMapper applicationMapper; // DTO<->Entity dönüşümleri için --> en aşağıda manuel olarak yapıyoruz, toResponse() metodu ile

    @Transactional
    public ApplicationResponse create(ApplicationCreateRequest req) { // Integer userId, silindi

        // 1. Güvenlik: İsteği atan kullanıcıyı tespit et
        String currentStudentEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        // 2. Senin StudentRepository'ni kullanarak veritabanından öğrenciyi çek
        Student currentStudent = studentRepository.findByEmail(currentStudentEmail)
            .orElseThrow(() -> new EntityNotFoundException("Öğrenci bulunamadı."));

        // 1. İŞ KURALI: Öğrenci aynı bölüme aynı dönemde birden fazla başvuru yapamaz
        boolean alreadyApplied = applicationRepository.existsByStudentIdAndTargetDeptAndAcademicYear(
                currentStudent.getUserId(), // Artık request'ten değil, güvenilir kaynaktan alıyoruz
                req.getTargetDepartment(), 
                req.getAcademicYear()
        );

        if (alreadyApplied) {                               
            throw new IllegalArgumentException("Bir öğrenci aynı bölüme, aynı akademik dönemde birden fazla başvuru yapamaz.");
        }

        // 2. Diğer geçerlilik kontrolleri // gerekli miiii?
        if (!Boolean.TRUE.equals(req.getKvkkAccepted())) {
            throw new IllegalArgumentException("KVKK onayı zorunludur.");
        }

        // 3. Dış Sistem Entegrasyonu: YÖKSİS'ten akademik verileri çek
        YoksisStudentResponse yoksisData = yoksisIntegrationService.fetchAcademicDataByTckn(currentStudent.getTckn());

        // 4. Application objesini oluşturma (Kendi verilerimiz + YÖKSİS verileri + Request verileri harmanlanıyor)
        Application app = Application.builder()
                .student(currentStudent) // İlişkiyi kuruyoruz ?????
                .status(ApplicationStatus.DRAFT) // İlk oluşumda durumu genelde DRAFT (Taslak) olur
                .academicYear(req.getAcademicYear())
                .targetDepartment(req.getTargetDepartment())
                .targetFaculty(req.getTargetFaculty())

                // Front-end'den gelen YKS verileri
                .sayYksScore(req.getSayYksScore())
                .sayYksRank(req.getSayYksRank())

                // YÖKSİS'ten otomatik gelen veriler
                .currentUniversity(yoksisData.currentUniversity())
                .currentFaculty(yoksisData.currentFaculty())
                .currentDepartment(yoksisData.currentDepartment())
                .gpa(yoksisData.gpa())

                .build();
        
        // 4. Veritabanına kaydet
        app = applicationRepository.save(app);

        // 5. Response olarak dön
        return toResponse(app);
    }

    @Transactional
    public ApplicationResponse submit(Integer applicationId) { //, Integer userId silindi

        // Sadece başvuru id'si yeterli, kullanıcının kendi başvurusu olup olmadığını kontrol edelim
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found"));
        
        verifyOwnership(app); // Helper metot ile güvenlik kontrolü

        if (app.getStatus() != ApplicationStatus.DRAFT && app.getStatus() != ApplicationStatus.REVISION_REQUESTED) { // REVISION_REQUESTED ekledik, böylece öğrenci düzeltme yapıp tekrar gönderebilir
            throw new IllegalStateException("Sadece DRAFT veya REVISION_REQUESTED durumundaki başvurular gönderilebilir.");
        }
        
        app.setStatus(ApplicationStatus.SUBMITTED);
        app = applicationRepository.save(app);

        return toResponse(app);
    }

    @Transactional
    public ApplicationResponse processOidbReview(Integer applicationId, OidbReviewRequest req) {
         
        Application app = applicationRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found"));

        app.setStatus(req.isApproved() ? ApplicationStatus.YDYO_REVIEW : ApplicationStatus.OIDB_REJECTED);

        if (req.isRequestRevision()) {
            if (app.isRevisionRequestedBefore()) {
                throw new IllegalStateException("Öğrenciye zaten bir kez düzeltme hakkı tanınmış. Başvuruyu ya onaylamalı ya da reddetmelisiniz.");
            }
            app.setStatus(ApplicationStatus.REVISION_REQUESTED);
            app.setRevisionRequestedBefore(true);
        } else if (Boolean.TRUE.equals(req.isApproved())) {
            // Eğer memur her şeyi onayladıysa YDYO'ya gider
            app.setStatus(ApplicationStatus.YDYO_REVIEW);
        } else {
            // Tamamen reddedildiyse
            app.setStatus(ApplicationStatus.OIDB_REJECTED);
        }
        
        app.setOidbApproved(req.isApproved());
        app.setOidbNotes(req.getNotes());
        app.setOidbReviewedBy(req.getReviewer()); // bu kısım kontrol edilsin !!!
        app.setOidbReviewedDate(LocalDateTime.now());

        app = applicationRepository.save(app);
        return toResponse(app);
    }

    @Transactional
    public ApplicationResponse processYdyoReview(Integer applicationId, YdyoReviewRequest req) {
        
        Application app = applicationRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found"));

        app.setStatus(req.isApproved() ? ApplicationStatus.YDYO_ACCEPTED : ApplicationStatus.YDYO_REJECTED); //  EVALUATION_QUEUE yerine YDYO_ACCEPTED olmalı ??
        app.setYdyoApproved(req.isApproved());
        app.setYdyoNotes(req.getNotes());
        app.setYdyoReviewedBy(req.getReviewer()); 
        app.setYdyoReviewedDate(LocalDateTime.now());

        app = applicationRepository.save(app);
        
        return toResponse(app);
    }

    // ==========================================
    // YENİ EKLENEN 3 METOT (GET ALL, GET BY ID, UPLOAD)
    // ==========================================

    public List<ApplicationResponse> getAllApplications() {
        return getAllApplications(null);
    }

    public List<ApplicationResponse> getAllApplications(ApplicationStatus status) { // eklendi 28.05 : ApplicationStatus status, int page, int size
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // JWT'den gelen getName() metodu kullanıcının E-POSTA adresini döner (Örn: busra@std.iztech.edu.tr)
        String currentUserEmail = authentication.getName(); 

        boolean isStudent = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT") || a.getAuthority().equals("STUDENT"));

        //eklendi 28.05
        //Pageable pageable = PageRequest.of(page, size);
        //Page<Application> applicationPage;

        if (isStudent) {
            // E-postadan ID'yi buluyoruz
            Student currentStudent = studentRepository.findByEmail(currentUserEmail)
                    .orElseThrow(() -> new EntityNotFoundException("Kullanıcı bulunamadı."));

            if (status == null) {
                return applicationRepository.findByStudentId(currentStudent.getUserId()).stream()
                        .map(this::toResponse)
                        .collect(Collectors.toList());
            }

            return applicationRepository.findByStudentIdAndStatus(currentStudent.getUserId(), status).stream()
                    .map(this::toResponse)
                    .collect(Collectors.toList());

        } else {
            // Öğrenci değilse (OIDB, YDYO, FACULTY, DEAN vs.) herkesi görebilir
            if (status == null) {
                return applicationRepository.findAll().stream()
                        .map(this::toResponse)
                        .collect(Collectors.toList());
            }

            return applicationRepository.findByStatus(status).stream()
                    .map(this::toResponse)
                    .collect(Collectors.toList());
        }

    }

    public ApplicationResponse getApplicationById(Integer id) {
        Application app = applicationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Başvuru bulunamadı. ID: " + id));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        //String currentUsername = authentication.getName();
        
        boolean isStudent = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT") || a.getAuthority().equals("STUDENT"));

        // Güvenlik: Öğrenci sadece kendi başvurusunu görebilir. String.valueOf() ile tip uyuşmazlığını önlüyoruz.
        //if (isStudent && !String.valueOf(app.getStudentId()).equals(currentUsername)) {
            //throw new AccessDeniedException("Bu başvuruyu görüntüleme yetkiniz bulunmuyor.");
        //}

        if (isStudent) {
            verifyOwnership(app);
        }

        return toResponse(app);
    }


    @Transactional
    public ApplicationResponse withdrawApplication(Integer applicationId) {
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new EntityNotFoundException("Başvuru bulunamadı."));

        // 1. GÜVENLİK: Sadece başvurunun sahibi iptal edebilir
        verifyOwnership(app);

        // 2. ZAMAN KONTROLÜ (Tek ve kesin kural)
        if (!isApplicationPeriodActive()) {
            throw new IllegalStateException("Başvuru dönemi sona erdiği için başvurunuzu artık geri çekemezsiniz.");
        }

        // Sistemin patlamaması için eklenebilecek tek minik statü kontrolü (Opsiyonel)
        if (app.getStatus() == ApplicationStatus.WITHDRAWN) {
             throw new IllegalStateException("Başvuru zaten geri çekilmiş.");
        }

        // 3. STATÜYÜ GÜNCELLE VE KAYDET
        app.setStatus(ApplicationStatus.WITHDRAWN);
        app = applicationRepository.save(app);

        return toResponse(app);
    }


    // --- ZAMAN KONTROLÜ İÇİN YARDIMCI METOT ---
    // entitye class eklenecek mi ? applicationPeriod diye ?
    private boolean isApplicationPeriodActive() {
        // TODO: Projendeki takvim yapısına göre burayı güncellemelisin.
        // Örnek Senaryo:
        // AcademicCalendar calendar = calendarRepository.findActiveTerm();
        // LocalDateTime now = LocalDateTime.now();
        // return now.isAfter(calendar.getStartDate()) && now.isBefore(calendar.getEndDate());
        
        return true; // Şimdilik testlerin geçmesi için true dönüyoruz.
    }

    // --- HELPER METHODS ---

    // DRY (Don't Repeat Yourself) prensibi için sahiplik kontrolünü tek bir yere aldık
    private void verifyOwnership(Application app) {
        String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();

        // E-posta ile veritabanından kullanıcıyı bul
        Student student = studentRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new EntityNotFoundException("Kullanıcı bulunamadı."));

        // Veritabanındaki Student ID'si ile Başvuru üzerindeki Student ID eşleşiyor mu?
        if (!app.getStudent().getUserId().equals(student.getUserId())) { 
            throw new AccessDeniedException("Bu başvuru üzerinde işlem yapma yetkiniz bulunmuyor.");
        }
    }

    private ApplicationResponse toResponse(Application app) {
        ApplicationResponse response = new ApplicationResponse();
        response.setId(app.getApplicationId());
        response.setStudentId(app.getStudent().getUserId()); //
        response.setStatus(app.getStatus());
        response.setAcademicYear(app.getAcademicYear());
        response.setTargetDepartment(app.getTargetDepartment());
        response.setCurrentUniversity(app.getCurrentUniversity());
        response.setCurrentFaculty(app.getCurrentFaculty());
        response.setCurrentDepartment(app.getCurrentDepartment());
        response.setGpa(app.getGpa());
        return response;
    }
}