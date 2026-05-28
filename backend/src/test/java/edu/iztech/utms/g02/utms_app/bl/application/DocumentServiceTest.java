package edu.iztech.utms.g02.utms_app.bl.application;

import edu.iztech.utms.g02.utms_app.api.application.dto.*;
import edu.iztech.utms.g02.utms_app.dal.application.entity.*;
import edu.iztech.utms.g02.utms_app.dal.application.repository.*;
import edu.iztech.utms.g02.utms_app.dal.user.entity.Staff;
import edu.iztech.utms.g02.utms_app.dal.user.entity.Student;
import edu.iztech.utms.g02.utms_app.dal.user.entity.UserRole;
import edu.iztech.utms.g02.utms_app.dal.user.repository.StudentRepository;
import edu.iztech.utms.g02.utms_app.integration.yoksis.YoksisIntegrationService;
import edu.iztech.utms.g02.utms_app.integration.yoksis.dto.YoksisStudentResponse;

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


    @Test
    void uploadDocument_validFile_savesDocumentRecord() throws Exception {
        Student student = buildStudent();
        Application application = buildApplication(student);

        SecurityContextHolder.setContext(new SecurityContextImpl());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "student@iyte.edu.tr",
                        "password",
                        List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))
                )
        );

        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("resume.pdf");
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream("pdf-content".getBytes()));

        when(studentRepository.findByEmail("student@iyte.edu.tr")).thenReturn(Optional.of(student));
        when(applicationRepository.findById(1)).thenReturn(Optional.of(application));

        documentService.uploadDocument(1, file);

        ArgumentCaptor<edu.iztech.utms.g02.utms_app.dal.application.entity.Document> captor =
                ArgumentCaptor.forClass(edu.iztech.utms.g02.utms_app.dal.application.entity.Document.class);
        verify(documentRepository).save(captor.capture());

        var savedDocument = captor.getValue();
        assertThat(savedDocument.getApplication()).isEqualTo(application);
        assertThat(savedDocument.getDocumentType()).isEqualTo("OTHER");
        assertThat(savedDocument.getFileName()).isEqualTo("resume.pdf");
        assertThat(savedDocument.getFilePath()).contains("uploads");
        assertThat(savedDocument.getFilePath()).endsWith("resume.pdf");
    }

    @Test
    void uploadDocument_notOwner_throwsAccessDeniedException() throws Exception {
        Student currentStudent = buildStudent();
        Student otherStudent = buildOtherStudent();
        Application otherApplication = buildApplication(otherStudent);

        SecurityContextHolder.setContext(new SecurityContextImpl());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "student@iyte.edu.tr",
                        "password",
                        List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))
                )
        );

        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);

        when(studentRepository.findByEmail("student@iyte.edu.tr")).thenReturn(Optional.of(currentStudent));
        when(applicationRepository.findById(5)).thenReturn(Optional.of(otherApplication));

        assertThatThrownBy(() -> documentService.uploadDocument(5, file))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("yetkiniz bulunmuyor");

        verify(documentRepository, never()).save(any());
    }


    @Test
    void uploadDocument_emptyFile_throwsIllegalArgumentException() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(true);

        assertThatThrownBy(() -> documentService.uploadDocument(1, file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("boş olamaz");

        verifyNoInteractions(applicationRepository, documentRepository);
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
        Application application = Application.builder()
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
        return application;
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
