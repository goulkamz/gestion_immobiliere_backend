package com.immobilier.gestionImmobiliere.modules.paiements.controllers;

import com.immobilier.gestionImmobiliere.donnees.paiements.model.StatutEcheance;
import com.immobilier.gestionImmobiliere.donnees.paiements.model.TypeEcheance;
import com.immobilier.gestionImmobiliere.modules.paiements.apis.EcheanceAPI;
import com.immobilier.gestionImmobiliere.modules.paiements.dto.requests.ConfirmerVirementDTO;
import com.immobilier.gestionImmobiliere.modules.paiements.services.EcheanceService;
import com.immobilier.gestionImmobiliere.modules.user.jwtService.UserDetailsImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

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

    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    @Override
    public ResponseEntity<?> getEcheanceLocationEnRetard() {
        return echeanceService.getEcheanceLocationEnRetard();
    }

    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    @Override
    public ResponseEntity<?> getEcheanceMandatEnRetard() {
        return echeanceService.getEcheanceMandatEnRetard();
    }

    /**
     * @param idMandat
     * @param periode
     * @param currentUser
     * @return
     */
    @Override
    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    public ResponseEntity<?> calculerReversementMandat(Integer idMandat, LocalDate periode, UserDetailsImpl currentUser) {
        return echeanceService.calculerReversementMandat(idMandat,periode,currentUser.getIdUser());
    }

    /**
     * @param idEcheance
     * @param dto
     * @param currentUser
     * @return
     */
    @Override
    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    public ResponseEntity<?> confirmerVirementMandat(Integer idEcheance, ConfirmerVirementDTO dto, UserDetailsImpl currentUser) {
        return echeanceService.confirmerVirementMandat(idEcheance,dto,currentUser.getIdUser());
    }

    /**
     * @param idMandat
     * @return
     */
    @Override
    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    public ResponseEntity<?> getReversementsEnAttente(Integer idMandat) {
        return echeanceService.getReversementsEnAttente(idMandat);
    }
}