package com.immobilier.gestionImmobiliere.modules.annonces.controllers;

import com.immobilier.gestionImmobiliere.donnees.annonces.model.StatutAnnonce;
import com.immobilier.gestionImmobiliere.modules.annonces.apis.AnnonceAPI;
import com.immobilier.gestionImmobiliere.modules.annonces.dto.requests.*;
import com.immobilier.gestionImmobiliere.modules.annonces.services.AnnonceService;
import com.immobilier.gestionImmobiliere.modules.user.jwtService.UserDetailsImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AnnonceController implements AnnonceAPI {

    private final AnnonceService annonceService;

    public AnnonceController(AnnonceService annonceService) {
        this.annonceService = annonceService;
    }

    @Override
    public ResponseEntity<?> getAll(StatutAnnonce statut, Pageable pageable) {
        return annonceService.getAll(statut, pageable);
    }

    @Override
    public ResponseEntity<?> getById(Integer id) {
        return annonceService.getById(id);
    }

    @Override
    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    public ResponseEntity<?> create(CreateAnnonceDTO dto) {
        return annonceService.create(dto);
    }

    @Override
    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    public ResponseEntity<?> update(Integer id, UpdateAnnonceDTO dto) {
        return annonceService.update(id, dto);
    }

    @Override
    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    public ResponseEntity<?> updateStatut(Integer id, UpdateStatutAnnonceDTO dto,@AuthenticationPrincipal UserDetailsImpl currentUser) {
        return annonceService.updateStatut(id, dto,currentUser.getIdUser());
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> delete(Integer id) {
        return annonceService.delete(id);
    }
}