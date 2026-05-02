package edu.iztech.utms.g02.utms_app.config;

import edu.iztech.utms.g02.utms_app.dal.user.entity.Staff;
import edu.iztech.utms.g02.utms_app.dal.user.entity.UserRole;
import edu.iztech.utms.g02.utms_app.dal.user.repository.StaffRepository;
import edu.iztech.utms.g02.utms_app.dal.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final StaffRepository staffRepository;
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
