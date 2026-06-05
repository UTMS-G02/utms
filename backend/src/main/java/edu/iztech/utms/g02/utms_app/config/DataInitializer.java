package edu.iztech.utms.g02.utms_app.config;

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

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final StaffRepository staffRepository;
    private final StudentRepository studentRepository;
    private final ApplicationRepository applicationRepository;
    private final NotificationRepository notificationRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // Sistem ilk çalıştığında eğer hiç kullanıcı yoksa, varsayılan test personel hesaplarını oluştur.
        if (userRepository.count() == 0) {
            String encodedPassword = passwordEncoder.encode("test123");

            createStaff("oidb@iyte.edu.tr", encodedPassword, "ÖİDB", "Personeli", UserRole.OIDB);
            createStaff("ydyo@iyte.edu.tr", encodedPassword, "YDYO", "Personeli", UserRole.YDYO);
            createStaff("ygk@iyte.edu.tr", encodedPassword, "YGK", "Üyesi", UserRole.YGK);
            createStaff("dean@iyte.edu.tr", encodedPassword, "Dekanlık", "Personeli", UserRole.DEAN_OFFICE);
            createStaff("faculty@iyte.edu.tr", encodedPassword, "Fakülte", "Kurulu", UserRole.FACULTY_BOARD);

            System.out.println("Test personelleri (Staff) başarıyla veritabanına eklendi.");
        }

        // Test öğrencisi (aktif): aktivasyon e-postası beklemeden doğrudan giriş yapıp
        // başvuru akışını test etmek için. Başvuru formu kişisel bilgileri bu kayıttan
        // (auth /me) otomatik ve salt-okunur doldurur. DB boş olmasa da, yalnızca bu
        // e-posta yoksa eklenir; mevcut veriyi bozmaz.
        if (userRepository.findByEmail("ogrenci@iyte.edu.tr").isEmpty()) {
            createStudent("ogrenci@iyte.edu.tr", passwordEncoder.encode("test123"),
                    "Ardacan", "Aktürk", "11111111110", "5551112233", LocalDate.of(2002, 5, 14));
            System.out.println("Test öğrencisi (ogrenci@iyte.edu.tr / test123) başarıyla eklendi.");
        }

        // Geçmiş döneme (2025-2026) ait, REDDEDİLMİŞ örnek başvuru. Amaç: "Başvurularım"
        // listesinde geçmiş kayıt + akademik yıl kolonu görünür olsun. Reddedilmiş statü
        // olduğu için tek-program kuralını ihlal etmez; öğrenci 2026-2027'ye başvurabilir.
        seedPastRejectedApplication("ogrenci@iyte.edu.tr");

        // UC-15: Her kullanıcıya örnek bildirimler (idempotent — bir kısmı okunmamış).
        seedNotifications();
    }

    // Dev seed: bildirimler artık canlı ÖİDB aksiyonlarından üretiliyor; burada yalnızca
    // test öğrencisine kutu boş kalmasın diye ÖİDB-tarzı örnekler eklenir (idempotent).
    // Personel hesaplarına seed yok — bildirimleri kendi işlemlerinden doğar.
    private void seedNotifications() {
        userRepository.findByEmail("ogrenci@iyte.edu.tr").ifPresent(user -> {
            if (notificationRepository.existsByUserId(user.getUserId())) return;

            LocalDateTime now = LocalDateTime.now();
            notificationRepository.saveAll(List.of(
                    Notification.builder()
                            .userId(user.getUserId())
                            .title("Başvurunuz Reddedildi")
                            .message("Önceki dönem başvurunuz eksik belgeler nedeniyle reddedilmiştir.")
                            .isRead(true)
                            .createdAt(now.minusDays(3))
                            .build(),
                    Notification.builder()
                            .userId(user.getUserId())
                            .title("Ön İnceleme Tamamlandı")
                            .message("Başvurunuz Öğrenci İşleri ön incelemesinden başarıyla geçti ve değerlendirme sürecine alındı.")
                            .isRead(false)
                            .createdAt(now.minusDays(1))
                            .build(),
                    Notification.builder()
                            .userId(user.getUserId())
                            .title("Belge Güncellemesi Gerekiyor")
                            .message("Başvurunuzdaki transkript belgesinin güncellenmesi istenmektedir. Lütfen başvuru detayından yeniden yükleyin.")
                            .isRead(false)
                            .createdAt(now.minusHours(4))
                            .build()
            ));
            System.out.println("UC-15: Test öğrencisi için örnek bildirimler eklendi.");
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

    private void createStudent(String email, String passwordHash, String firstName, String lastName,
                               String tckn, String phoneNumber, LocalDate dateOfBirth) {
        Student student = new Student();
        student.setEmail(email);
        student.setPasswordHash(passwordHash);
        student.setFirstName(firstName);
        student.setLastName(lastName);
        student.setRole(UserRole.STUDENT);
        student.setActive(true); // aktivasyon adımını atla, doğrudan giriş yapılabilsin
        student.setTckn(tckn);
        student.setPhoneNumber(phoneNumber);
        student.setDateOfBirth(dateOfBirth);
        student.setKvkkAcceptedAt(LocalDateTime.now());
        studentRepository.save(student);
    }

    private void createStaff(String email, String passwordHash, String firstName, String lastName, UserRole role) {
        Staff staff = new Staff();
        staff.setEmail(email);
        staff.setPasswordHash(passwordHash);
        staff.setFirstName(firstName);
        staff.setLastName(lastName);
        staff.setRole(role);
        staff.setActive(true);
        staff.setDepartmentId(1); // Varsayılan temsili bir departman id'si
        staff.setLastLoginDate(LocalDate.now());
        staffRepository.save(staff);
    }
}
