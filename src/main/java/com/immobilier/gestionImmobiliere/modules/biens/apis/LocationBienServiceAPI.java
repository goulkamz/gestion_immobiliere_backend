package com.immobilier.gestionImmobiliere.modules.biens.apis;

import com.immobilier.gestionImmobiliere.modules.biens.dto.requests.*;
import com.immobilier.gestionImmobiliere.modules.user.jwtService.UserDetailsImpl;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/locations-biens-services")
public interface LocationBienServiceAPI {

    // AGENT/ADMIN : vue globale. CLIENT : le service filtre sur ses propres locations
    @PreAuthorize("hasAnyRole('AGENT','ADMIN','CLIENT')")
    @GetMapping
    ResponseEntity<?> getAll(Pageable pageable, @AuthenticationPrincipal UserDetailsImpl currentUser);

    @PreAuthorize("hasAnyRole('AGENT','ADMIN','CLIENT')")
    @GetMapping("/{id}")
    ResponseEntity<?> getById(@PathVariable Integer id, @AuthenticationPrincipal UserDetailsImpl currentUser);

    // Étape 1 : le client dépose sa demande, sans paiement
    @PreAuthorize("hasRole('CLIENT')")
    @PostMapping
    ResponseEntity<?> create(@Valid @RequestBody CreateLocationBienServiceDTO dto, @AuthenticationPrincipal UserDetailsImpl currentUser);

    // Étape 2 : l'agent réceptionne le paiement et confirme
    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    @PatchMapping("/{id}/confirmer")
    ResponseEntity<?> confirmer(@PathVariable Integer id, @Valid @RequestBody ConfirmerLocationDTO dto, @AuthenticationPrincipal UserDetailsImpl currentUser);

    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    @PatchMapping("/{id}/duree")
    ResponseEntity<?> modifierDuree(@PathVariable Integer id, @Valid @RequestBody ModifierDureeLocationDTO dto, @AuthenticationPrincipal UserDetailsImpl currentUser);

    @PreAuthorize("hasAnyRole('AGENT','ADMIN','CLIENT')")
    @PatchMapping("/{id}/annuler")
    ResponseEntity<?> annuler(@PathVariable Integer id, @AuthenticationPrincipal UserDetailsImpl currentUser);

    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    @PostMapping("/{id}/rembourser")
    ResponseEntity<?> rembourser(@PathVariable Integer id, @Valid @RequestBody CreerRemboursementDTO dto, @AuthenticationPrincipal UserDetailsImpl currentUser);

    @PreAuthorize("hasAnyRole('AGENT','ADMIN','CLIENT')")
    @GetMapping("/{id}/remboursements")
    ResponseEntity<?> getRemboursements(@PathVariable Integer id);

    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    @PatchMapping("/{id}/statut")
    ResponseEntity<?> updateStatut(@PathVariable Integer id, @Valid @RequestBody UpdateStatutLocationBienServiceDTO dto);
}