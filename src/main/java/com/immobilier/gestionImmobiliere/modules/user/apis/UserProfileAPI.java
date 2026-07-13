package com.immobilier.gestionImmobiliere.modules.user.apis;

import com.immobilier.gestionImmobiliere.modules.user.dto.requests.UpdateProfileDTO;
import com.immobilier.gestionImmobiliere.modules.user.jwtService.UserDetailsImpl;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/users")
public interface UserProfileAPI {

    @GetMapping("/me")
    ResponseEntity<?> getMe(@AuthenticationPrincipal UserDetailsImpl currentUser);

    @PutMapping("/me")
    ResponseEntity<?> updateMe(@Valid @RequestBody UpdateProfileDTO dto, @AuthenticationPrincipal UserDetailsImpl currentUser);
}