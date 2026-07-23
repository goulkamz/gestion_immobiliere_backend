package com.immobilier.gestionImmobiliere.modules.reservations.apis;

import com.immobilier.gestionImmobiliere.modules.reservations.dto.requests.CreateReservationDTO;
import com.immobilier.gestionImmobiliere.modules.user.jwtService.UserDetailsImpl;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/reservations")
public interface ReservationAPI {

    @GetMapping
    ResponseEntity<?> getAll(@RequestParam(required = false) Integer idMaison, Pageable pageable,
                             @AuthenticationPrincipal UserDetailsImpl currentUser);

    @GetMapping("/{id}")
    ResponseEntity<?> getById(@PathVariable Integer id,@AuthenticationPrincipal UserDetailsImpl currentUser);

    @PostMapping
    ResponseEntity<?> create(@Valid @RequestBody CreateReservationDTO dto, @AuthenticationPrincipal UserDetailsImpl currentUser);

    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    @PatchMapping("/{id}/confirmer")
    ResponseEntity<?> confirmer(@PathVariable Integer id, @AuthenticationPrincipal UserDetailsImpl currentUser);

    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    @PatchMapping("/{id}/annuler")
    ResponseEntity<?> annuler(@PathVariable Integer id, @AuthenticationPrincipal UserDetailsImpl currentUser);

    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    @PatchMapping("/{id}/convertir")
    ResponseEntity<?> convertir(@PathVariable Integer id, @RequestParam Double montantLoyer,
                                @RequestParam(required = false) String typeContrat,
                                @AuthenticationPrincipal UserDetailsImpl currentUser);
}