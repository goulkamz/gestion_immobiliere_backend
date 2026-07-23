package com.immobilier.gestionImmobiliere.modules.reservations.controllers;

import com.immobilier.gestionImmobiliere.modules.reservations.apis.ReservationAPI;
import com.immobilier.gestionImmobiliere.modules.reservations.dto.requests.CreateReservationDTO;
import com.immobilier.gestionImmobiliere.modules.reservations.services.ReservationService;
import com.immobilier.gestionImmobiliere.modules.user.jwtService.UserDetailsImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasAnyRole('CLIENT','AGENT','ADMIN','BAILLEUR')")
public class ReservationController implements ReservationAPI {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    // Lecture : ouverte à tous les rôles, filtrage par ownership fait dans le service
    @Override
    public ResponseEntity<?> getAll(Integer idMaison, Pageable pageable, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        boolean isAdminOrAgent = currentUser.hasAnyRole("ADMIN", "AGENT");
        boolean isBailleur = currentUser.hasRole("BAILLEUR");
        return reservationService.getAllForCurrentUser(idMaison, currentUser.getIdUser(), isAdminOrAgent, isBailleur, pageable);
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','AGENT') " +
            "or @reservationSecurity.isProprietaireMaison(#id, authentication.principal.idUser) " +
            "or @reservationSecurity.isOwner(#id, authentication.principal.idUser)")
    public ResponseEntity<?> getById(Integer id, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return reservationService.getByIdForCurrentUser(id, currentUser.getIdUser(),
                currentUser.hasAnyRole("ADMIN", "AGENT"));
    }

    // Création : réservée au Client (le Bailleur ne réserve pas son propre bien)
    @Override
    @PreAuthorize("hasAnyRole('CLIENT','AGENT','ADMIN')")
    public ResponseEntity<?> create(CreateReservationDTO dto, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return reservationService.create(dto, currentUser.getIdUser());
    }

    // Validation : réservée à Agent/Admin uniquement — le Bailleur ne tranche plus
    @Override
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<?> confirmer(Integer id, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return reservationService.confirmer(id);
    }

    // Annulation : Agent/Admin, ou le Client lui-même (rétractation) — pas le Bailleur
    @Override
    @PreAuthorize("hasAnyRole('ADMIN','AGENT') or @reservationSecurity.isOwner(#id, authentication.principal.idUser)")
    public ResponseEntity<?> annuler(Integer id, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return reservationService.annuler(id);
    }

    // Conversion en contrat de location : action métier réservée à Agent/Admin
    @Override
    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    public ResponseEntity<?> convertir(Integer id, Double montantLoyer, String typeContrat,
                                       @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return reservationService.convertirEnLocation(id, montantLoyer, typeContrat, currentUser.getIdUser());
    }
}