package com.immobilier.gestionImmobiliere.modules.annonces.controllers;

import com.immobilier.gestionImmobiliere.donnees.annonces.model.StatutDemande;
import com.immobilier.gestionImmobiliere.modules.annonces.apis.DemandeAPI;
import com.immobilier.gestionImmobiliere.modules.annonces.dto.requests.CreateDemandeDTO;
import com.immobilier.gestionImmobiliere.modules.annonces.dto.requests.UpdateStatutDemandeDTO;
import com.immobilier.gestionImmobiliere.modules.annonces.services.DemandeService;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DemandeController implements DemandeAPI {

    private final DemandeService demandeService;

    public DemandeController(DemandeService demandeService) {
        this.demandeService = demandeService;
    }

    @Override
    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    public ResponseEntity<?> getAll(StatutDemande statut, Pageable pageable) {
        return demandeService.getAll(statut, pageable);
    }

    @Override
    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    public ResponseEntity<?> getById(Integer id) {
        return demandeService.getById(id);
    }

    @Override // public — F18, "dépôt par les clients", pas nécessairement connectés
    public ResponseEntity<?> create(CreateDemandeDTO dto) {
        return demandeService.create(dto);
    }

    @Override
    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    public ResponseEntity<?> updateStatut(Integer id, UpdateStatutDemandeDTO dto) {
        return demandeService.updateStatut(id, dto);
    }
}