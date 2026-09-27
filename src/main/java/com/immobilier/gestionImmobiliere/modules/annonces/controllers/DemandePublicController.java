package com.immobilier.gestionImmobiliere.modules.annonces.controllers;

import com.immobilier.gestionImmobiliere.modules.annonces.apis.DemandePublicAPI;
import com.immobilier.gestionImmobiliere.modules.annonces.dto.requests.CreateDemandeDTO;
import com.immobilier.gestionImmobiliere.modules.annonces.services.DemandeService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DemandePublicController implements DemandePublicAPI {

    private final DemandeService demandeService;

    public DemandePublicController(DemandeService demandeService) {
        this.demandeService = demandeService;
    }

    @Override // public — F18, "dépôt par les clients", pas nécessairement connectés
    public ResponseEntity<?> create(CreateDemandeDTO dto, HttpServletRequest request) {
        return demandeService.create(dto, request);
    }

}
