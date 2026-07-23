package com.immobilier.gestionImmobiliere.modules.paiements.controllers;

import com.immobilier.gestionImmobiliere.donnees.paiements.model.StatutEcheance;
import com.immobilier.gestionImmobiliere.donnees.paiements.model.TypeEcheance;
import com.immobilier.gestionImmobiliere.modules.paiements.apis.EcheanceAPI;
import com.immobilier.gestionImmobiliere.modules.paiements.services.EcheanceService;
import com.immobilier.gestionImmobiliere.modules.user.jwtService.UserDetailsImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasAnyRole('ADMIN','AGENT','BAILLEUR','CLIENT')")
public class EcheanceController implements EcheanceAPI {

    private final EcheanceService echeanceService;

    public EcheanceController(EcheanceService echeanceService) {
        this.echeanceService = echeanceService;
    }

    @Override
    public ResponseEntity<?> getAll(TypeEcheance type, Integer entiteId, StatutEcheance statut, Pageable pageable,
                                    @AuthenticationPrincipal UserDetailsImpl currentUser) {
        boolean isAdminOrAgent = currentUser.hasAnyRole("ADMIN", "AGENT");
        boolean isBailleur = currentUser.hasRole("BAILLEUR");
        return echeanceService.getAllForCurrentUser(type, entiteId, statut, currentUser.getIdUser(), isAdminOrAgent, isBailleur, pageable);
    }

    @Override
    public ResponseEntity<?> getById(Integer id, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        boolean isAdminOrAgent = currentUser.hasAnyRole("ADMIN", "AGENT");
        boolean isBailleur = currentUser.hasRole("BAILLEUR");
        return echeanceService.getByIdForCurrentUser(id, currentUser.getIdUser(), isAdminOrAgent, isBailleur);
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<?> getEnRetard() {
        return echeanceService.getEnRetard();
    }
}