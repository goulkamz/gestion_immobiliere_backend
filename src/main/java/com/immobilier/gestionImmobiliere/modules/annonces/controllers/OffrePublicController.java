package com.immobilier.gestionImmobiliere.modules.annonces.controllers;

import com.immobilier.gestionImmobiliere.modules.annonces.apis.OffrePublicAPI;
import com.immobilier.gestionImmobiliere.modules.annonces.dto.requests.CreateOffreDTO;
import com.immobilier.gestionImmobiliere.modules.annonces.services.OffreService;
import com.immobilier.gestionImmobiliere.modules.medias.dto.requests.UploadPhotosOffreDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OffrePublicController implements OffrePublicAPI {

    private final OffreService offreService;

    public OffrePublicController(OffreService offreService) {
        this.offreService = offreService;
    }

    @Override // public — F19, dépôt spontané, pas de compte requis
    public ResponseEntity<?> create(CreateOffreDTO dto, HttpServletRequest request) {
        return offreService.create(dto, request);
    }

    @Override // public — protégé par le jeton de dépôt, images uniquement
    public ResponseEntity<?> deposerPhotos(Integer id, String jeton, UploadPhotosOffreDTO dto, HttpServletRequest request) {
        return offreService.deposerPhotos(id, jeton, dto, request);
    }

}
