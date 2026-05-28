package edu.iztech.utms.g02.utms_app.bl.application;

import edu.iztech.utms.g02.utms_app.dal.application.entity.Application;
import edu.iztech.utms.g02.utms_app.dal.application.entity.ApplicationStatus;
import edu.iztech.utms.g02.utms_app.dal.application.entity.Document;
import edu.iztech.utms.g02.utms_app.dal.application.repository.*;
import edu.iztech.utms.g02.utms_app.dal.user.entity.Student;
import edu.iztech.utms.g02.utms_app.dal.user.repository.StudentRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.List;


/*
// DocumentService.java
// Belge yükleme sürecini yönetir.

// - Gelen dosyayı uploads/ klasörüne fiziksel olarak kaydeder
// - Dosyanın adını ve yolunu Document entity'sine yazar
// - Entity'yi DocumentRepository aracılığıyla veritabanına kaydeder
// - Dosyanın kendisi DB'ye gitmez — sadece yolu (file path) gider
*/


@Service
@RequiredArgsConstructor
public class DocumentService {

    private static final String DEFAULT_DOCUMENT_TYPE = "OTHER";
    private static final Path UPLOAD_DIRECTORY = Paths.get("uploads");

    private final DocumentRepository documentRepository;
    private final ApplicationRepository applicationRepository;
private final StudentRepository studentRepository; // eklendi 28.05

    @Transactional
    public Document uploadDocument(Integer applicationId, MultipartFile file) {
        return uploadDocument(applicationId, DEFAULT_DOCUMENT_TYPE, file);
    }

    @Transactional
    public Document uploadDocument(Integer applicationId, String documentType, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Yuklenecek dosya bos olamaz.");
        }

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new EntityNotFoundException("Basvuru bulunamadi. ID: " + applicationId));

        checkStudentCanManageApplication(application);

        // GÜVENLİK: Başvuru incelemeye alınmışsa veya onaylanmışsa belge yüklenemez!
        if (application.getStatus() != ApplicationStatus.DRAFT && application.getStatus() != ApplicationStatus.REVISION_REQUESTED) {
             throw new AccessDeniedException("Başvuru inceleme aşamasında olduğu için belge ekleyemez veya değiştiremezsiniz.");
        }

        String originalFileName = StringUtils.cleanPath(
                file.getOriginalFilename() == null ? "document" : file.getOriginalFilename()
        );

        if (originalFileName.contains("..")) {
            throw new IllegalArgumentException("Gecersiz dosya adi: " + originalFileName);
        }

        try {
            Files.createDirectories(UPLOAD_DIRECTORY);

            String uniqueFileName = applicationId + "_" + System.currentTimeMillis() + "_" + originalFileName;
            Path filePath = UPLOAD_DIRECTORY.resolve(uniqueFileName).normalize();

            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            Document document = Document.builder()
                    .application(application)
                    .documentType(normalizeDocumentType(documentType))
                    .fileName(originalFileName)
                    .filePath(filePath.toString())
                    .ydyoApproved(false)
                    .documentUploadDate(LocalDate.now())
                    .active(true)
                    .build();

            return documentRepository.save(document);
        } catch (IOException e) {
            throw new RuntimeException("Dosya sisteme kaydedilirken bir hata olustu: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public List<Document> getDocumentsByApplicationId(Integer applicationId) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new EntityNotFoundException("Basvuru bulunamadi. ID: " + applicationId));

        checkStudentCanViewApplication(application);

        return documentRepository.findByApplicationApplicationId(applicationId);
    }

    @Transactional(readOnly = true)
    public List<Document> getDocumentsByType(String documentType) {
        return documentRepository.findByDocumentType(normalizeDocumentType(documentType));
    }

    @Transactional(readOnly = true)
    public Document getDocumentById(Integer documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new EntityNotFoundException("Belge bulunamadi. ID: " + documentId));

        checkStudentCanViewApplication(document.getApplication());

        return document;
    }

    @Transactional
    public Document approveByYdyo(Integer documentId, boolean approved) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new EntityNotFoundException("Belge bulunamadi. ID: " + documentId));

        document.setYdyoApproved(approved);
        return documentRepository.save(document);
    }

    @Transactional
    public void deactivateDocument(Integer documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new EntityNotFoundException("Belge bulunamadi. ID: " + documentId));

        checkStudentCanManageApplication(document.getApplication());

        document.setActive(false);
        documentRepository.save(document);
    }

    private String normalizeDocumentType(String documentType) {
        if (!StringUtils.hasText(documentType)) {
            return DEFAULT_DOCUMENT_TYPE;
        }

        return documentType.trim().toUpperCase();
    }

    private void checkStudentCanManageApplication(Application application) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || !isStudent(authentication)) {
            return;
        }


        // Doğru Kıyaslama Yöntemi:
        String currentUserEmail = authentication.getName();
        Student currentStudent = studentRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new EntityNotFoundException("Öğrenci bulunamadı."));

        if (!application.getStudent().getUserId().equals(currentStudent.getUserId())) { 
            throw new AccessDeniedException("Sadece kendi başvurunuza belge yükleyebilir veya silebilirsiniz.");
        }
    }

    private void checkStudentCanViewApplication(Application application) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || !isStudent(authentication)) {
            return;
        }

        // Doğru Kıyaslama Yöntemi:
        String currentUserEmail = authentication.getName();
        Student currentStudent = studentRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new EntityNotFoundException("Öğrenci bulunamadı."));

        if (!application.getStudent().getUserId().equals(currentStudent.getUserId())) { 
            throw new AccessDeniedException("Bu basvurunun belgelerini goruntuleme yetkiniz bulunmuyor.");
        }
    }

    private boolean isStudent(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT") || a.getAuthority().equals("STUDENT"));
    }
}
