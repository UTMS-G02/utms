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

    @Test
    void create_validRequest_savesApplicationAndReturnsResponse() {
        Student student = buildStudent();
        SecurityContextHolder.setContext(new SecurityContextImpl());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("student@iyte.edu.tr", "password")
        );

        when(studentRepository.findByEmail("student@iyte.edu.tr")).thenReturn(Optional.of(student));
        when(applicationRepository.existsByStudentIdAndTargetDeptAndAcademicYear(
                student.getUserId(), "Bilgisayar Mühendisliği", "2026-2027"
        )).thenReturn(false);
        when(yoksisIntegrationService.fetchAcademicDataByTckn("12345678901"))
                .thenReturn(new YoksisStudentResponse(
                        "İYTE",
                        "Mühendislik Fakültesi",
                        "Bilgisayar Mühendisliği",
                        "3. Sınıf",
                        3.5
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
        assertThat(saved.getAcademicYear()).isEqualTo("2026-2027");
        assertThat(saved.getCurrentUniversity()).isEqualTo("İYTE");
        assertThat(saved.getCurrentDepartment()).isEqualTo("Bilgisayar Mühendisliği");
        assertThat(saved.getGpa()).isEqualTo(3.5);

        assertThat(response.getId()).isEqualTo(10);
        //assertThat(response.getStudentId()).isEqualTo(student.getUserId());
        assertThat(response.getStatus()).isEqualTo(ApplicationStatus.DRAFT);
    }

    @Test
    void create_duplicateApplication_throwsIllegalArgumentException() {
        Student student = buildStudent();
        SecurityContextHolder.setContext(new SecurityContextImpl());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("student@iyte.edu.tr", "password")
        );

        when(studentRepository.findByEmail("student@iyte.edu.tr")).thenReturn(Optional.of(student));
        when(applicationRepository.existsByStudentIdAndTargetDeptAndAcademicYear(
                student.getUserId(), "Bilgisayar Mühendisliği", "2026-2027"
        )).thenReturn(true);

        assertThatThrownBy(() -> applicationService.create(buildCreateRequest()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Bir öğrenci aynı bölüme");

        verify(applicationRepository, never()).save(any(Application.class));
    }

    @Test
    void submit_draftApplication_marksSubmittedAndReturnsResponse() {
        Student student = buildStudent();
        Application application = buildApplication(student);

        SecurityContextHolder.setContext(new SecurityContextImpl());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("student@iyte.edu.tr", "password")
        );

        when(studentRepository.findByEmail("student@iyte.edu.tr")).thenReturn(Optional.of(student));
        when(applicationRepository.findById(1)).thenReturn(Optional.of(application));
        when(applicationRepository.save(application)).thenReturn(application);

        ApplicationResponse response = applicationService.submit(1);

        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.SUBMITTED);
        assertThat(response.getStatus()).isEqualTo(ApplicationStatus.SUBMITTED);
        verify(applicationRepository).save(application);
    }

    @Test
    void processOidbReview_approved_updatesReviewData() {
        Application application = buildApplication(buildStudent());

        OidbReviewRequest request = new OidbReviewRequest();
        request.setApproved(true);
        request.setNotes("Onaylandı");

        Staff reviewer = new Staff();
        reviewer.setUserId(99);
        request.setReviewer(reviewer);

        // DÜZELTME 2: findByApplicationId yerine servisin kullandığı findById metodunu mock'ladık
        when(applicationRepository.findById(1)).thenReturn(Optional.of(application));
        when(applicationRepository.save(application)).thenReturn(application);

        //when(applicationRepository.findByApplicationId(1)).thenReturn(Optional.of(application));

        ApplicationResponse response = applicationService.processDynamicOidbReview(1, request);

        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.YDYO_REVIEW);
        assertThat(application.getOidbApproved()).isTrue();
        assertThat(application.getOidbNotes()).isEqualTo("Onaylandı");
        assertThat(application.getOidbReviewedBy()).isEqualTo(reviewer);
        assertThat(response.getStatus()).isEqualTo(ApplicationStatus.YDYO_REVIEW);
    }

    @Test
    void processYdyoReview_rejected_updatesReviewData() {
        Application application = buildApplication(buildStudent());

        YdyoReviewRequest request = new YdyoReviewRequest();
        request.setApproved(false);
        request.setNotes("Dil belgesi yetersiz");

        Staff reviewer = new Staff();
        reviewer.setUserId(77);
        request.setReviewer(reviewer);

        when(applicationRepository.findByApplicationId(1)).thenReturn(Optional.of(application));
        when(applicationRepository.save(application)).thenReturn(application);

        ApplicationResponse response = applicationService.processYdyoReview(1, request);

        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.YDYO_REJECTED);
        assertThat(application.getYdyoApproved()).isFalse();
        assertThat(application.getYdyoNotes()).isEqualTo("Dil belgesi yetersiz");
        assertThat(application.getYdyoReviewedBy()).isEqualTo(reviewer);
        assertThat(response.getStatus()).isEqualTo(ApplicationStatus.YDYO_REJECTED);
    }

    @Test
    void getApplicationById_studentOwnApplication_returnsResponse() {
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

        when(studentRepository.findByEmail("student@iyte.edu.tr")).thenReturn(Optional.of(student));
        when(applicationRepository.findById(1)).thenReturn(Optional.of(application));

        ApplicationResponse response = applicationService.getApplicationById(1);

        assertThat(response.getId()).isEqualTo(1);
        //assertThat(response.getStudentId()).isEqualTo(student.getUserId());
        assertThat(response.getStatus()).isEqualTo(ApplicationStatus.DRAFT);
        assertThat(response.getCurrentDepartment()).isEqualTo("Bilgisayar Mühendisliği");
    }

    @Test
    void getAllApplications_studentReturnsOwnApplications() {
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

        when(studentRepository.findByEmail("student@iyte.edu.tr")).thenReturn(Optional.of(student));
        when(applicationRepository.findByStudentId(student.getUserId())).thenReturn(List.of(application));

        List<ApplicationResponse> responses = applicationService.getAllApplications();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getId()).isEqualTo(1);
        assertThat(responses.get(0).getCurrentUniversity()).isEqualTo("İYTE");
    }



    @Test
    void getApplicationById_notOwner_throwsAccessDeniedException() {
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

        when(studentRepository.findByEmail("student@iyte.edu.tr")).thenReturn(Optional.of(currentStudent));
        when(applicationRepository.findById(5)).thenReturn(Optional.of(otherApplication));

        assertThatThrownBy(() -> applicationService.getApplicationById(5))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("yetkiniz bulunmuyor");
    }



    @Test
    void submit_notOwner_throwsAccessDeniedException() {
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

        when(studentRepository.findByEmail("student@iyte.edu.tr")).thenReturn(Optional.of(currentStudent));
        when(applicationRepository.findById(5)).thenReturn(Optional.of(otherApplication));

        assertThatThrownBy(() -> applicationService.submit(5))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("yetkiniz bulunmuyor");
    }

    //@Test
    /*void create_kvkkNotAccepted_throwsIllegalArgumentException() {
        Student student = buildStudent();

        SecurityContextHolder.setContext(new SecurityContextImpl());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("student@iyte.edu.tr", "password")
        );

        when(studentRepository.findByEmail("student@iyte.edu.tr")).thenReturn(Optional.of(student));
        when(applicationRepository.existsByStudentIdAndTargetDeptAndAcademicYear(
                student.getUserId(), "Bilgisayar Mühendisliği", "2026-2027"
        )).thenReturn(false);

        ApplicationCreateRequest request = buildCreateRequest();
        //request.setKvkkAccepted(false);

        assertThatThrownBy(() -> applicationService.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("KVKK");

        verify(yoksisIntegrationService, never()).fetchAcademicDataByTckn(anyString());
        verify(applicationRepository, never()).save(any(Application.class));
    }*/

    @Test
    void submit_applicationNotInDraftState_throwsIllegalStateException() {
        Student student = buildStudent();
        Application submittedApplication = buildApplication(student);
        submittedApplication.setStatus(ApplicationStatus.SUBMITTED);

        SecurityContextHolder.setContext(new SecurityContextImpl());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "student@iyte.edu.tr",
                        "password",
                        List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))
                )
        );

        when(studentRepository.findByEmail("student@iyte.edu.tr")).thenReturn(Optional.of(student));
        when(applicationRepository.findById(1)).thenReturn(Optional.of(submittedApplication));

        assertThatThrownBy(() -> applicationService.submit(1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DRAFT");

        verify(applicationRepository, never()).save(any(Application.class));
    }



    @Test
    void getAllApplications_asOidbRole_returnsAllApplications() {
        SecurityContextHolder.setContext(new SecurityContextImpl());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "oidb@iyte.edu.tr",
                        "password",
                        List.of(new SimpleGrantedAuthority("ROLE_OIDB"))
                )
        );

        Application application = buildApplication(buildStudent());
        when(applicationRepository.findAll()).thenReturn(List.of(application));

        List<ApplicationResponse> responses = applicationService.getAllApplications();

        assertThat(responses).hasSize(1);
        verify(applicationRepository).findAll();
        verifyNoInteractions(studentRepository);
    }

    @Test
    void getApplicationById_asOidbRole_skipsOwnershipCheck() {
        Application application = buildApplication(buildStudent());

        SecurityContextHolder.setContext(new SecurityContextImpl());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "oidb@iyte.edu.tr",
                        "password",
                        List.of(new SimpleGrantedAuthority("ROLE_OIDB"))
                )
        );

        when(applicationRepository.findById(1)).thenReturn(Optional.of(application));

        ApplicationResponse response = applicationService.getApplicationById(1);

        assertThat(response.getId()).isEqualTo(1);
        verify(studentRepository, never()).findByEmail(anyString());
    }

    @Test
    void getAllApplications_withStatusFilter_callsFilteredRepositoryMethod() {
        SecurityContextHolder.setContext(new SecurityContextImpl());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "oidb@iyte.edu.tr",
                        "password",
                        List.of(new SimpleGrantedAuthority("ROLE_OIDB"))
                )
        );

        Application reviewedApplication = buildApplication(buildStudent());
        reviewedApplication.setStatus(ApplicationStatus.YDYO_REVIEW);

        when(applicationRepository.findByStatus(ApplicationStatus.YDYO_REVIEW))
                .thenReturn(List.of(reviewedApplication));

        List<ApplicationResponse> responses = applicationService.getAllApplications(ApplicationStatus.YDYO_REVIEW);

        assertThat(responses).hasSize(1);
        verify(applicationRepository).findByStatus(ApplicationStatus.YDYO_REVIEW);
        verify(applicationRepository, never()).findAll();
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

    private ApplicationCreateRequest buildCreateRequest() {
        ApplicationCreateRequest request = new ApplicationCreateRequest();
        //request.setStudentId("7");
        request.setAcademicYear("2026-2027");
        request.setTargetFaculty("Mühendislik Fakültesi");
        request.setTargetDepartment("Bilgisayar Mühendisliği");
        //request.setKvkkAccepted(true);
        request.setSayYksScore(320.0);
        request.setSayYksRank(12345);
        return request;
    }
}
