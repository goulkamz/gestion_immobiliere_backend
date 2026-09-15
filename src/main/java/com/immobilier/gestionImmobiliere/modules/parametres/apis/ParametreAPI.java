package com.immobilier.gestionImmobiliere.modules.parametres.apis;

import com.immobilier.gestionImmobiliere.modules.parametres.dto.requests.UpdateParametreDTO;
import com.immobilier.gestionImmobiliere.modules.user.jwtService.UserDetailsImpl;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/parametres")
public interface ParametreAPI {

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    ResponseEntity<?> getAll();

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{cle}")
    ResponseEntity<?> mettreAJour(@PathVariable String cle, @Valid @RequestBody UpdateParametreDTO dto,
                                  @AuthenticationPrincipal UserDetailsImpl currentUser);
}