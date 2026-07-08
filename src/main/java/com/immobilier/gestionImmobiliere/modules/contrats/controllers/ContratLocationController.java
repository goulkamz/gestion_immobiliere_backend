package com.immobilier.gestionImmobiliere.modules.contrats.controllers;

import com.immobilier.gestionImmobiliere.modules.contrats.apis.ContratLocationAPI;
import com.immobilier.gestionImmobiliere.modules.contrats.dto.requests.CreateContratLocationDTO;
import com.immobilier.gestionImmobiliere.modules.contrats.dto.requests.TerminerLocationDTO;
import com.immobilier.gestionImmobiliere.modules.contrats.dto.responses.ContratLocationResponseDTO;
import com.immobilier.gestionImmobiliere.modules.contrats.services.ContratLocationService;
import com.immobilier.gestionImmobiliere.modules.user.jwtService.UserDetailsImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RestController;

import static com.immobilier.gestionImmobiliere.utils.BuildSuccessResponse.buildSuccessResponse;

@RestController
public class ContratLocationController implements ContratLocationAPI {

    private final ContratLocationService locationService;

    public ContratLocationController(ContratLocationService locationService) {
        this.locationService = locationService;
    }

    @Override
    @PreAuthorize("hasAnyRole('AGENT','ADMIN','CLIENT','BAILLEUR')")
    public ResponseEntity<?> getAll(Integer idMaison, Integer idLocataire, Pageable pageable,
                                    @AuthenticationPrincipal UserDetailsImpl currentUser) {
        boolean isAdminOrAgent = currentUser.hasAnyRole("ADMIN", "AGENT"); // méthode utilitaire à ajouter si absente
        boolean isBailleur = currentUser.hasRole("BAILLEUR");
        return locationService.getAllForCurrentUser(idMaison, idLocataire, currentUser.getIdUser(), isAdminOrAgent, isBailleur, pageable);
    }



    @Override
    @PreAuthorize("hasAnyRole('AGENT','ADMIN','CLIENT','BAILLEUR')")
    public ResponseEntity<?> getById(Integer id) {
        // Appel via le proxy (méthode publique du bean) -> @PostAuthorize s'exécute ici
        ContratLocationResponseDTO dto = locationService.getContratById(id);
        return buildSuccessResponse(HttpStatus.OK, "Contrat de location trouvé", "LOCATION_FOUND", dto);
    }

    @Override
    @PreAuthorize("hasRole('AGENT')")
    public ResponseEntity<?> create(CreateContratLocationDTO dto, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return locationService.create(dto, currentUser.getIdUser());
    }

    @Override
    @PreAuthorize("hasRole('AGENT')")
    public ResponseEntity<?> terminer(Integer id, TerminerLocationDTO dto) {
        return locationService.terminer(id, dto);
    }

    @Override
    @PreAuthorize("hasRole('AGENT')")
    public ResponseEntity<?> resilier(Integer id) {
        return locationService.resilier(id);
    }
}