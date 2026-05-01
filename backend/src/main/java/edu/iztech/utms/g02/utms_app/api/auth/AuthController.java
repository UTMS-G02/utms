package edu.iztech.utms.g02.utms_app.api.auth;

import edu.iztech.utms.g02.utms_app.api.auth.dto.LoginRequest;
import edu.iztech.utms.g02.utms_app.api.auth.dto.LoginResponse;
import edu.iztech.utms.g02.utms_app.api.auth.dto.RegisterRequest;
import edu.iztech.utms.g02.utms_app.bl.auth.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Yeni öğrenci kaydı oluşturur.
     * POST /api/auth/register
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(Map.of("message", "Kayıt başarılı."));
    }

    /**
     * Kullanıcı girişi yapar ve JWT token döner.
     * Tüm roller için geçerlidir (Student, OIDB, YDYO, YGK, DEAN_OFFICE, FACULTY_BOARD).
     * POST /api/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}
