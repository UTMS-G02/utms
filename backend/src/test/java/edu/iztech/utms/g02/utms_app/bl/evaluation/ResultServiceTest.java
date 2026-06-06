package edu.iztech.utms.g02.utms_app.bl.evaluation;

import edu.iztech.utms.g02.utms_app.api.evaluation.dto.PublishedResultResponse;
import edu.iztech.utms.g02.utms_app.dal.application.entity.Application;
import edu.iztech.utms.g02.utms_app.dal.application.entity.ApplicationStatus;
import edu.iztech.utms.g02.utms_app.dal.application.repository.ApplicationRepository;
import edu.iztech.utms.g02.utms_app.dal.department.entity.Department;
import edu.iztech.utms.g02.utms_app.dal.evaluation.entity.EvaluationResult;
import edu.iztech.utms.g02.utms_app.dal.evaluation.entity.PublishedResult;
import edu.iztech.utms.g02.utms_app.dal.evaluation.repository.EvaluationResultRepository;
import edu.iztech.utms.g02.utms_app.dal.evaluation.repository.PublishedResultRepository;
import edu.iztech.utms.g02.utms_app.dal.user.entity.Student;
import edu.iztech.utms.g02.utms_app.dal.user.entity.UserRole;
import edu.iztech.utms.g02.utms_app.dal.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResultServiceTest {

    @Mock private ApplicationRepository applicationRepository;
    @Mock private PublishedResultRepository publishedResultRepository;
    @Mock private EvaluationResultRepository evaluationResultRepository;
    @Mock private UserRepository userRepository;
    @Mock private edu.iztech.utms.g02.utms_app.bl.notification.NotificationService notificationService;

    @InjectMocks private ResultService resultService;

    @BeforeEach
    void stubSecondarySources() {
        // computeListTypes() önceden ACCEPTED olanları da sorgular; finalize guard FACULTY_BOARD_REVIEW'ı sorgular.
        lenient().when(applicationRepository.findByStatus(eq(ApplicationStatus.ACCEPTED), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        lenient().when(applicationRepository.findByStatus(eq(ApplicationStatus.FACULTY_BOARD_REVIEW), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // ==========================================
    // YAYINLAMA — publishResults()
    // ==========================================

    @Test
    void publish_resultPublishedApp_createsAcceptedRowAndSetsAccepted() {
        loginAs("oidb@iyte.edu.tr", "ROLE_OIDB");
        Application app = buildApp(1, ApplicationStatus.OIDB_FINAL_REVIEW);
        when(userRepository.findByEmail("oidb@iyte.edu.tr")).thenReturn(Optional.of(buildPublisher()));
        when(applicationRepository.findByStatus(eq(ApplicationStatus.OIDB_FINAL_REVIEW), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(app)));
        when(applicationRepository.findByStatus(eq(ApplicationStatus.REJECTED), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(publishedResultRepository.existsByApplication_ApplicationId(1)).thenReturn(false);
        when(publishedResultRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        int count = resultService.publishResults();

        assertThat(count).isEqualTo(1);
        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.ACCEPTED);
        verify(publishedResultRepository).save(argThat(r ->
                "ACCEPTED".equals(r.getFinalDecision()) && r.getApplication() == app));
    }

    @Test
    void publish_idempotent_alreadyPublishedAppIsSkipped() {
        loginAs("oidb@iyte.edu.tr", "ROLE_OIDB");
        Application app = buildApp(2, ApplicationStatus.OIDB_FINAL_REVIEW);
        when(userRepository.findByEmail("oidb@iyte.edu.tr")).thenReturn(Optional.of(buildPublisher()));
        when(applicationRepository.findByStatus(eq(ApplicationStatus.OIDB_FINAL_REVIEW), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(app)));
        when(applicationRepository.findByStatus(eq(ApplicationStatus.REJECTED), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(publishedResultRepository.existsByApplication_ApplicationId(2)).thenReturn(true);

        int count = resultService.publishResults();

        assertThat(count).isEqualTo(0);
        verify(publishedResultRepository, never()).save(any());
        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.OIDB_FINAL_REVIEW);
    }

    @Test
    void publish_rejectedApp_createsRejectedRow_andKeepsStatus() {
        // TC-10.4: Dekan, Fakülte Kurulu reddini ÖİDB'ye iletince statü REJECTED olur; ÖİDB yayınlar.
        loginAs("oidb@iyte.edu.tr", "ROLE_OIDB");
        Application app = buildApp(3, ApplicationStatus.REJECTED);
        when(userRepository.findByEmail("oidb@iyte.edu.tr")).thenReturn(Optional.of(buildPublisher()));
        when(applicationRepository.findByStatus(eq(ApplicationStatus.OIDB_FINAL_REVIEW), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(applicationRepository.findByStatus(eq(ApplicationStatus.REJECTED), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(app)));
        when(publishedResultRepository.existsByApplication_ApplicationId(3)).thenReturn(false);
        when(publishedResultRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        int count = resultService.publishResults();

        assertThat(count).isEqualTo(1);
        verify(publishedResultRepository).save(argThat(r -> "REJECTED".equals(r.getFinalDecision())));
        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.REJECTED); // statü değişmez, yalnızca yayın kaydı
    }

    @Test
    void publish_assignsPrimaryAndWaitlistByDepartmentQuota() {
        // Bölüm kontenjanı 1: bölüm-içi sıralamada ilk → asil (PRIMARY), ikinci → yedek (WAITLIST). (TC-11.0 POST-3)
        loginAs("oidb@iyte.edu.tr", "ROLE_OIDB");
        Department dept = Department.builder().departmentId(100).name("Bilgisayar Mühendisliği").quota(1).build();
        Application a1 = acceptedAppInDept(11, dept);   // ranking 1
        Application a2 = acceptedAppInDept(12, dept);   // ranking 2
        when(userRepository.findByEmail("oidb@iyte.edu.tr")).thenReturn(Optional.of(buildPublisher()));
        when(applicationRepository.findByStatus(eq(ApplicationStatus.OIDB_FINAL_REVIEW), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(a1, a2)));
        when(applicationRepository.findByStatus(eq(ApplicationStatus.REJECTED), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(evaluationResultRepository.findByApplication_ApplicationId(11))
                .thenReturn(Optional.of(EvaluationResult.builder().ranking(1).build()));
        when(evaluationResultRepository.findByApplication_ApplicationId(12))
                .thenReturn(Optional.of(EvaluationResult.builder().ranking(2).build()));
        when(publishedResultRepository.existsByApplication_ApplicationId(any())).thenReturn(false);
        when(publishedResultRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        int count = resultService.publishResults();

        assertThat(count).isEqualTo(2);
        ArgumentCaptor<PublishedResult> captor = ArgumentCaptor.forClass(PublishedResult.class);
        verify(publishedResultRepository, times(2)).save(captor.capture());
        Map<Integer, String> listTypeByApp = captor.getAllValues().stream()
                .collect(Collectors.toMap(r -> r.getApplication().getApplicationId(), PublishedResult::getListType));
        assertThat(listTypeByApp.get(11)).isEqualTo("PRIMARY");   // sıra 1, kontenjan 1 → asil
        assertThat(listTypeByApp.get(12)).isEqualTo("WAITLIST");  // sıra 2 → yedek
    }

    @Test
    void publish_blockedWhenFacultyBoardDecisionsPending() {
        // TC-6.2: 'Fakülte Kurulu Kararı Bekleniyor' (FACULTY_BOARD_REVIEW) varken yayın engellenir.
        loginAs("oidb@iyte.edu.tr", "ROLE_OIDB");
        when(userRepository.findByEmail("oidb@iyte.edu.tr")).thenReturn(Optional.of(buildPublisher()));
        when(applicationRepository.findByStatus(eq(ApplicationStatus.FACULTY_BOARD_REVIEW), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(buildApp(1, ApplicationStatus.FACULTY_BOARD_REVIEW))));

        assertThatThrownBy(() -> resultService.publishResults())
                .isInstanceOf(IllegalStateException.class);
        verify(publishedResultRepository, never()).save(any());
    }

    @Test
    void publish_notificationFailure_doesNotRollbackPublish() {
        // TC-6.3: Bildirim hatası yayını geri almamalı — sonuç kaydedilmeli, hata loglanmalı.
        loginAs("oidb@iyte.edu.tr", "ROLE_OIDB");
        Application app = buildApp(99, ApplicationStatus.OIDB_FINAL_REVIEW);
        when(userRepository.findByEmail("oidb@iyte.edu.tr")).thenReturn(Optional.of(buildPublisher()));
        when(applicationRepository.findByStatus(eq(ApplicationStatus.FACULTY_BOARD_REVIEW), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(applicationRepository.findByStatus(eq(ApplicationStatus.OIDB_FINAL_REVIEW), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(app)));
        when(applicationRepository.findByStatus(eq(ApplicationStatus.ACCEPTED), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(applicationRepository.findByStatus(eq(ApplicationStatus.REJECTED), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(publishedResultRepository.existsByApplication_ApplicationId(99)).thenReturn(false);
        when(publishedResultRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        org.mockito.Mockito.doThrow(new RuntimeException("SMTP bağlantısı kesildi"))
                .when(notificationService).create(any(), any(), any());

        int count = resultService.publishResults();

        assertThat(count).isEqualTo(1);
        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.ACCEPTED);
        verify(publishedResultRepository).save(any());
    }

    // ==========================================
    // SONUÇ GÖRÜNTÜLEME — getResults()
    // ==========================================

    @Test
    void getResults_studentRole_returnsOnlyOwnResult() {
        loginAs("student@iyte.edu.tr", "ROLE_STUDENT");
        Application app = buildAppWithStudent(7, "student@iyte.edu.tr");
        PublishedResult pr = PublishedResult.builder()
                .application(app).finalDecision("ACCEPTED").build();
        when(publishedResultRepository.findByApplication_Student_Email("student@iyte.edu.tr"))
                .thenReturn(List.of(pr));

        List<PublishedResultResponse> results = resultService.getResults();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getFinalDecision()).isEqualTo("ACCEPTED");
        assertThat(results.get(0).getApplicationId()).isEqualTo(7);
        verify(publishedResultRepository, never()).findAll();
    }

    @Test
    void getResults_oidbRole_returnsAllResults() {
        loginAs("oidb@iyte.edu.tr", "ROLE_OIDB");
        PublishedResult r1 = PublishedResult.builder()
                .application(buildAppWithStudent(8, "s1@iyte.edu.tr")).finalDecision("ACCEPTED").build();
        PublishedResult r2 = PublishedResult.builder()
                .application(buildAppWithStudent(9, "s2@iyte.edu.tr")).finalDecision("REJECTED").build();
        when(publishedResultRepository.findAll()).thenReturn(List.of(r1, r2));

        List<PublishedResultResponse> results = resultService.getResults();

        assertThat(results).hasSize(2);
        verify(publishedResultRepository, never()).findByApplication_Student_Email(any());
    }

    @Test
    void getResults_studentWithNoPublishedResult_returnsEmptyList() {
        loginAs("student@iyte.edu.tr", "ROLE_STUDENT");
        when(publishedResultRepository.findByApplication_Student_Email("student@iyte.edu.tr"))
                .thenReturn(List.of());

        List<PublishedResultResponse> results = resultService.getResults();

        assertThat(results).isEmpty();
    }

    // ==========================================
    // YARDIMCI METOTLAR
    // ==========================================

    private void loginAs(String email, String role) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, "pw",
                        List.of(new SimpleGrantedAuthority(role))));
    }

    private Application acceptedAppInDept(Integer id, Department dept) {
        Student student = Student.builder()
                .email("s" + id + "@iyte.edu.tr").firstName("Test").lastName("Student")
                .role(UserRole.STUDENT).build();
        student.setUserId(id);
        return Application.builder()
                .applicationId(id)
                .status(ApplicationStatus.OIDB_FINAL_REVIEW)
                .student(student)
                .department(dept)
                .build();
    }

    private Application buildApp(Integer id, ApplicationStatus status) {
        Student student = Student.builder()
                .email("student@iyte.edu.tr")
                .firstName("Test").lastName("Student")
                .role(UserRole.STUDENT)
                .build();
        student.setUserId(id);
        return Application.builder()
                .applicationId(id).status(status).student(student).build();
    }

    private Application buildAppWithStudent(Integer id, String email) {
        Student student = Student.builder()
                .email(email).firstName("Test").lastName("Student")
                .role(UserRole.STUDENT)
                .build();
        student.setUserId(id);
        return Application.builder()
                .applicationId(id).status(ApplicationStatus.ACCEPTED).student(student).build();
    }

    private Student buildPublisher() {
        Student publisher = Student.builder()
                .email("oidb@iyte.edu.tr")
                .firstName("OIDB").lastName("User")
                .role(UserRole.OIDB)
                .build();
        publisher.setUserId(99);
        return publisher;
    }
}
