package edu.iztech.utms.g02.utms_app.bl.auth;

import edu.iztech.utms.g02.utms_app.api.auth.dto.LoginRequest;
import edu.iztech.utms.g02.utms_app.api.auth.dto.LoginResponse;
import edu.iztech.utms.g02.utms_app.api.auth.dto.RegisterRequest;
import edu.iztech.utms.g02.utms_app.dal.user.entity.Student;
import edu.iztech.utms.g02.utms_app.dal.user.entity.UserRole;
import edu.iztech.utms.g02.utms_app.dal.user.repository.StudentRepository;
import edu.iztech.utms.g02.utms_app.dal.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private StudentRepository studentRepository;
    @Mock private JwtService jwtService;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks private AuthService authService;

    // ─── register ────────────────────────────────────────────────────────────

    @Test
    void register_validRequest_savesStudent() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(studentRepository.findByTckn(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");

        authService.register(buildRegisterRequest("new@iyte.edu.tr", "12345678901", true));

        verify(studentRepository, times(1)).save(any(Student.class));
    }

    @Test
    void register_duplicateEmail_throwsAuthException() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(buildStudent()));

        assertThatThrownBy(() -> authService.register(buildRegisterRequest("exists@iyte.edu.tr", "12345678901", true)))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("e-posta");
    }

    @Test
    void register_duplicateTckn_throwsAuthException() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(studentRepository.findByTckn(anyString())).thenReturn(Optional.of(buildStudent()));

        assertThatThrownBy(() -> authService.register(buildRegisterRequest("new@iyte.edu.tr", "12345678901", true)))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("TCKN");
    }

    @Test
    void register_kvkkNotAccepted_throwsAuthException() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(studentRepository.findByTckn(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.register(buildRegisterRequest("new@iyte.edu.tr", "12345678901", false)))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("KVKK");
    }

    // ─── login ───────────────────────────────────────────────────────────────

    @Test
    void login_validCredentials_returnsTokenAndUserInfo() {
        Student student = buildStudent();
        when(userRepository.findByEmail("test@iyte.edu.tr")).thenReturn(Optional.of(student));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);
        when(jwtService.generateToken("test@iyte.edu.tr")).thenReturn("jwt-token");

        LoginResponse response = authService.login(buildLoginRequest("test@iyte.edu.tr", "password123"));

        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getFirstName()).isEqualTo("Test");
        assertThat(response.getRole()).isEqualTo(UserRole.STUDENT);
    }

    @Test
    void login_emailNotFound_throwsAuthException() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(buildLoginRequest("ghost@iyte.edu.tr", "password123")))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("hatalı");
    }

    @Test
    void login_wrongPassword_throwsAuthException() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(buildStudent()));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(buildLoginRequest("test@iyte.edu.tr", "wrongpass")))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("hatalı");
    }

    @Test
    void login_inactiveAccount_throwsAuthException() {
        Student inactive = Student.builder()
                .email("test@iyte.edu.tr")
                .passwordHash("hashed")
                .firstName("Test")
                .lastName("User")
                .role(UserRole.STUDENT)
                .active(false)
                .build();

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(inactive));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        assertThatThrownBy(() -> authService.login(buildLoginRequest("test@iyte.edu.tr", "password123")))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("aktif");
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private Student buildStudent() {
        return Student.builder()
                .email("test@iyte.edu.tr")
                .passwordHash("hashed")
                .firstName("Test")
                .lastName("User")
                .role(UserRole.STUDENT)
                .build();
    }

    private RegisterRequest buildRegisterRequest(String email, String tckn, boolean kvkk) {
        RegisterRequest req = new RegisterRequest();
        req.setEmail(email);
        req.setPassword("password123");
        req.setFirstName("Test");
        req.setLastName("User");
        req.setTckn(tckn);
        req.setKvkkAccepted(kvkk);
        return req;
    }

    private LoginRequest buildLoginRequest(String email, String password) {
        LoginRequest req = new LoginRequest();
        req.setEmail(email);
        req.setPassword(password);
        return req;
    }
}