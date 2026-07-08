package com.immobilier.gestionImmobiliere.modules.annonces.controllers;

import com.immobilier.gestionImmobiliere.donnees.annonces.model.StatutOffre;
import com.immobilier.gestionImmobiliere.modules.annonces.apis.OffreAPI;
import com.immobilier.gestionImmobiliere.modules.annonces.dto.requests.CreateOffreDTO;
import com.immobilier.gestionImmobiliere.modules.annonces.dto.requests.UpdateStatutOffreDTO;
import com.immobilier.gestionImmobiliere.modules.annonces.services.OffreService;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OffreController implements OffreAPI {

    private final OffreService offreService;

    public OffreController(OffreService offreService) {
        this.offreService = offreService;
    }

    @Override
    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    public ResponseEntity<?> getAll(StatutOffre statut,Pageable pageable) {
        return offreService.getAll( statut,pageable);
    }


    @Override
    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    public ResponseEntity<?> getById(Integer id) {
        return offreService.getById(id);
    }

    @Override // public — F19, dépôt spontané, pas de compte requis
    public ResponseEntity<?> create(CreateOffreDTO dto) {
        return offreService.create(dto);
    }

    /**
     * @param id
     * @param dto
     * @return
     */
    @Override
    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    public ResponseEntity<?> updateStatut(Integer id, UpdateStatutOffreDTO dto) {
        return offreService.updateStatut(id, dto);
    }
}