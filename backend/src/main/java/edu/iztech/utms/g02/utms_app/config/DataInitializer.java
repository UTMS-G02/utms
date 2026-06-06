package edu.iztech.utms.g02.utms_app.config;

import edu.iztech.utms.g02.utms_app.dal.department.entity.Department;
import edu.iztech.utms.g02.utms_app.dal.department.entity.Faculty;
import edu.iztech.utms.g02.utms_app.dal.department.repository.DepartmentRepository;
import edu.iztech.utms.g02.utms_app.dal.department.repository.FacultyRepository;
import edu.iztech.utms.g02.utms_app.dal.application.entity.Application;
import edu.iztech.utms.g02.utms_app.dal.application.entity.ApplicationStatus;
import edu.iztech.utms.g02.utms_app.dal.application.repository.ApplicationRepository;
import edu.iztech.utms.g02.utms_app.dal.notification.entity.Notification;
import edu.iztech.utms.g02.utms_app.dal.notification.repository.NotificationRepository;
import edu.iztech.utms.g02.utms_app.dal.user.entity.Staff;
import edu.iztech.utms.g02.utms_app.dal.user.entity.Student;
import edu.iztech.utms.g02.utms_app.dal.user.entity.UserRole;
import edu.iztech.utms.g02.utms_app.dal.user.repository.StaffRepository;
import edu.iztech.utms.g02.utms_app.dal.user.repository.StudentRepository;
import edu.iztech.utms.g02.utms_app.dal.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private static final Map<String, String> DEPT_SLUG = Map.ofEntries(
            Map.entry("Bilgisayar Mühendisliği",            "ceng"),
            Map.entry("Elektrik-Elektronik Mühendisliği",   "eee"),
            Map.entry("Çevre Mühendisliği",                 "enve"),
            Map.entry("Gıda Mühendisliği",                  "food"),
            Map.entry("Kimya Mühendisliği",                 "che"),
            Map.entry("İnşaat Mühendisliği",                "civil"),
            Map.entry("Makine Mühendisliği",                "mech"),
            Map.entry("Malzeme Bilimi ve Mühendisliği",     "mse"),
            Map.entry("Fizik",                              "phys"),
            Map.entry("Kimya",                              "chem"),
            Map.entry("Matematik",                          "math"),
            Map.entry("Moleküler Biyoloji ve Genetik",      "mbg"),
            Map.entry("Fotonik",                            "phot"),
            Map.entry("Mimarlık",                           "arch"),
            Map.entry("Şehir ve Bölge Planlama",            "urp"),
            Map.entry("Endüstriyel Tasarım",                "ide")
    );

    private final UserRepository userRepository;
    private final StaffRepository staffRepository;
    private final StudentRepository studentRepository;
    private final FacultyRepository facultyRepository;
    private final DepartmentRepository departmentRepository;
    private final ApplicationRepository applicationRepository;
    private final NotificationRepository notificationRepository;
    private final PasswordEncoder passwordEncoder;

    // Pair 3: seed/backfill için varsayılan bölüm kontenjanı (ayarlanabilir).
    private static final int DEFAULT_QUOTA = 5;

    @Override
    public void run(String... args) throws Exception {
        seedFacultiesAndDepartments();
        backfillDepartmentQuota();

        if (userRepository.count() == 0) {
            String encodedPassword = passwordEncoder.encode("test123");

            Faculty engineering = facultyRepository.findByName("Mühendislik Fakültesi").orElseThrow();
            Faculty science = facultyRepository.findByName("Fen Fakültesi").orElseThrow();
            Faculty architecture = facultyRepository.findByName("Mimarlık Fakültesi").orElseThrow();

            createStaff("oidb@iyte.edu.tr", encodedPassword, "ÖİDB", "Personeli", UserRole.OIDB, null, null);
            createStaff("ydyo@iyte.edu.tr", encodedPassword, "YDYO", "Personeli", UserRole.YDYO, null, null);
            createStaff("dean.eng@iyte.edu.tr", encodedPassword, "Mühendislik Dekanlık", "Personeli", UserRole.DEAN_OFFICE, null, engineering.getFacultyId());
            createStaff("dean.sci@iyte.edu.tr", encodedPassword, "Fen Dekanlık", "Personeli", UserRole.DEAN_OFFICE, null, science.getFacultyId());
            createStaff("dean.arch@iyte.edu.tr", encodedPassword, "Mimarlık Dekanlık", "Personeli", UserRole.DEAN_OFFICE, null, architecture.getFacultyId());
            createStaff("faculty.eng@iyte.edu.tr", encodedPassword, "Mühendislik Fakülte", "Kurulu", UserRole.FACULTY_BOARD, null, engineering.getFacultyId());
            createStaff("faculty.sci@iyte.edu.tr", encodedPassword, "Fen Fakülte", "Kurulu", UserRole.FACULTY_BOARD, null, science.getFacultyId());
            createStaff("faculty.arch@iyte.edu.tr", encodedPassword, "Mimarlık Fakülte", "Kurulu", UserRole.FACULTY_BOARD, null, architecture.getFacultyId());

            departmentRepository.findAll().forEach(dept -> {
                String slug = DEPT_SLUG.getOrDefault(dept.getName(), "dept" + dept.getDepartmentId());
                String email = "ygk." + slug + "@iyte.edu.tr";
                createStaff(email, encodedPassword, dept.getName(), "YGK Üyesi", UserRole.YGK, dept.getDepartmentId(), null);
            });

            System.out.println("Test personelleri başarıyla veritabanına eklendi.");
        }

        if (userRepository.findByEmail("ogrenci@iyte.edu.tr").isEmpty()) {
            createStudent("ogrenci@iyte.edu.tr", passwordEncoder.encode("test123"),
                    "Ardacan", "Aktürk", "11111111110", "5551112233", LocalDate.of(2002, 5, 14));
            System.out.println("Test öğrencisi (ogrenci@iyte.edu.tr / test123) başarıyla eklendi.");
        }

        // Geçmiş döneme (2025-2026) ait, REDDEDİLMİŞ örnek başvuru. Amaç: "Başvurularım"
        // listesinde geçmiş kayıt + akademik yıl kolonu görünür olsun. Reddedilmiş statü
        // olduğu için tek-program kuralını ihlal etmez; öğrenci 2026-2027'ye başvurabilir.
        seedPastRejectedApplication("ogrenci@iyte.edu.tr");

        // Pair 3: mevcut/seed başvuruların hedef fakülte/bölüm FK'larını metinlerinden doldur.
        backfillApplicationOrgUnits();

        // UC-15: Her kullanıcıya örnek bildirimler (idempotent — bir kısmı okunmamış).
        seedNotifications();
    }

    /**
     * Pair 3: kontenjanı (quota) boş olan bölümleri varsayılan değerle doldurur. Yeni DB'de
     * bölümler kontenjansız oluşturulur; bu hem onları hem eski kayıtları doldurur (asil/yedek için).
     * İleride gerçek kontenjanlar buradan/DB'den ayarlanabilir.
     */
    private void backfillDepartmentQuota() {
        departmentRepository.findAll().forEach(dept -> {
            if (dept.getQuota() == null) {
                dept.setQuota(DEFAULT_QUOTA);
                departmentRepository.save(dept);
            }
        });
    }

    /**
     * Pair 3: {@code faculty_id}/{@code department_id} FK'sı boş olan başvuruları,
     * {@code targetFaculty}/{@code targetDepartment} metinlerinden çözerek doldurur (en iyi çaba).
     * Yeni başvurular zaten {@code ApplicationService.create} içinde bağlanır; bu, eski/seed kayıtlar içindir.
     */
    private void backfillApplicationOrgUnits() {
        applicationRepository.findAll().forEach(app -> {
            boolean changed = false;
            if (app.getFaculty() == null && app.getTargetFaculty() != null) {
                Faculty f = facultyRepository.findByName(app.getTargetFaculty()).orElse(null);
                if (f != null) { app.setFaculty(f); changed = true; }
            }
            if (app.getDepartment() == null && app.getTargetDepartment() != null) {
                Department d = departmentRepository.findByName(app.getTargetDepartment()).orElse(null);
                if (d != null) { app.setDepartment(d); changed = true; }
            }
            if (changed) applicationRepository.save(app);
        });
    }

    // Dev seed: bildirimler artık canlı ÖİDB aksiyonlarından üretiliyor; burada yalnızca
    // test öğrencisinin kutusu boş kalmasın diye, onun seed başvurusunun GERÇEK yolculuğuyla
    // birebir tutarlı bildirim(ler) eklenir (idempotent). Test öğrencisinin başvurusu
    // OIDB_REJECTED olduğundan tek bir "Başvurunuz Reddedildi" bildirimi düşer — başka adım yok.
    // Personel hesaplarına seed yok — bildirimleri kendi işlemlerinden doğar.
    private void seedNotifications() {
        userRepository.findByEmail("ogrenci@iyte.edu.tr").ifPresent(user -> {
            if (notificationRepository.existsByUserId(user.getUserId())) return;

            notificationRepository.save(Notification.builder()
                    .userId(user.getUserId())
                    .title("Başvurunuz Reddedildi")
                    .message("Önceki dönem başvurunuz eksik belgeler nedeniyle reddedilmiştir.")
                    .isRead(false)
                    .createdAt(LocalDateTime.now().minusDays(2))
                    .build());
            System.out.println("UC-15: Test öğrencisi için örnek bildirim eklendi.");
        });
    }

    private void seedPastRejectedApplication(String studentEmail) {
        studentRepository.findByEmail(studentEmail).ifPresent(student -> {
            boolean hasPast = applicationRepository
                    .findFirstByStudent_UserIdAndStatus(student.getUserId(), ApplicationStatus.OIDB_REJECTED)
                    .isPresent();
            if (hasPast) return;

            Application past = Application.builder()
                    .student(student)
                    .status(ApplicationStatus.OIDB_REJECTED)
                    .academicYear("2025-2026")
                    .semester("3")
                    .targetFaculty("Mühendislik Fakültesi")
                    .targetDepartment("Bilgisayar Mühendisliği")
                    .currentUniversity("Ege Üniversitesi")
                    .currentFaculty("Mühendislik Fakültesi")
                    .currentDepartment("Yazılım Mühendisliği")
                    .gpa(2.85)
                    .sayYksScore(455.0)
                    .sayYksRank(18500)
                    .submissionDate(LocalDate.of(2025, 7, 15))
                    .oidbApproved(false)
                    .oidbNotes("Önceki dönem başvurusu eksik belgeler nedeniyle reddedilmiştir.")
                    .build();

            applicationRepository.save(past);
            System.out.println("Geçmiş dönem (2025-2026) örnek reddedilmiş başvuru eklendi.");
        });
    }

    private void seedFacultiesAndDepartments() {
        if (facultyRepository.count() > 0) return;

        Faculty engineering = facultyRepository.save(Faculty.builder().name("Mühendislik Fakültesi").build());
        Faculty science = facultyRepository.save(Faculty.builder().name("Fen Fakültesi").build());
        Faculty architecture = facultyRepository.save(Faculty.builder().name("Mimarlık Fakültesi").build());

        List<String> engineeringDepts = List.of(
                "Bilgisayar Mühendisliği",
                "Elektrik-Elektronik Mühendisliği",
                "Çevre Mühendisliği",
                "Gıda Mühendisliği",
                "Kimya Mühendisliği",
                "İnşaat Mühendisliği",
                "Makine Mühendisliği",
                "Malzeme Bilimi ve Mühendisliği"
        );

        List<String> scienceDepts = List.of(
                "Fizik",
                "Kimya",
                "Matematik",
                "Moleküler Biyoloji ve Genetik",
                "Fotonik"
        );

        List<String> architectureDepts = List.of(
                "Mimarlık",
                "Şehir ve Bölge Planlama",
                "Endüstriyel Tasarım"
        );

        Map<Faculty, List<String>> deptMap = Map.of(
                engineering, engineeringDepts,
                science, scienceDepts,
                architecture, architectureDepts
        );

        deptMap.forEach((faculty, depts) ->
                depts.forEach(name ->
                        departmentRepository.save(Department.builder().name(name).faculty(faculty).build())
                )
        );

        System.out.println("Fakülte ve bölümler başarıyla veritabanına eklendi.");
    }

    private void createStudent(String email, String passwordHash, String firstName, String lastName,
                               String tckn, String phoneNumber, LocalDate dateOfBirth) {
        Student student = new Student();
        student.setEmail(email);
        student.setPasswordHash(passwordHash);
        student.setFirstName(firstName);
        student.setLastName(lastName);
        student.setRole(UserRole.STUDENT);
        student.setActive(true);
        student.setTckn(tckn);
        student.setPhoneNumber(phoneNumber);
        student.setDateOfBirth(dateOfBirth);
        student.setKvkkAcceptedAt(LocalDateTime.now());
        studentRepository.save(student);
    }

    private void createStaff(String email, String passwordHash, String firstName, String lastName,
                             UserRole role, Integer departmentId, Integer facultyId) {
        Staff staff = new Staff();
        staff.setEmail(email);
        staff.setPasswordHash(passwordHash);
        staff.setFirstName(firstName);
        staff.setLastName(lastName);
        staff.setRole(role);
        staff.setActive(true);
        staff.setDepartmentId(departmentId);
        staff.setFacultyId(facultyId);
        staff.setLastLoginDate(LocalDate.now());
        staffRepository.save(staff);
    }
}