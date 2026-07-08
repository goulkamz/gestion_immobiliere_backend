package com.immobilier.gestionImmobiliere.modules.biens.controllers;

import com.immobilier.gestionImmobiliere.donnees.biens.model.StatutMaison;
import com.immobilier.gestionImmobiliere.modules.biens.apis.MaisonAPI;
import com.immobilier.gestionImmobiliere.modules.biens.dto.requests.CreateMaisonDTO;
import com.immobilier.gestionImmobiliere.modules.biens.dto.requests.UpdateMaisonDTO;
import com.immobilier.gestionImmobiliere.modules.biens.dto.requests.UpdateStatutMaisonDTO;
import com.immobilier.gestionImmobiliere.modules.biens.services.MaisonService;
import com.immobilier.gestionImmobiliere.modules.user.jwtService.UserDetailsImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MaisonController implements MaisonAPI {

    private final MaisonService maisonService;

    public MaisonController(MaisonService maisonService) {
        this.maisonService = maisonService;
    }

    @Override
    public ResponseEntity<?> getAll(Integer idCour, StatutMaison statut, Pageable pageable) {
        return maisonService.getAll(idCour, statut, pageable);
    }

    @Override
    public ResponseEntity<?> getById(Integer id) {
        return maisonService.getById(id);
    }

    @Override
    public ResponseEntity<?> create(CreateMaisonDTO dto) {
        return null;
    }

    @Override
    public ResponseEntity<?> update(Integer id, UpdateMaisonDTO dto) {
        return null;
    }

    @Override
    public ResponseEntity<?> updateStatut(Integer id, UpdateStatutMaisonDTO dto) {
        return maisonService.updateStatut(id,dto);
    }

    @Override
    @PreAuthorize("hasRole('AGENT')")
    public ResponseEntity<?> create(CreateMaisonDTO dto, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return maisonService.create(dto, currentUser.getIdUser());
    }

    @Override
    @PreAuthorize("hasRole('AGENT')")
    public ResponseEntity<?> update(Integer id, UpdateMaisonDTO dto, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return maisonService.update(id, dto, currentUser.getIdUser());
    }

    @Override
    @PreAuthorize("hasRole('AGENT')")
    public ResponseEntity<?> updateStatut(Integer id, UpdateStatutMaisonDTO dto, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return maisonService.updateStatut(id, dto, currentUser.getIdUser());
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> delete(Integer id) {
        return maisonService.delete(id);
    }
}