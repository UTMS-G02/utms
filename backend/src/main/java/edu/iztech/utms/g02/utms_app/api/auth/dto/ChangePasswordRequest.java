package edu.iztech.utms.g02.utms_app.api.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ChangePasswordRequest {

    @NotBlank(message = "Mevcut şifre boş bırakılamaz.")
    private String currentPassword;

    @NotBlank(message = "Yeni şifre boş bırakılamaz.")
    @Size(min = 8, max = 72, message = "Yeni şifre en az 8 karakter olmalıdır.")
    private String newPassword;
}
