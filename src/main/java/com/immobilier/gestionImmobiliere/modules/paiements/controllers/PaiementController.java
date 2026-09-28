package com.immobilier.gestionImmobiliere.modules.paiements.controllers;

import com.immobilier.gestionImmobiliere.modules.paiements.apis.PaiementAPI;
import com.immobilier.gestionImmobiliere.modules.paiements.dto.requests.CreatePaiementDTO;
import com.immobilier.gestionImmobiliere.modules.paiements.dto.responses.PaiementResponseDTO;
import com.immobilier.gestionImmobiliere.modules.paiements.services.PaiementService;
import com.immobilier.gestionImmobiliere.modules.user.jwtService.UserDetailsImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RestController;

import static com.immobilier.gestionImmobiliere.utils.BuildSuccessResponse.buildSuccessResponse;

@RestController
@PreAuthorize("hasAnyRole('ADMIN','AGENT','CLIENT','BAILLEUR')")
public class PaiementController implements PaiementAPI {

    private final PaiementService paiementService;

    public PaiementController(PaiementService paiementService) {
        this.paiementService = paiementService;
    }

    // Pas de passerelle de paiement en ligne : un paiement n'est enregistre qu'une fois
    // l'argent recu par l'agence, jamais sur simple declaration du client.
    @Override
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<?> create(CreatePaiementDTO dto, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return paiementService.create(dto, currentUser.getIdUser());
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')" +
            "or @paiementOwnershipResolver.isPaiementAccessible(#id, authentication.principal.idUser, false) " +
            "or @paiementOwnershipResolver.isPaiementAccessible(#id, authentication.principal.idUser, true)" +
            "or @paiementOwnershipResolver.isPaiementAccessibleBienService(#id, authentication.principal.idUser)")
    public ResponseEntity<?> getById(Integer id) {
        PaiementResponseDTO dto = paiementService.getPaiementById(id);
        return buildSuccessResponse(HttpStatus.OK, "Paiement trouvé", "PAIEMENT_FOUND", dto);
    }
}