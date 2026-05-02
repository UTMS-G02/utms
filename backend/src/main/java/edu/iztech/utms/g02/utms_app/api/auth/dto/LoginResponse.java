package edu.iztech.utms.g02.utms_app.api.auth.dto;

import edu.iztech.utms.g02.utms_app.dal.user.entity.UserRole;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResponse {

    private Integer userId;
    private String firstName;
    private String lastName;
    private UserRole role;
    private String token;
}