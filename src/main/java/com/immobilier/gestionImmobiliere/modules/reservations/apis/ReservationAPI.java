package com.immobilier.gestionImmobiliere.modules.reservations.apis;

import com.immobilier.gestionImmobiliere.donnees.reservations.model.StatutReservation;
import com.immobilier.gestionImmobiliere.modules.reservations.dto.requests.CreateReservationDTO;
import com.immobilier.gestionImmobiliere.modules.user.jwtService.UserDetailsImpl;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@RequestMapping("/api/reservations")
public interface ReservationAPI {

    @GetMapping
    ResponseEntity<?> getAll(@RequestParam(required = false) Integer idMaison,
                             @RequestParam(required = false) StatutReservation statut,
                             @RequestParam(required = false) String recherche,
                             Pageable pageable,
                             @AuthenticationPrincipal UserDetailsImpl currentUser);

    @GetMapping("/{id}")
    ResponseEntity<?> getById(@PathVariable Integer id,@AuthenticationPrincipal UserDetailsImpl currentUser);

    @PostMapping
    ResponseEntity<?> create(@Valid @RequestBody CreateReservationDTO dto, @AuthenticationPrincipal UserDetailsImpl currentUser);

    @PatchMapping("/{id}/confirmer")
    ResponseEntity<?> confirmer(@PathVariable Integer id, @AuthenticationPrincipal UserDetailsImpl currentUser);

    @PatchMapping("/{id}/annuler")
    ResponseEntity<?> annuler(@PathVariable Integer id, @AuthenticationPrincipal UserDetailsImpl currentUser);

    // dateSortie optionnelle : bail à durée indéterminée si non fournie (cas courant
    // en pratique, le locataire ne sachant pas toujours à l'avance quand il partira)
    @PatchMapping("/{id}/convertir")
    ResponseEntity<?> convertir(@PathVariable Integer id, @RequestParam BigDecimal montantLoyer,
                                @RequestParam(required = false) String typeContrat,
                                @RequestParam(required = false) LocalDateTime dateSortie,
                                @AuthenticationPrincipal UserDetailsImpl currentUser);
}