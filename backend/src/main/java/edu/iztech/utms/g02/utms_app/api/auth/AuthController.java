package edu.iztech.utms.g02.utms_app.api.auth;

import edu.iztech.utms.g02.utms_app.api.auth.dto.ForgotPasswordRequest;
import edu.iztech.utms.g02.utms_app.api.auth.dto.LoginRequest;
import edu.iztech.utms.g02.utms_app.api.auth.dto.LoginResponse;
import edu.iztech.utms.g02.utms_app.api.auth.dto.MeResponse;
import edu.iztech.utms.g02.utms_app.api.auth.dto.RegisterRequest;
import edu.iztech.utms.g02.utms_app.api.auth.dto.RegisterResponse;
import edu.iztech.utms.g02.utms_app.api.auth.dto.ResetPasswordRequest;
import edu.iztech.utms.g02.utms_app.bl.auth.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Registers a new student account.
     * POST /api/auth/register
     */
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new RegisterResponse("Kayıt başarılı."));
    }

    /**
     * Authenticates a user and returns a JWT token.
     * Valid for all roles (Student, OIDB, YDYO, YGK, DEAN_OFFICE, FACULTY_BOARD).
     * POST /api/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Returns profile info for the token owner.
     * Used by the frontend to rehydrate session state on page refresh.
     * GET /api/auth/me
     */
    @GetMapping("/me")
    public ResponseEntity<MeResponse> me(@AuthenticationPrincipal UserDetails userDetails) {
        MeResponse response = authService.getMe(userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    /**
     * Initiates a password reset request.
     * In production an email would be sent; the reset link is logged for now.
     * POST /api/auth/forgot-password
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok().build();
    }

    /**
     * Sets a new password using a valid reset token.
     * POST /api/auth/reset-password
     */
    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok().build();
    }
}
