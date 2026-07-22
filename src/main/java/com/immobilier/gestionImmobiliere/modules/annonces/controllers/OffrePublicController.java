package com.immobilier.gestionImmobiliere.modules.annonces.controllers;

import com.immobilier.gestionImmobiliere.modules.annonces.apis.OffrePublicAPI;
import com.immobilier.gestionImmobiliere.modules.annonces.dto.requests.CreateOffreDTO;
import com.immobilier.gestionImmobiliere.modules.annonces.services.OffreService;
import org.springframework.http.ResponseEntity;

public class OffrePublicController implements OffrePublicAPI {

    private final OffreService offreService;

    public OffrePublicController(OffreService offreService) {
        this.offreService = offreService;
    }

    @Override // public — F19, dépôt spontané, pas de compte requis
    public ResponseEntity<?> create(CreateOffreDTO dto) {
        return offreService.create(dto);
    }

}
