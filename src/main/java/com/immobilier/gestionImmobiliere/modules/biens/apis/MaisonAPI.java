package com.immobilier.gestionImmobiliere.modules.biens.apis;

import com.immobilier.gestionImmobiliere.donnees.biens.model.StatutMaison;
import com.immobilier.gestionImmobiliere.modules.biens.dto.requests.CreateMaisonDTO;
import com.immobilier.gestionImmobiliere.modules.biens.dto.requests.UpdateMaisonDTO;
import com.immobilier.gestionImmobiliere.modules.biens.dto.requests.UpdateStatutMaisonDTO;
import com.immobilier.gestionImmobiliere.modules.user.jwtService.UserDetailsImpl;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

// Écriture réservée aux agents (le bailleur ne saisit pas directement)
@RequestMapping("/api/maisons")
@PreAuthorize("hasAnyRole('AGENT','ADMIN')")
public interface MaisonAPI {
    @PostMapping
    ResponseEntity<?> create(@Valid @RequestBody CreateMaisonDTO dto, @AuthenticationPrincipal UserDetailsImpl currentUser);

    @PutMapping("/{id}")
    ResponseEntity<?> update(@PathVariable Integer id, @Valid @RequestBody UpdateMaisonDTO dto, @AuthenticationPrincipal UserDetailsImpl currentUser);

    @PatchMapping("/{id}/statut")
    ResponseEntity<?> updateStatut(@PathVariable Integer id, @Valid @RequestBody UpdateStatutMaisonDTO dto, @AuthenticationPrincipal UserDetailsImpl currentUser);

    @DeleteMapping("/{id}")
    ResponseEntity<?> delete(@PathVariable Integer id);
}