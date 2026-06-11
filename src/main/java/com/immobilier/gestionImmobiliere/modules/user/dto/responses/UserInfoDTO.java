package com.immobilier.gestionImmobiliere.modules.user.dto.responses;

import lombok.Builder;
import lombok.Data;

import java.util.List;


    @Data
    @Builder
    public class UserInfoDTO {

        private String username;
        private String accessToken;
        private String refreshToken;  // ← Pour mobile
        private Long expiresIn;
        private List<String> roles;


}
