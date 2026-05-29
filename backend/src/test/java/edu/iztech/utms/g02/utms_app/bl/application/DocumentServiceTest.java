package edu.iztech.utms.g02.utms_app.bl.application;

import edu.iztech.utms.g02.utms_app.dal.application.entity.*;
import edu.iztech.utms.g02.utms_app.dal.application.repository.*;
import edu.iztech.utms.g02.utms_app.dal.user.entity.Student;
import edu.iztech.utms.g02.utms_app.dal.user.entity.UserRole;
import edu.iztech.utms.g02.utms_app.dal.user.repository.StudentRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private ApplicationRepository applicationRepository;
    @Mock
    private StudentRepository studentRepository;
    @InjectMocks
    private DocumentService documentService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // --- YENİ EKLENEN FORMAT VE BOYUT TESTLERİ ---

    @Test
    void uploadDocument_notPdfFormat_throwsIllegalArgumentException() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getContentType()).thenReturn("image/jpeg"); // PDF DEĞİL!

        assertThatThrownBy(() -> documentService.uploadDocument(1, file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Sadece .pdf uzantılı dosyalar yüklenebilir");

        verifyNoInteractions(applicationRepository, documentRepository);
    }

    @Test
    void uploadDocument_fileTooLarge_throwsIllegalArgumentException() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getContentType()).thenReturn("application/pdf");
        when(file.getSize()).thenReturn(15L * 1024 * 1024); // 15 MB (10MB sınırını aşıyor)

        assertThatThrownBy(() -> documentService.uploadDocument(1, file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("10 MB'dan fazla olamaz");

        verifyNoInteractions(applicationRepository, documentRepository);
    }

    // --- YENİ EKLENEN KİLİT (LOCK) TESTLERİ ---

    @Test
    void uploadDocument_applicationNotDraftOrRevision_throwsAccessDeniedException() {
        Student student = buildStudent();
        Application application = buildApplication(student);
        application.setStatus(ApplicationStatus.SUBMITTED); // Başvuru gönderilmiş, kilitli!

        setupSecurityContext("student@iyte.edu.tr");

        MultipartFile file = createValidMockFile();

        when(studentRepository.findByEmail("student@iyte.edu.tr")).thenReturn(Optional.of(student));
        when(applicationRepository.findById(1)).thenReturn(Optional.of(application));

        assertThatThrownBy(() -> documentService.uploadDocument(1, file))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("inceleme aşamasında olduğu için belge ekleyemez");

        verify(documentRepository, never()).save(any());
    }

    @Test
    void uploadDocument_revisionRequestedButDocumentOidbApproved_throwsAccessDeniedException() {
        Student student = buildStudent();
        Application application = buildApplication(student);
        application.setStatus(ApplicationStatus.REVISION_REQUESTED); // Düzeltme açılmış

        setupSecurityContext("student@iyte.edu.tr");
        MultipartFile file = createValidMockFile();

        when(studentRepository.findByEmail("student@iyte.edu.tr")).thenReturn(Optional.of(student));
        when(applicationRepository.findById(1)).thenReturn(Optional.of(application));

        // Sahte olarak daha önceden yüklenmiş ve ONAYLANMIŞ bir belge yaratıyoruz
        Document existingDoc = new Document();
        existingDoc.setOidbApproved(true); 

        when(documentRepository.findByApplicationIdAndDocumentType(1, "OTHER"))
                .thenReturn(Optional.of(existingDoc));

        assertThatThrownBy(() -> documentService.uploadDocument(1, "OTHER", file))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("ÖİDB tarafından onaylanmış ve kilitlenmiştir");

        verify(documentRepository, never()).save(any());
    }

    // --- GÜNCELLENEN ESKİ TESTLER ---

    @Test
    void uploadDocument_validFile_savesDocumentRecord() throws Exception {
        Student student = buildStudent();
        Application application = buildApplication(student);

        setupSecurityContext("student@iyte.edu.tr");
        
        // Yardımcı metodumuzla kurallara uyan mükemmel bir sahte dosya yaratıyoruz
        MultipartFile file = createValidMockFile();
        when(file.getOriginalFilename()).thenReturn("resume.pdf");
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream("pdf-content".getBytes()));

        when(studentRepository.findByEmail("student@iyte.edu.tr")).thenReturn(Optional.of(student));
        when(applicationRepository.findById(1)).thenReturn(Optional.of(application));
        // Mevcut dosya yok diye simüle edelim
        when(documentRepository.findByApplicationIdAndDocumentType(1, "OTHER")).thenReturn(Optional.empty());

        documentService.uploadDocument(1, file);

        ArgumentCaptor<Document> captor = ArgumentCaptor.forClass(Document.class);
        verify(documentRepository).save(captor.capture());

        var savedDocument = captor.getValue();
        assertThat(savedDocument.getApplication()).isEqualTo(application);
        assertThat(savedDocument.getDocumentType()).isEqualTo("OTHER");
        assertThat(savedDocument.getFileName()).isEqualTo("resume.pdf");
        assertThat(savedDocument.getFilePath()).contains("uploads");
        assertThat(savedDocument.getFilePath()).endsWith("resume.pdf");
    }

    @Test
    void uploadDocument_notOwner_throwsAccessDeniedException() {
        Student currentStudent = buildStudent();
        Student otherStudent = buildOtherStudent();
        Application otherApplication = buildApplication(otherStudent);

        setupSecurityContext("student@iyte.edu.tr");
        
        MultipartFile file = createValidMockFile(); // Format doğrulamasını geçmesi lazım ki sahiplik kontrolüne ulaşsın

        when(studentRepository.findByEmail("student@iyte.edu.tr")).thenReturn(Optional.of(currentStudent));
        when(applicationRepository.findById(5)).thenReturn(Optional.of(otherApplication));

        assertThatThrownBy(() -> documentService.uploadDocument(5, file))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Sadece kendi başvurunuza"); // Mesajını güncelledim

        verify(documentRepository, never()).save(any());
    }

    @Test
    void uploadDocument_emptyFile_throwsIllegalArgumentException() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(true);

        assertThatThrownBy(() -> documentService.uploadDocument(1, file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("bos olamaz");

        verifyNoInteractions(applicationRepository, documentRepository);
    }

    // --- YARDIMCI METOTLAR (HELPER METHODS) ---

    private MultipartFile createValidMockFile() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getContentType()).thenReturn("application/pdf");
        when(file.getSize()).thenReturn(1024L); // 1 KB
        return file;
    }

    private void setupSecurityContext(String email) {
        SecurityContextHolder.setContext(new SecurityContextImpl());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        email,
                        "password",
                        List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))
                )
        );
    }

    private Student buildStudent() {
        Student student = Student.builder()
                .email("student@iyte.edu.tr")
                .passwordHash("hashed")
                .firstName("Test")
                .lastName("Student")
                .role(UserRole.STUDENT)
                .build();
        student.setTckn("12345678901");
        student.setUserId(7);
        return student;
    }

    private Application buildApplication(Student student) {
        return Application.builder()
                .applicationId(1)
                .student(student)
                .targetDepartment("Bilgisayar Mühendisliği")
                .targetFaculty("Mühendislik Fakültesi")
                .status(ApplicationStatus.DRAFT)
                .academicYear("2026-2027")
                .semester("Fall")
                .sayYksScore(320.0)
                .sayYksRank(12345)
                .currentUniversity("İYTE")
                .currentFaculty("Mühendislik Fakültesi")
                .currentDepartment("Bilgisayar Mühendisliği")
                .gpa(3.5)
                .build();
    }

    private Student buildOtherStudent() {
        Student student = Student.builder()
                .email("other@iyte.edu.tr")
                .passwordHash("hashed")
                .firstName("Other")
                .lastName("Student")
                .role(UserRole.STUDENT)
                .build();
        student.setTckn("99999999999");
        student.setUserId(9);
        return student;
    }
}