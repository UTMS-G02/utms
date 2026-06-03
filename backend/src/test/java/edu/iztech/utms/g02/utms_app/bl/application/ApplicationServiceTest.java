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
        when(applicationRepository.existsByStudent_UserIdAndTargetDepartmentAndAcademicYear(
                student.getUserId(), "Bilgisayar Mühendisliği", "2026-2027"
        )).thenReturn(false);
        
        // ÖĞRENCİ 2. YARIYILDA VE ORTALAMASI 3.5 (Başvurusu KABUL EDİLMELİ)
        when(yoksisIntegrationService.fetchAcademicDataByTckn("12345678901"))
                .thenReturn(new YoksisStudentResponse(
                        "İYTE", "Mühendislik Fakültesi", "Bilgisayar Mühendisliği", 2, 3.5
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
        when(applicationRepository.existsByStudent_UserIdAndTargetDepartmentAndAcademicYear(
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
        when(applicationRepository.existsByStudent_UserIdAndTargetDepartmentAndAcademicYear(
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
        when(applicationRepository.existsByStudent_UserIdAndTargetDepartmentAndAcademicYear(
                student.getUserId(), "Bilgisayar Mühendisliği", "2026-2027"
        )).thenReturn(false);

        // YÖKSİS'ten GPA'i bilerek 2.49 dönüyoruz
        when(yoksisIntegrationService.fetchAcademicDataByTckn("12345678901"))
                .thenReturn(new YoksisStudentResponse(
                        "İYTE", "Mühendislik Fakültesi", "Bilgisayar Mühendisliği", 2, 2.49 
                ));

        ApplicationCreateRequest request = buildCreateRequest(); // KVKK true geliyor

        assertThatThrownBy(() -> applicationService.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("2.50'nin altındadır"); // Baraj uyarısını görmeliyiz

        verify(applicationRepository, never()).save(any(Application.class));
    }

    @Test
    void create_invalidSemester_throwsIllegalArgumentException() {
        Student student = buildStudent();
        setupSecurityContext("student@iyte.edu.tr", "ROLE_STUDENT");

        when(studentRepository.findByEmail("student@iyte.edu.tr")).thenReturn(Optional.of(student));
        when(applicationRepository.existsByStudent_UserIdAndTargetDepartmentAndAcademicYear(
                student.getUserId(), "Bilgisayar Mühendisliği", "2026-2027"
        )).thenReturn(false);

        // ÖĞRENCİ 3. YARIYILDA (Yani başvurduğunda 4. yarıyıla geçecek, ki bu kurala aykırı!) Ortalaması 3.5 olsa bile REDDEDİLMELİ.
        when(yoksisIntegrationService.fetchAcademicDataByTckn("12345678901"))
                .thenReturn(new YoksisStudentResponse(
                        "İYTE", "Mühendislik Fakültesi", "Bilgisayar Mühendisliği", 3, 3.5 
                ));

        ApplicationCreateRequest request = buildCreateRequest(); // KVKK true geliyor

        // Eylem ve Doğrulama
        assertThatThrownBy(() -> applicationService.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("şu an 2. veya 4. yarıyılı tamamlıyor olmalısınız"); 

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

    // ==========================================
    // YDYO — "DEĞİŞİKLİK YAPILMIŞTIR" (yeniden değerlendirme) TESTLERİ
    // Kullanıcının bildirdiği hata: zaten muaf (Onaylandı) kayda sonradan sınav notu
    // girilince çelişkili durum oluşuyordu. Artık: yeniden değerlendirilebilir AMA
    // damgalanır ve alanlar tutarlı kalır.
    // ==========================================

    @Test
    void enterYdyoExamResult_firstEntryFromExamPending_notMarkedModified() {
        Application application = buildApplication(buildStudent());
        application.setStatus(ApplicationStatus.YDYO_EXAM_PENDING); // normal sınav akışı

        YdyoExamResultRequest request = new YdyoExamResultRequest();
        request.setExamScore(72.0);
        request.setPassed(true);

        when(applicationRepository.findByApplicationId(1)).thenReturn(Optional.of(application));
        when(applicationRepository.save(application)).thenReturn(application);

        ApplicationResponse response = applicationService.enterYdyoExamResult(1, request);

        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.YDYO_ACCEPTED);
        assertThat(application.isYdyoDecisionModified()).isFalse(); // ilk karar = değişiklik değil
        assertThat(response.isModified()).isFalse();
        assertThat(response.getExamPassed()).isTrue();
    }

    @Test
    void enterYdyoExamResult_reEvaluatesExemptApplication_marksModifiedAndStaysConsistent() {
        // HATA SENARYOSU: kayıt zaten "Onaylandı/muaf" (YDYO_ACCEPTED), üstüne 50 girilir.
        Application application = buildApplication(buildStudent());
        application.setStatus(ApplicationStatus.YDYO_ACCEPTED);
        application.setYdyoApproved(true); // belgeyle muaftı

        Staff reviewer = buildYdyoStaff();
        YdyoExamResultRequest request = new YdyoExamResultRequest();
        request.setExamScore(50.0);
        request.setPassed(false);
        request.setNotes("yanlışlıkla değiştirildi");
        request.setReviewer(reviewer);

        when(applicationRepository.findByApplicationId(1)).thenReturn(Optional.of(application));
        when(applicationRepository.save(application)).thenReturn(application);

        ApplicationResponse response = applicationService.enterYdyoExamResult(1, request);

        // Damga konur:
        assertThat(application.isYdyoDecisionModified()).isTrue();
        assertThat(application.getYdyoModifiedBy()).isEqualTo(reviewer);
        assertThat(application.getYdyoModifiedDate()).isNotNull();
        assertThat(response.isModified()).isTrue();

        // Alanlar TUTARLI: sınav yolu → ydyoApproved=false → requiresExam=true, kaldı.
        // (Eski hatada bunlar eski kalıp Onaylandı+Muaf Değil çelişiyordu.)
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.YDYO_REJECTED);
        assertThat(application.getYdyoApproved()).isFalse();
        assertThat(response.getRequiresExam()).isTrue();
        assertThat(response.getExamPassed()).isFalse();
        assertThat(response.getExamScore()).isEqualTo(50.0);
    }

    @Test
    void enterYdyoExamResult_invalidStatus_throwsIllegalState() {
        Application application = buildApplication(buildStudent());
        application.setStatus(ApplicationStatus.YDYO_REVIEW); // ne EXAM_PENDING ne kesin karar

        YdyoExamResultRequest request = new YdyoExamResultRequest();
        request.setExamScore(50.0);
        request.setPassed(false);

        when(applicationRepository.findByApplicationId(1)).thenReturn(Optional.of(application));

        assertThatThrownBy(() -> applicationService.enterYdyoExamResult(1, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("sınav sonucu girilebilecek");

        verify(applicationRepository, never()).save(any(Application.class));
    }

    @Test
    void processYdyoReview_reEvaluatesDecidedApplication_marksModified() {
        Application application = buildApplication(buildStudent());
        application.setStatus(ApplicationStatus.YDYO_ACCEPTED); // zaten karara bağlı
        application.setYdyoApproved(true);

        Staff reviewer = buildYdyoStaff();
        YdyoReviewRequest request = new YdyoReviewRequest();
        request.setRequiresExam(true); // fikir değişti → sınava yönlendir
        request.setNotes("tekrar incelendi");
        request.setReviewer(reviewer);

        when(applicationRepository.findByApplicationId(1)).thenReturn(Optional.of(application));
        when(applicationRepository.save(application)).thenReturn(application);

        ApplicationResponse response = applicationService.processYdyoReview(1, request);

        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.YDYO_EXAM_PENDING);
        assertThat(application.getYdyoExamScore()).isNull(); // eski sonuç temizlendi
        assertThat(application.isYdyoDecisionModified()).isTrue();
        assertThat(response.isModified()).isTrue();
    }

    @Test
    void processYdyoReview_nonYdyoStatus_throwsIllegalState() {
        Application application = buildApplication(buildStudent());
        application.setStatus(ApplicationStatus.SUBMITTED); // YDYO aşamasında değil

        YdyoReviewRequest request = new YdyoReviewRequest();
        request.setApproved(true);

        when(applicationRepository.findByApplicationId(1)).thenReturn(Optional.of(application));

        assertThatThrownBy(() -> applicationService.processYdyoReview(1, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("YDYO'nun işlem yapabileceği");

        verify(applicationRepository, never()).save(any(Application.class));
    }

    // ==========================================
    // YDYO 3. AŞAMA: CSV TOPLU YÜKLEME TESTLERİ
    // ==========================================

    
    @Test
    void uploadYdyoExamResultsCsv_emptyFile_throwsIllegalArgumentException() {
        org.springframework.web.multipart.MultipartFile file = mock(org.springframework.web.multipart.MultipartFile.class);
        when(file.isEmpty()).thenReturn(true);
        
        assertThatThrownBy(() -> applicationService.uploadYdyoExamResultsCsv(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("boş olamaz");
                
        verify(applicationRepository, never()).save(any());
    }
    @Test
    void uploadYdyoExamResultsCsv_studentNotPendingExam_skipsStudent() throws Exception {
        // 1. HAZIRLIK (Arrange)
        String csvContent = "Adı Soyadı,E-posta,Sınav Notu\n" +
                            "Mehmet Can,mehmet@std.iztech.edu.tr,85.5\n";

        org.springframework.web.multipart.MultipartFile file = mock(org.springframework.web.multipart.MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        // DÜZELTME: Servisteki uzantı kontrolünü geçmesi için sahte dosya adı verdik
        when(file.getOriginalFilename()).thenReturn("sonuclar.csv"); 
        when(file.getInputStream()).thenReturn(new java.io.ByteArrayInputStream(csvContent.getBytes(java.nio.charset.StandardCharsets.UTF_8)));

        when(applicationRepository.findByStudent_EmailAndStatus("mehmet@std.iztech.edu.tr", ApplicationStatus.YDYO_EXAM_PENDING))
                .thenReturn(java.util.Collections.emptyList());

        // 2. EYLEM (Act)
        int processedCount = applicationService.uploadYdyoExamResultsCsv(file);

        // 3. DOĞRULAMA (Assert)
        assertThat(processedCount).isEqualTo(0); 
        verify(applicationRepository, never()).save(any(Application.class)); 
    }

    @Test
    void uploadYdyoExamResultsCsv_notCsvFormat_throwsIllegalArgumentException() {
        org.springframework.web.multipart.MultipartFile file = mock(org.springframework.web.multipart.MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("sinav_sonuclari.pdf"); // .csv DEĞİL

        assertThatThrownBy(() -> applicationService.uploadYdyoExamResultsCsv(file))
                .isInstanceOf(IllegalArgumentException.class)
                // DÜZELTME: Servisteki mesaj "sadece" diye küçük harfle başlıyordu, AssertJ büyük/küçük harfe duyarlıdır.
                .hasMessageContaining("sadece .csv uzantılı"); 

        verify(applicationRepository, never()).save(any());
    }

    @Test
    void uploadYdyoExamResultsCsv_validCsv_updatesApplicationsAndReturnsCount() throws Exception {
        // 1. HAZIRLIK (Arrange)
        String csvContent = "Adı Soyadı,E-posta,Sınav Notu\n" +
                            "Ahmet Yılmaz,ahmet@std.iztech.edu.tr,85.5\n" +
                            "Ayşe Demir,ayse@std.iztech.edu.tr,45.0\n";
                            
        org.springframework.web.multipart.MultipartFile file = mock(org.springframework.web.multipart.MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        // DÜZELTME: Servisteki uzantı kontrolünü geçmesi için sahte dosya adı verdik
        when(file.getOriginalFilename()).thenReturn("sonuclar.csv");
        when(file.getInputStream()).thenReturn(new java.io.ByteArrayInputStream(csvContent.getBytes(java.nio.charset.StandardCharsets.UTF_8)));

        Application app1 = new Application();
        app1.setApplicationId(1);
        app1.setStatus(ApplicationStatus.YDYO_EXAM_PENDING);

        Application app2 = new Application();
        app2.setApplicationId(2);
        app2.setStatus(ApplicationStatus.YDYO_EXAM_PENDING);

        when(applicationRepository.findByStudent_EmailAndStatus("ahmet@std.iztech.edu.tr", ApplicationStatus.YDYO_EXAM_PENDING))
                .thenReturn(List.of(app1));
                
        when(applicationRepository.findByStudent_EmailAndStatus("ayse@std.iztech.edu.tr", ApplicationStatus.YDYO_EXAM_PENDING))
                .thenReturn(List.of(app2));

        // 2. EYLEM (Act)
        int processedCount = applicationService.uploadYdyoExamResultsCsv(file);

        // 3. DOĞRULAMA (Assert)
        assertThat(processedCount).isEqualTo(2); 
        
        assertThat(app1.getStatus()).isEqualTo(ApplicationStatus.YDYO_ACCEPTED); 
        assertThat(app1.getYdyoExamScore()).isEqualTo(85.5);
        assertThat(app1.getYdyoReviewedDate()).isNotNull();

        assertThat(app2.getStatus()).isEqualTo(ApplicationStatus.YDYO_REJECTED); 
        assertThat(app2.getYdyoExamScore()).isEqualTo(45.0);
        assertThat(app2.getYdyoReviewedDate()).isNotNull();

        verify(applicationRepository, times(2)).save(any(Application.class));
    }



    //@Test
    /*void create_kvkkNotAccepted_throwsIllegalArgumentException() {
        Student student = buildStudent();

        SecurityContextHolder.setContext(new SecurityContextImpl());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("student@iyte.edu.tr", "password")
        );

        when(studentRepository.findByEmail("student@iyte.edu.tr")).thenReturn(Optional.of(student));
        when(applicationRepository.existsByStudent_UserIdAndTargetDepartmentAndAcademicYear(
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
    void getAllApplications_studentReturnsOwnApplications() {
        Student student = buildStudent();
        Application application = buildApplication(student);

        setupSecurityContext("student@iyte.edu.tr", "ROLE_STUDENT");

        when(studentRepository.findByEmail("student@iyte.edu.tr")).thenReturn(Optional.of(student));
        
        Page<Application> mockPage = new PageImpl<>(List.of(application));
        when(applicationRepository.findByStudent_UserId(org.mockito.ArgumentMatchers.eq(student.getUserId()), org.mockito.ArgumentMatchers.any(org.springframework.data.domain.Pageable.class)))
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

    private Staff buildYdyoStaff() {
        Staff staff = Staff.builder()
                .email("ydyo@iyte.edu.tr")
                .passwordHash("hashed")
                .firstName("YDYO")
                .lastName("Personeli")
                .role(UserRole.YDYO)
                .build();
        staff.setUserId(2);
        return staff;
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