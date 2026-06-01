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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    @Mock private ApplicationRepository applicationRepository;
    @Mock private StudentRepository studentRepository;
    @Mock private YoksisIntegrationService yoksisIntegrationService;

    @InjectMocks private ApplicationService applicationService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ==========================================
    // 1. CREATE (YENİ BAŞVURU) TESTLERİ
    // ==========================================

    @Test
    void create_validRequest_savesApplicationAndReturnsResponse() {
        Student student = buildStudent();
        setupSecurityContext("student@iyte.edu.tr", "ROLE_STUDENT");

        when(studentRepository.findByEmail("student@iyte.edu.tr")).thenReturn(Optional.of(student));
        when(applicationRepository.existsByStudentIdAndTargetDeptAndAcademicYear(
                student.getUserId(), "Bilgisayar Mühendisliği", "2026-2027"
        )).thenReturn(false);
        
        // YÖKSİS'ten dönen GPA 3.5 (2.50 barajından yüksek, sorunsuz geçmeli)
        when(yoksisIntegrationService.fetchAcademicDataByTckn("12345678901"))
                .thenReturn(new YoksisStudentResponse(
                        "İYTE", "Mühendislik Fakültesi", "Bilgisayar Mühendisliği", "3. Sınıf", 3.5
                ));
                
        when(applicationRepository.save(any(Application.class))).thenAnswer(invocation -> {
            Application application = invocation.getArgument(0);
            application.setApplicationId(10);
            return application;
        });

        ApplicationResponse response = applicationService.create(buildCreateRequest());

        ArgumentCaptor<Application> captor = ArgumentCaptor.forClass(Application.class);
        verify(applicationRepository).save(captor.capture());
        Application saved = captor.getValue();

        assertThat(saved.getStudent()).isEqualTo(student);
        assertThat(saved.getStatus()).isEqualTo(ApplicationStatus.DRAFT);
        assertThat(saved.getGpa()).isEqualTo(3.5);
        assertThat(response.getId()).isEqualTo(10);
    }

    @Test
    void create_duplicateApplication_throwsIllegalArgumentException() {
        Student student = buildStudent();
        setupSecurityContext("student@iyte.edu.tr", "ROLE_STUDENT");

        when(studentRepository.findByEmail("student@iyte.edu.tr")).thenReturn(Optional.of(student));
        when(applicationRepository.existsByStudentIdAndTargetDeptAndAcademicYear(
                student.getUserId(), "Bilgisayar Mühendisliği", "2026-2027"
        )).thenReturn(true);

        assertThatThrownBy(() -> applicationService.create(buildCreateRequest()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("birden fazla başvuru yapamaz");

        verify(applicationRepository, never()).save(any(Application.class));
    }

    @Test
    void create_kvkkNotAccepted_throwsIllegalArgumentException() {
        Student student = buildStudent();
        setupSecurityContext("student@iyte.edu.tr", "ROLE_STUDENT");

        when(studentRepository.findByEmail("student@iyte.edu.tr")).thenReturn(Optional.of(student));
        when(applicationRepository.existsByStudentIdAndTargetDeptAndAcademicYear(
                student.getUserId(), "Bilgisayar Mühendisliği", "2026-2027"
        )).thenReturn(false);

        ApplicationCreateRequest request = buildCreateRequest();
        request.setKvkkAccepted(false); // Bilerek KVKK'yı reddediyoruz

        assertThatThrownBy(() -> applicationService.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("KVKK onayı zorunludur");

        verify(yoksisIntegrationService, never()).fetchAcademicDataByTckn(anyString());
        verify(applicationRepository, never()).save(any(Application.class));
    }

    @Test
    void create_gpaBelowMinimum_throwsIllegalArgumentException() {
        Student student = buildStudent();
        setupSecurityContext("student@iyte.edu.tr", "ROLE_STUDENT");

        when(studentRepository.findByEmail("student@iyte.edu.tr")).thenReturn(Optional.of(student));
        when(applicationRepository.existsByStudentIdAndTargetDeptAndAcademicYear(
                student.getUserId(), "Bilgisayar Mühendisliği", "2026-2027"
        )).thenReturn(false);

        // YÖKSİS'ten GPA'i bilerek 2.49 dönüyoruz
        when(yoksisIntegrationService.fetchAcademicDataByTckn("12345678901"))
                .thenReturn(new YoksisStudentResponse(
                        "İYTE", "Mühendislik Fakültesi", "Bilgisayar Mühendisliği", "3. Sınıf", 2.49 
                ));

        ApplicationCreateRequest request = buildCreateRequest(); // KVKK true geliyor

        assertThatThrownBy(() -> applicationService.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("2.50'nin altındadır"); // Baraj uyarısını görmeliyiz

        verify(applicationRepository, never()).save(any(Application.class));
    }

    // ==========================================
    // 2. SUBMİT VE OİDB TESTLERİ
    // ==========================================

    @Test
    void submit_draftApplication_marksSubmittedAndReturnsResponse() {
        Student student = buildStudent();
        Application application = buildApplication(student);

        setupSecurityContext("student@iyte.edu.tr", "ROLE_STUDENT");

        when(studentRepository.findByEmail("student@iyte.edu.tr")).thenReturn(Optional.of(student));
        when(applicationRepository.findById(1)).thenReturn(Optional.of(application));
        when(applicationRepository.save(application)).thenReturn(application);

        ApplicationResponse response = applicationService.submit(1);

        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.SUBMITTED);
        assertThat(response.getStatus()).isEqualTo(ApplicationStatus.SUBMITTED);
    }

    @Test
    void submit_applicationNotInDraftState_throwsIllegalStateException() {
        Student student = buildStudent();
        Application application = buildApplication(student);
        application.setStatus(ApplicationStatus.SUBMITTED); // Artık DRAFT değil

        setupSecurityContext("student@iyte.edu.tr", "ROLE_STUDENT");

        when(studentRepository.findByEmail("student@iyte.edu.tr")).thenReturn(Optional.of(student));
        when(applicationRepository.findById(1)).thenReturn(Optional.of(application));

        assertThatThrownBy(() -> applicationService.submit(1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DRAFT veya REVISION_REQUESTED");
    }

    @Test
    void processOidbReview_approved_updatesReviewData() {
        Application application = buildApplication(buildStudent());
        application.setStatus(ApplicationStatus.SUBMITTED); // OİDB işlem yapabilsin diye

        OidbReviewRequest request = new OidbReviewRequest();
        request.setApproved(true);
        request.setNotes("Onaylandı");

        when(applicationRepository.findById(1)).thenReturn(Optional.of(application));
        when(applicationRepository.save(application)).thenReturn(application);

        ApplicationResponse response = applicationService.processDynamicOidbReview(1, request);

        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.YDYO_REVIEW);
        assertThat(application.getOidbApproved()).isTrue();
    }

    @Test
    void processOidbReview_draftApplication_throwsIllegalStateException() {
        Application application = buildApplication(buildStudent()); // DRAFT

        OidbReviewRequest request = new OidbReviewRequest();
        request.setApproved(true);

        when(applicationRepository.findById(1)).thenReturn(Optional.of(application));

        assertThatThrownBy(() -> applicationService.processDynamicOidbReview(1, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("OİDB'nin işlem yapabileceği bir statüde değil");
    }

    // ==========================================
    // 3. YDYO İŞLEMLERİ (YENİ VE GÜNCELLENMİŞ)
    // ==========================================

    @Test
    void processYdyoReview_requiresExam_setsExamPending() {
        Application application = buildApplication(buildStudent());
        application.setStatus(ApplicationStatus.YDYO_REVIEW); // YDYO evrak incelemesinde

        YdyoReviewRequest request = new YdyoReviewRequest();
        request.setRequiresExam(true);
        request.setNotes("Belge yetersiz, sınava girmeli");

        when(applicationRepository.findByApplicationId(1)).thenReturn(Optional.of(application));
        when(applicationRepository.save(application)).thenReturn(application);

        ApplicationResponse response = applicationService.processYdyoReview(1, request);

        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.YDYO_EXAM_PENDING);
        assertThat(response.getStatus()).isEqualTo(ApplicationStatus.YDYO_EXAM_PENDING);
    }

    @Test
    void processYdyoReview_approved_setsYdyoAccepted() {
        Application application = buildApplication(buildStudent());
        application.setStatus(ApplicationStatus.YDYO_REVIEW);

        YdyoReviewRequest request = new YdyoReviewRequest();
        request.setApproved(true); // Direkt muaf oldu
        
        when(applicationRepository.findByApplicationId(1)).thenReturn(Optional.of(application));
        when(applicationRepository.save(application)).thenReturn(application);

        ApplicationResponse response = applicationService.processYdyoReview(1, request);

        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.YDYO_ACCEPTED);
        assertThat(response.getStatus()).isEqualTo(ApplicationStatus.YDYO_ACCEPTED);
    }

    @Test
    void enterYdyoExamResult_passed_updatesStatusToAccepted() {
        Application application = buildApplication(buildStudent());
        application.setStatus(ApplicationStatus.YDYO_EXAM_PENDING); // Sınav bekliyor

        YdyoExamResultRequest request = new YdyoExamResultRequest();
        request.setPassed(true);
        request.setExamScore(85.5);

        when(applicationRepository.findByApplicationId(1)).thenReturn(Optional.of(application));
        when(applicationRepository.save(application)).thenReturn(application);

        ApplicationResponse response = applicationService.enterYdyoExamResult(1, request);

        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.YDYO_ACCEPTED);
        assertThat(application.getYdyoExamScore()).isEqualTo(85.5);
    }

    @Test
    void enterYdyoExamResult_failed_updatesStatusToRejected() {
        Application application = buildApplication(buildStudent());
        application.setStatus(ApplicationStatus.YDYO_EXAM_PENDING);

        YdyoExamResultRequest request = new YdyoExamResultRequest();
        request.setPassed(false); // Sınavdan kaldı

        when(applicationRepository.findByApplicationId(1)).thenReturn(Optional.of(application));
        when(applicationRepository.save(application)).thenReturn(application);

        ApplicationResponse response = applicationService.enterYdyoExamResult(1, request);

        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.YDYO_REJECTED);
    }

    @Test
    void enterYdyoExamResult_notPendingExam_throwsIllegalStateException() {
        Application application = buildApplication(buildStudent());
        application.setStatus(ApplicationStatus.DRAFT); // Sınav bekleyen bir durumu yok!

        YdyoExamResultRequest request = new YdyoExamResultRequest();
        request.setPassed(true);

        when(applicationRepository.findByApplicationId(1)).thenReturn(Optional.of(application));

        assertThatThrownBy(() -> applicationService.enterYdyoExamResult(1, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("bekleyen bir sınavı bulunmuyor");
    }

    // ==========================================
    // 4. LİSTELEME VE ERİŞİM KONTROL (GET & SAYFALAMA)
    // ==========================================

    @Test
    void getAllApplications_studentReturnsOwnApplications() {
        Student student = buildStudent();
        Application application = buildApplication(student);

        setupSecurityContext("student@iyte.edu.tr", "ROLE_STUDENT");

        when(studentRepository.findByEmail("student@iyte.edu.tr")).thenReturn(Optional.of(student));
        
        Page<Application> mockPage = new PageImpl<>(List.of(application));
        when(applicationRepository.findByStudentId(org.mockito.ArgumentMatchers.eq(student.getUserId()), org.mockito.ArgumentMatchers.any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(mockPage);

        Page<ApplicationResponse> pageResult = applicationService.getAllApplications(null, 0, 20);

        assertThat(pageResult.getContent()).hasSize(1);
        assertThat(pageResult.getContent().get(0).getId()).isEqualTo(1);
    }

    @Test
    void getAllApplications_asOidbRole_returnsAllApplications() {
        setupSecurityContext("oidb@iyte.edu.tr", "ROLE_OIDB");

        Application application = buildApplication(buildStudent());
        Page<Application> mockPage = new PageImpl<>(List.of(application));

        when(applicationRepository.findAll(org.mockito.ArgumentMatchers.any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(mockPage);

        Page<ApplicationResponse> pageResult = applicationService.getAllApplications(null, 0, 20);

        assertThat(pageResult.getContent()).hasSize(1);
    }

    // ==========================================
    // YARDIMCI METOTLAR (HELPER METHODS)
    // ==========================================

    private void setupSecurityContext(String email, String role) {
        SecurityContextHolder.setContext(new SecurityContextImpl());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, "password", List.of(new SimpleGrantedAuthority(role)))
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

    private ApplicationCreateRequest buildCreateRequest() {
        ApplicationCreateRequest request = new ApplicationCreateRequest();
        request.setAcademicYear("2026-2027");
        request.setTargetFaculty("Mühendislik Fakültesi");
        request.setTargetDepartment("Bilgisayar Mühendisliği");
        request.setKvkkAccepted(true); // Testin geçmesi için onaylıyoruz
        request.setSayYksScore(320.0);
        request.setSayYksRank(12345);
        return request;
    }
}