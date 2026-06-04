package edu.iztech.utms.g02.utms_app.config;

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

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final StaffRepository staffRepository;
    private final StudentRepository studentRepository;
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
