package com.immobilier.gestionImmobiliere.modules.annonces.controllers;

import com.immobilier.gestionImmobiliere.donnees.annonces.model.StatutTemoignage;
import com.immobilier.gestionImmobiliere.modules.annonces.apis.TemoignageAdminAPI;
import com.immobilier.gestionImmobiliere.modules.annonces.dto.requests.CreateTemoignageDTO;
import com.immobilier.gestionImmobiliere.modules.annonces.dto.requests.UpdateStatutTemoignageDTO;
import com.immobilier.gestionImmobiliere.modules.annonces.services.TemoignageService;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TemoignageAdminController implements TemoignageAdminAPI {

    private final TemoignageService temoignageService;

    public TemoignageAdminController(TemoignageService temoignageService) {
        this.temoignageService = temoignageService;
    }

    @Override
    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    public ResponseEntity<?> getAll(StatutTemoignage statut, String recherche, Pageable pageable) {
        return temoignageService.getAll(statut, recherche, pageable);
    }

    @Override
    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    public ResponseEntity<?> create(CreateTemoignageDTO dto) {
        return temoignageService.create(dto);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateStatut(Integer id, UpdateStatutTemoignageDTO dto) {
        return temoignageService.updateStatut(id, dto);
    }
}
