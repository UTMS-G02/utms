package edu.iztech.utms.g02.utms_app.bl.application;

import edu.iztech.utms.g02.utms_app.api.application.dto.*;

import edu.iztech.utms.g02.utms_app.dal.application.entity.*;

import edu.iztech.utms.g02.utms_app.dal.application.repository.*;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// --- EKLENEN YENİ İMPORTLAR ---
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.multipart.MultipartFile;
import jakarta.persistence.EntityNotFoundException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
// ------------------------------

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final DocumentRepository documentRepository;
//  private final ApplicationMapper applicationMapper; // DTO<->Entity dönüşümleri için

    @Transactional
    public ApplicationResponse create(Integer userId, ApplicationCreateRequest req) { 

        // 1. İŞ KURALI: Öğrenci aynı bölüme aynı dönemde birden fazla başvuru yapamaz
        boolean alreadyApplied = applicationRepository.existsByStudentIdAndTargetDeptAndAcademicYear(
                req.getStudentId(), // String dönüyordur diye varsayıyoruz
                req.getTargetDept(), 
                req.getAcademicYear()
        );

        if (alreadyApplied) {                               
            throw new IllegalArgumentException("Bir öğrenci aynı bölüme, aynı akademik dönemde birden fazla başvuru yapamaz.");
        }

        // 2. Diğer geçerlilik kontrolleri
        if (!Boolean.TRUE.equals(req.getKvkkAccepted())) {
            throw new IllegalArgumentException("KVKK onayı zorunludur.");
        }

        // 3. Application objesini oluşturma
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
    public ApplicationResponse submit(Integer applicationId, Integer userId) {
        
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found"));
        
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
        String currentUsername = authentication.getName(); // Genelde öğrenci numarası/ID'si buraya düşer

        boolean isStudent = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT") || a.getAuthority().equals("STUDENT"));

        List<Application> applications;

        if (isStudent) {
            // DİKKAT: ApplicationRepository içine "List<Application> findByStudentId(String studentId);" metodunu eklemelisin.
            applications = applicationRepository.findByStudentId(currentUsername); 
        } else {
            // Öğrenci değilse (OIDB, YDYO, FACULTY, DEAN vs.) herkesi görebilir
            applications = applicationRepository.findAll(); 
        }

        return applications.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public ApplicationResponse getApplicationById(Integer id) {
        Application app = applicationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Başvuru bulunamadı. ID: " + id));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();
        
        boolean isStudent = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT") || a.getAuthority().equals("STUDENT"));

        // Güvenlik: Öğrenci sadece kendi başvurusunu görebilir. String.valueOf() ile tip uyuşmazlığını önlüyoruz.
        if (isStudent && !String.valueOf(app.getStudentId()).equals(currentUsername)) {
            throw new AccessDeniedException("Bu başvuruyu görüntüleme yetkiniz bulunmuyor.");
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

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();

        if (!String.valueOf(app.getStudentId()).equals(currentUsername)) {
            throw new AccessDeniedException("Sadece kendi başvurunuza belge yükleyebilirsiniz.");
        }

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
                     .filePath(filePath.toString())
                     .fileName(originalFileName)
                    .build();
            
            documentRepository.save(document);

        } catch (IOException e) {
            throw new RuntimeException("Dosya sisteme kaydedilirken bir hata oluştu: " + e.getMessage());
        }
    }


    // --- HELPER METHOD ---
    private ApplicationResponse toResponse(Application app) {
        ApplicationResponse response = new ApplicationResponse();
        response.setId(app.getApplicationId());
        response.setStudentId(app.getStudentId());
        response.setStatus(app.getStatus());
        response.setAcademicYear(app.getAcademicYear());
        response.setTargetDept(app.getTargetDept());
        // ... diğer alanlarınızı (get/set) ihtiyaca göre buraya ekleyebilirsiniz.
        return response;
    }
}