package com.immobilier.gestionImmobiliere.modules.depenses.controllers;

import com.immobilier.gestionImmobiliere.donnees.depenses.model.StatutDepense;
import com.immobilier.gestionImmobiliere.modules.depenses.apis.DepenseAPI;
import com.immobilier.gestionImmobiliere.modules.depenses.dto.AbandonnerDepenseDTO;
import com.immobilier.gestionImmobiliere.modules.depenses.dto.AnnulerDepenseDTO;
import com.immobilier.gestionImmobiliere.modules.depenses.dto.AnnulerReglementDTO;
import com.immobilier.gestionImmobiliere.modules.depenses.dto.CreateDepenseDTO;
import com.immobilier.gestionImmobiliere.modules.depenses.dto.UpdateDepenseDTO;
import com.immobilier.gestionImmobiliere.modules.depenses.services.DepenseService;
import com.immobilier.gestionImmobiliere.modules.user.jwtService.UserDetailsImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RestController;

/** Lecture : admin, agent et bailleur (ses cours uniquement, filtré dans le service). Écriture : admin et agent. */
@RestController
@PreAuthorize("hasAnyRole('ADMIN','AGENT','BAILLEUR')")
public class DepenseController implements DepenseAPI {

    private final DepenseService depenseService;

    public DepenseController(DepenseService depenseService) {
        this.depenseService = depenseService;
    }

    @Override
    public ResponseEntity<?> getAll(Integer idCour, Integer idMaison, Integer idMandat, StatutDepense statut, String recherche,
                                    Pageable pageable, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return depenseService.getAllForCurrentUser(idCour, idMaison, idMandat, statut, recherche,
                currentUser.getIdUser(), currentUser.hasAnyRole("ADMIN", "AGENT"), pageable);
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<?> getMandatsEchus() {
        return depenseService.getMandatsEchus();
    }

    @Override
    public ResponseEntity<?> getById(Integer id, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return depenseService.getByIdForCurrentUser(id, currentUser.getIdUser(), currentUser.hasAnyRole("ADMIN", "AGENT"));
    }

    @Override
    public ResponseEntity<?> getImputationsReversement(Integer idEcheance, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return depenseService.getImputationsReversement(idEcheance, currentUser.getIdUser(), currentUser.hasAnyRole("ADMIN", "AGENT"));
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<?> create(CreateDepenseDTO dto, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return depenseService.create(dto, currentUser.getIdUser());
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<?> update(Integer id, UpdateDepenseDTO dto, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return depenseService.update(id, dto, currentUser.getIdUser());
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<?> annuler(Integer id, AnnulerDepenseDTO dto, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return depenseService.annuler(id, dto, currentUser.getIdUser());
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<?> reechelonner(Integer id, com.immobilier.gestionImmobiliere.modules.depenses.dto.ReechelonnerDepenseDTO dto,
                                          @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return depenseService.reechelonner(id, dto, currentUser.getIdUser());
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<?> enregistrerReglement(Integer id, com.immobilier.gestionImmobiliere.modules.depenses.dto.ReglementDepenseDTO dto,
                                                  @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return depenseService.enregistrerReglement(id, dto, currentUser.getIdUser());
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<?> annulerReglement(Integer id, Integer idReglement, AnnulerReglementDTO dto,
                                              @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return depenseService.annulerReglement(id, idReglement, dto, currentUser.getIdUser());
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> retablirCreance(Integer id, com.immobilier.gestionImmobiliere.modules.depenses.dto.MotifDTO dto,
                                             @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return depenseService.retablirCreance(id, dto, currentUser.getIdUser());
    }

    @Override
    @PreAuthorize("hasRole('BAILLEUR')")
    public ResponseEntity<?> contester(Integer id, com.immobilier.gestionImmobiliere.modules.depenses.dto.MotifDTO dto,
                                       @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return depenseService.contester(id, dto, currentUser.getIdUser());
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<?> repondreContestation(Integer id, com.immobilier.gestionImmobiliere.modules.depenses.dto.MotifDTO dto,
                                                  @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return depenseService.repondreContestation(id, dto, currentUser.getIdUser());
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> abandonner(Integer id, AbandonnerDepenseDTO dto, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return depenseService.abandonner(id, dto, currentUser.getIdUser());
    }
}
