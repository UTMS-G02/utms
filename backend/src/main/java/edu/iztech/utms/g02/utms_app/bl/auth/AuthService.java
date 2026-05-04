package edu.iztech.utms.g02.utms_app.bl.auth;

import edu.iztech.utms.g02.utms_app.api.auth.dto.LoginRequest;
import edu.iztech.utms.g02.utms_app.api.auth.dto.LoginResponse;
import edu.iztech.utms.g02.utms_app.api.auth.dto.MeResponse;
import edu.iztech.utms.g02.utms_app.api.auth.dto.RegisterRequest;
import edu.iztech.utms.g02.utms_app.dal.user.entity.Student;
import edu.iztech.utms.g02.utms_app.dal.user.entity.User;
import edu.iztech.utms.g02.utms_app.dal.user.entity.UserRole;
import edu.iztech.utms.g02.utms_app.dal.user.repository.StudentRepository;
import edu.iztech.utms.g02.utms_app.dal.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Business logic for user authentication and student registration.
 * Handles UC-1 (Sign up) and UC-12 (Login).
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    /**
     * Registers a new student account.
     * Validates uniqueness of email and TCKN, and requires KVKK consent.
     *
     * @param request registration form data
     * @throws AuthException if email or TCKN already exists, or KVKK is not accepted
     */
    public void register(RegisterRequest request) {
        validateRegistration(request);
        studentRepository.save(buildStudent(request));
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
                .orElseThrow(() -> new AuthException("E-posta veya şifre hatalı."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new AuthException("E-posta veya şifre hatalı.");
        }

        if (!user.isActive()) {
            throw new AuthException("Hesap aktif değil.");
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
                .orElseThrow(() -> new AuthException("Kullanıcı bulunamadı."));

        if (!user.isActive()) {
            throw new AuthException("Hesap aktif değil.");
        }

        String fullName = user.getFirstName() + " " + user.getLastName();
        return new MeResponse(user.getUserId(), user.getEmail(), user.getRole(), fullName);
    }

    /**
     * Validates that the registration request meets all business rules.
     *
     * @throws AuthException if any validation rule is violated
     */
    private void validateRegistration(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new AuthException("Bu e-posta zaten kayıtlı.");
        }
        if (studentRepository.findByTckn(request.getTckn()).isPresent()) {
            throw new AuthException("Bu TCKN zaten kayıtlı.");
        }
        if (!Boolean.TRUE.equals(request.getKvkkAccepted())) {
            throw new AuthException("KVKK onayı zorunludur.");
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
                .build();
    }
}