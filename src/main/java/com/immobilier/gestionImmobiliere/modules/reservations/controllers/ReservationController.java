package com.immobilier.gestionImmobiliere.modules.reservations.controllers;

import com.immobilier.gestionImmobiliere.modules.reservations.apis.ReservationAPI;
import com.immobilier.gestionImmobiliere.modules.reservations.dto.requests.CreateReservationDTO;
import com.immobilier.gestionImmobiliere.modules.reservations.dto.responses.ReservationResponseDTO;
import com.immobilier.gestionImmobiliere.modules.reservations.services.ReservationService;
import com.immobilier.gestionImmobiliere.modules.user.jwtService.UserDetailsImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RestController;

import static com.immobilier.gestionImmobiliere.utils.BuildSuccessResponse.buildSuccessResponse;

@RestController
@PreAuthorize("hasAnyRole('CLIENT','AGENT','ADMIN')")
public class ReservationController implements ReservationAPI {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @Override
    public ResponseEntity<?> getAll(Integer idMaison, Pageable pageable, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        boolean isAdminOrAgent = currentUser.hasAnyRole("ADMIN", "AGENT");
        return reservationService.getAllForCurrentUser(idMaison, currentUser.getIdUser(), isAdminOrAgent, pageable);
    }

    @Override
    public ResponseEntity<?> getById(Integer id) {
        ReservationResponseDTO dto = reservationService.getReservationById(id);
        return buildSuccessResponse(HttpStatus.OK, "Réservation trouvée", "RESERVATION_FOUND", dto);
    }

    @Override
    public ResponseEntity<?> create(CreateReservationDTO dto, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return reservationService.create(dto, currentUser.getIdUser());
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','AGENT') or @reservationSecurity.isProprietaireMaison(#id, authentication.principal.id)")
    public ResponseEntity<?> confirmer(Integer id,@AuthenticationPrincipal UserDetailsImpl currentUser) {
        return reservationService.confirmer(id);
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','AGENT') " +
            "or @reservationSecurity.isProprietaireMaison(#id, authentication.principal.id) " +
            "or @reservationSecurity.isOwner(#id, authentication.principal.id)")
    public ResponseEntity<?> annuler(Integer id,@AuthenticationPrincipal UserDetailsImpl currentUser) {
        return reservationService.annuler(id);
    }

    @Override
    @PreAuthorize("hasRole('AGENT')")
    public ResponseEntity<?> convertir(Integer id, Double montantLoyer, String typeContrat, UserDetailsImpl currentUser) {
        return reservationService.convertirEnLocation(id, montantLoyer, typeContrat, currentUser.getIdUser());
    }
}