package com.immobilier.gestionImmobiliere.modules.annonces.controllers;

import com.immobilier.gestionImmobiliere.donnees.annonces.model.StatutOffre;
import com.immobilier.gestionImmobiliere.modules.annonces.apis.OffreAdminAPI;
import com.immobilier.gestionImmobiliere.modules.annonces.dto.requests.CreateOffreDTO;
import com.immobilier.gestionImmobiliere.modules.annonces.dto.requests.UpdateStatutOffreDTO;
import com.immobilier.gestionImmobiliere.modules.annonces.services.OffreService;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OffreAdminController implements OffreAdminAPI {

    private final OffreService offreService;

    public OffreAdminController(OffreService offreService) {
        this.offreService = offreService;
    }

    @Override
    public ResponseEntity<?> getAll(StatutOffre statut,Pageable pageable) {
        return offreService.getAll( statut,pageable);
    }

    @Override
    public ResponseEntity<?> getById(Integer id) {
        return offreService.getById(id);
    }

    /**
     * @param id
     * @param dto
     * @return
     */
    @Override
    public ResponseEntity<?> updateStatut(Integer id, UpdateStatutOffreDTO dto) {
        return offreService.updateStatut(id, dto);
    }
}