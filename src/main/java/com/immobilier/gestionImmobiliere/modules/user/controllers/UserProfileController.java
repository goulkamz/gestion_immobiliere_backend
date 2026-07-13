package com.immobilier.gestionImmobiliere.modules.user.controllers;

import com.immobilier.gestionImmobiliere.modules.user.apis.UserProfileAPI;
import com.immobilier.gestionImmobiliere.modules.user.dto.requests.UpdateProfileDTO;
import com.immobilier.gestionImmobiliere.modules.user.jwtService.UserDetailsImpl;
import com.immobilier.gestionImmobiliere.modules.user.services.UserProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("isAuthenticated()")
public class UserProfileController implements UserProfileAPI {

    private final UserProfileService userProfileService;

    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @Override
    public ResponseEntity<?> getMe(@AuthenticationPrincipal UserDetailsImpl currentUser) {
        return userProfileService.getMe(currentUser.getIdUser());
    }

    @Override
    public ResponseEntity<?> updateMe(UpdateProfileDTO dto, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return userProfileService.updateMe(currentUser.getIdUser(), dto);
    }
}