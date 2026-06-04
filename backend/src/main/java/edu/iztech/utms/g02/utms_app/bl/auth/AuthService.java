package edu.iztech.utms.g02.utms_app.bl.auth;

import edu.iztech.utms.g02.utms_app.api.auth.dto.ForgotPasswordRequest;
import edu.iztech.utms.g02.utms_app.api.auth.dto.LoginRequest;
import edu.iztech.utms.g02.utms_app.api.auth.dto.LoginResponse;
import edu.iztech.utms.g02.utms_app.api.auth.dto.MeResponse;
import edu.iztech.utms.g02.utms_app.api.auth.dto.RegisterRequest;
import edu.iztech.utms.g02.utms_app.api.auth.dto.ResetPasswordRequest;
import edu.iztech.utms.g02.utms_app.dal.user.entity.Student;
import edu.iztech.utms.g02.utms_app.dal.user.entity.User;
import edu.iztech.utms.g02.utms_app.dal.user.entity.UserRole;
import edu.iztech.utms.g02.utms_app.dal.user.repository.StudentRepository;
import edu.iztech.utms.g02.utms_app.dal.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Business logic for user authentication and student registration.
 * Handles UC-1 (Sign up) and UC-12 (Login).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    /**
     * Registers a new student account.
     * Validates uniqueness of email and TCKN, and requires KVKK consent.
     *
     * @param request registration form data
     * @throws AuthException if email or TCKN already exists, or KVKK is not accepted
     */
    public void register(RegisterRequest request) {
        validateRegistration(request);
        Student student = studentRepository.save(buildStudent(request));

        String activationToken = jwtService.generateActivationToken(student.getEmail());
        String activationLink = frontendUrl + "/activate?token=" + activationToken;
        mailService.sendActivationEmail(student.getEmail(), student.getFirstName(), activationLink);
    }

    public void activateAccount(String token) {
        String email;
        try {
            email = jwtService.extractEmailFromActivationToken(token);
        } catch (Exception e) {
            throw new AuthException("Geçersiz veya süresi dolmuş aktivasyon bağlantısı.", HttpStatus.BAD_REQUEST);
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthException("Kullanıcı bulunamadı.", HttpStatus.BAD_REQUEST));

        if (user.isActive()) {
            throw new AuthException("Hesap zaten aktive edilmiş.", HttpStatus.CONFLICT);
        }

        user.setActive(true);
        userRepository.save(user);
        log.info("Account activated for {}", email);
    }

    /**
     * Authenticates a user and returns a JWT token.
     * The same error message is returned for both wrong email and wrong password
     * to prevent user enumeration attacks.
     *
     * @param request login credentials
     * @return LoginResponse containing the JWT token and basic user info
     * @throws AuthException if credentials are invalid or the account is inactive
     */
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AuthException("E-posta veya şifre hatalı.", HttpStatus.UNAUTHORIZED));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new AuthException("E-posta veya şifre hatalı.", HttpStatus.UNAUTHORIZED);
        }

        if (!user.isActive()) {
            throw new AuthException("Hesabınız henüz aktive edilmemiş. Lütfen e-postanızı kontrol edin.", HttpStatus.UNAUTHORIZED);
        }

        user.setLastLoginDate(LocalDate.now());
        userRepository.save(user);

        String token = jwtService.generateToken(user.getEmail());

        return new LoginResponse(
                user.getUserId(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole(),
                token
        );
    }

    /**
     * Returns profile info for the currently authenticated user.
     * Used by the frontend on page refresh to re-hydrate session state.
     *
     * @param email the authenticated user's email (extracted from JWT by the filter)
     * @throws AuthException if the user no longer exists or is inactive
     */
    public MeResponse getMe(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthException("Kullanıcı bulunamadı.", HttpStatus.UNAUTHORIZED));

        if (!user.isActive()) {
            throw new AuthException("Hesap aktif değil.", HttpStatus.UNAUTHORIZED);
        }

        String fullName = user.getFirstName() + " " + user.getLastName();

        // Öğrenciye özel bilgileri başlangıçta null olarak tanımlıyoruz (Personel giriş yaparsa null dönecek)
        String tckn = null;
        LocalDate dateOfBirth = null;
        String phoneNumber = null;

        // EĞER bu user objesi arka planda aslında bir Student ise:
        if (user instanceof Student) {
            Student student = (Student) user; // User'ı Student'a cast ediyoruz
            tckn = student.getTckn();
            dateOfBirth = student.getDateOfBirth();
            phoneNumber = student.getPhoneNumber();
        }

        return new MeResponse(user.getUserId(), user.getEmail(), user.getRole(), fullName, tckn, dateOfBirth, phoneNumber);
    }

    /**
     * Initiates password reset by generating a short-lived reset token.
     * Always returns successfully to avoid leaking whether an email is registered.
     */
    public void forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            String resetToken = jwtService.generateResetToken(user.getEmail());
            String resetLink = frontendUrl + "/reset-password?token=" + resetToken;
            mailService.sendPasswordResetEmail(user.getEmail(), user.getFirstName(), resetLink);
        });
    }

    /**
     * Resets the user's password after validating the reset token.
     *
     * @throws AuthException if the token is invalid or expired
     */
    public void resetPassword(ResetPasswordRequest request) {
        String email;
        try {
            email = jwtService.extractEmailFromResetToken(request.getToken());
        } catch (Exception e) {
            throw new AuthException("Geçersiz veya süresi dolmuş şifre sıfırlama bağlantısı.", HttpStatus.BAD_REQUEST);
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthException("Kullanıcı bulunamadı.", HttpStatus.BAD_REQUEST));

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    /**
     * Validates that the registration request meets all business rules.
     *
     * @throws AuthException if any validation rule is violated
     */
    private void validateRegistration(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new AuthException("Bu e-posta zaten kayıtlı.", HttpStatus.CONFLICT);
        }
        if (studentRepository.findByTckn(request.getTckn()).isPresent()) {
            throw new AuthException("Bu TCKN zaten kayıtlı.", HttpStatus.CONFLICT);
        }
        if (!Boolean.TRUE.equals(request.getKvkkAccepted())) {
            throw new AuthException("KVKK onayı zorunludur.", HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Builds a Student entity from the registration request.
     * Password is hashed with BCrypt before being stored.
     */
    private Student buildStudent(RegisterRequest request) {
        return Student.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .middleName(request.getMiddleName())
                .lastName(request.getLastName())
                .tckn(request.getTckn())
                .phoneNumber(request.getPhoneNumber())
                .dateOfBirth(request.getDateOfBirth())
                .kvkkAcceptedAt(LocalDateTime.now())
                .role(UserRole.STUDENT)
                .active(false)
                .build();
    }
}