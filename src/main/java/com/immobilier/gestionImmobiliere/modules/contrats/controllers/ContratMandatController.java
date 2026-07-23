package com.immobilier.gestionImmobiliere.modules.contrats.controllers;

import com.immobilier.gestionImmobiliere.donnees.contrats.model.StatutMandat;
import com.immobilier.gestionImmobiliere.modules.contrats.apis.ContratMandatAPI;
import com.immobilier.gestionImmobiliere.modules.contrats.dto.requests.CreateContratMandatDTO;
import com.immobilier.gestionImmobiliere.modules.contrats.dto.requests.ResilierMandatDTO;
import com.immobilier.gestionImmobiliere.modules.contrats.services.ContratMandatService;
import com.immobilier.gestionImmobiliere.modules.user.jwtService.UserDetailsImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasAnyRole('AGENT','ADMIN','BAILLEUR')")
public class ContratMandatController implements ContratMandatAPI {

    private final ContratMandatService mandatService;

    public ContratMandatController(ContratMandatService mandatService) {
        this.mandatService = mandatService;
    }

    @Override
    public ResponseEntity<?> getAll(Integer idCour, StatutMandat statut, Pageable pageable,
                                    @AuthenticationPrincipal UserDetailsImpl currentUser) {
        boolean isAdminOrAgent = currentUser.hasAnyRole("ADMIN", "AGENT");
        return mandatService.getAllForCurrentUser(idCour, statut, currentUser.getIdUser(), isAdminOrAgent, pageable);
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','AGENT') or @mandatSecurity.isProprietaireCour(#id, authentication.principal.idUser)")
    public ResponseEntity<?> getById(Integer id, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return mandatService.getByIdForCurrentUser(id, currentUser.getIdUser(),
                currentUser.hasAnyRole("ADMIN", "AGENT"));
    }

    @Override
    @PreAuthorize("hasRole('AGENT')")
    public ResponseEntity<?> create(CreateContratMandatDTO dto, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return mandatService.create(dto, currentUser.getIdUser());
    }

    @Override
    @PreAuthorize("hasRole('AGENT')")
    public ResponseEntity<?> activer(Integer id, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return mandatService.activer(id, currentUser.getIdUser());
    }

    @Override
    @PreAuthorize("hasRole('AGENT')")
    public ResponseEntity<?> resilier(Integer id, ResilierMandatDTO dto, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return mandatService.resilier(id, dto, currentUser.getIdUser());
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> delete(Integer id) {
        return mandatService.delete(id);
    }
}