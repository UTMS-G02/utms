package edu.iztech.utms.g02.utms_app.api.auth.dto;

import edu.iztech.utms.g02.utms_app.dal.user.entity.UserRole;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MeResponse {
    private Integer userId;
    private String email;
    private UserRole role;
    private String fullName;
}