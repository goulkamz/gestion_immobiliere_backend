package com.immobilier.gestionImmobiliere.modules.annonces.apis;

import com.immobilier.gestionImmobiliere.modules.annonces.dto.requests.*;
import com.immobilier.gestionImmobiliere.modules.user.jwtService.UserDetailsImpl;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

// Écriture, réservée aux agents/admins authentifiés
@RequestMapping("/api/annonces")
@PreAuthorize("hasAnyRole('AGENT','ADMIN')")
public interface AnnonceAPI {

    @PostMapping
    ResponseEntity<?> create(@Valid @RequestBody CreateAnnonceDTO dto);

    @PutMapping("/{id}")
    ResponseEntity<?> update(@PathVariable Integer id, @Valid @RequestBody UpdateAnnonceDTO dto);

    @PatchMapping("/{id}/statut")
    ResponseEntity<?> updateStatut(@PathVariable Integer id, @Valid @RequestBody UpdateStatutAnnonceDTO dto);

    @DeleteMapping("/{id}")
    ResponseEntity<?> delete(@PathVariable Integer id);
}