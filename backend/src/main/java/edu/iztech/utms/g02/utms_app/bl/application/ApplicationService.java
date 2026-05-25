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
import org.springframework.web.multipart.MultipartFile;
import jakarta.persistence.EntityNotFoundException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final DocumentRepository documentRepository;

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
                currentStudent.getStudentId(), // Artık request'ten değil, güvenilir kaynaktan alıyoruz
                req.getTargetDept(), 
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
                .studentId(currentStudent.getStudentId())
                .status(ApplicationStatus.DRAFT) // İlk oluşumda durumu genelde DRAFT (Taslak) olur
                .academicYear(req.getAcademicYear())
                .targetDept(req.getTargetDept())
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

        if (app.getStatus() != ApplicationStatus.DRAFT) {
            throw new IllegalStateException("Application is not in DRAFT state");
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
        
        app.setOidbApproved(req.isApproved());
        app.setOidbNotes(req.getNotes());
        app.setOidbReviewedBy(req.getReviewerId()); 
        app.setOidbReviewedDate(LocalDateTime.now());

        app = applicationRepository.save(app);
        
        return toResponse(app);
    }

    @Transactional
    public ApplicationResponse processYdyoReview(Integer applicationId, YdyoReviewRequest req) {
        
        Application app = applicationRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found"));

        app.setStatus(req.isApproved() ? ApplicationStatus.EVALUATION_QUEUE : ApplicationStatus.YDYO_REJECTED);
        app.setYdyoApproved(req.isApproved());
        app.setYdyoNotes(req.getNotes());
        app.setYdyoReviewedBy(req.getReviewerId()); 
        app.setYdyoReviewedDate(LocalDateTime.now());

        app = applicationRepository.save(app);
        
        return toResponse(app);
    }

    // ==========================================
    // YENİ EKLENEN 3 METOT (GET ALL, GET BY ID, UPLOAD)
    // ==========================================

    public List<ApplicationResponse> getAllApplications() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // JWT'den gelen getName() metodu kullanıcının E-POSTA adresini döner (Örn: busra@std.iztech.edu.tr)
        String currentUserEmail = authentication.getName(); 

        boolean isStudent = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT") || a.getAuthority().equals("STUDENT"));

        if (isStudent) {
            // E-postadan ID'yi buluyoruz
            Student student = studentRepository.findByEmail(currentUserEmail)
                    .orElseThrow(() -> new EntityNotFoundException("Kullanıcı bulunamadı."));
            
            return applicationRepository.findByStudentId(student.getUserId()).stream() // getstudentId() mi olacak yoksa getuserId() mu olacak ???
                    .map(this::toResponse)
                    .collect(Collectors.toList());

        } else {
            // Öğrenci değilse (OIDB, YDYO, FACULTY, DEAN vs.) herkesi görebilir
            return applicationRepository.findAll().stream()
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
    public void uploadDocument(Integer id, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Yüklenecek dosya boş olamaz.");
        }

        Application app = applicationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Başvuru bulunamadı. ID: " + id));

        verifyOwnership(app); // Güvenlik: Başkası belge yükleyemesin

        //Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        //String currentUsername = authentication.getName();

        //if (!String.valueOf(app.getStudentId()).equals(currentUsername)) {
          //  throw new AccessDeniedException("Sadece kendi başvurunuza belge yükleyebilirsiniz.");
        //}

        try {
            Path uploadPath = Paths.get("uploads");
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String originalFileName = file.getOriginalFilename();
            String uniqueFileName = id + "_" + System.currentTimeMillis() + "_" + originalFileName;
            Path filePath = uploadPath.resolve(uniqueFileName);

            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Document Entity'sini oluşturup veritabanına kaydediyoruz
            // DİKKAT: Document sınıfında @Builder olduğunu ve aşağıdakine benzer alanları olduğunu varsaydım.
            // Eğer yoksa new Document() yapıp set... metotlarıyla doldurabilirsin.
            Document document = Document.builder()
                     //.application(app) // Eğer Document ile Application arasında @ManyToOne ilişkiniz varsa bunu açın
                     .application(app) // İlişkiyi mutlaka kurmalıyız ki hangi dosya kime ait bilelim
                     .filePath(filePath.toString())
                     .fileName(originalFileName)
                    .build();
            
            documentRepository.save(document);

        } catch (IOException e) {
            throw new RuntimeException("Dosya sisteme kaydedilirken bir hata oluştu: " + e.getMessage());
        }
    }

    // --- HELPER METHODS ---

    // DRY (Don't Repeat Yourself) prensibi için sahiplik kontrolünü tek bir yere aldık
    private void verifyOwnership(Application app) {
        String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();

        // E-posta ile veritabanından kullanıcıyı bul
        Student student = studentRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new EntityNotFoundException("Kullanıcı bulunamadı."));

        // Veritabanındaki Student ID'si ile Başvuru üzerindeki Student ID eşleşiyor mu?
        if (!app.getStudentId().equals(student.getUserId())) { // getStudentId() mu yoksa getUserId() mu olacak ???
            throw new AccessDeniedException("Bu başvuru üzerinde işlem yapma yetkiniz bulunmuyor.");
        }
    }

    private ApplicationResponse toResponse(Application app) {
        ApplicationResponse response = new ApplicationResponse();
        response.setId(app.getApplicationId());
        response.setStudentId(app.getStudentId());
        response.setStatus(app.getStatus());
        response.setAcademicYear(app.getAcademicYear());
        response.setTargetDept(app.getTargetDept());
        response.setCurrentUniversity(app.getCurrentUniversity());
        response.setCurrentFaculty(app.getCurrentFaculty());
        response.setCurrentDepartment(app.getCurrentDepartment());
        response.setGpa(app.getGpa());
        return response;
    }
}