package com.immobilier.gestionImmobiliere.modules.contrats.controllers;

import com.immobilier.gestionImmobiliere.donnees.contrats.model.StatutMandat;
import com.immobilier.gestionImmobiliere.modules.contrats.apis.ContratMandatAPI;
import com.immobilier.gestionImmobiliere.modules.contrats.dto.requests.CreateContratMandatDTO;
import com.immobilier.gestionImmobiliere.modules.contrats.dto.requests.ResilierMandatDTO;
import com.immobilier.gestionImmobiliere.modules.contrats.services.ContratMandatService;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasAnyRole('AGENT','ADMIN')")
public class ContratMandatController implements ContratMandatAPI {

    private final ContratMandatService mandatService;

    public ContratMandatController(ContratMandatService mandatService) {
        this.mandatService = mandatService;
    }

    @Override
    public ResponseEntity<?> getAll(Integer idCour, StatutMandat statut, Pageable pageable) {
        return mandatService.getAll(idCour, statut, pageable);
    }

    @Override
    public ResponseEntity<?> getById(Integer id) {
        return mandatService.getById(id);
    }

    @Override
    @PreAuthorize("hasRole('AGENT')")
    public ResponseEntity<?> create(CreateContratMandatDTO dto) {
        return mandatService.create(dto);
    }

    @Override
    @PreAuthorize("hasRole('AGENT')")
    public ResponseEntity<?> activer(Integer id) {
        return mandatService.activer(id);
    }

    @Override
    @PreAuthorize("hasRole('AGENT')")
    public ResponseEntity<?> resilier(Integer id, ResilierMandatDTO dto) {
        return mandatService.resilier(id, dto);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> delete(Integer id) {
        return mandatService.delete(id);
    }
}