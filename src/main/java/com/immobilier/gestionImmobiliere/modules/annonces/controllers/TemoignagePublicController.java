package com.immobilier.gestionImmobiliere.modules.annonces.controllers;

import com.immobilier.gestionImmobiliere.modules.annonces.apis.TemoignagePublicAPI;
import com.immobilier.gestionImmobiliere.modules.annonces.dto.requests.CreateTemoignagePublicDTO;
import com.immobilier.gestionImmobiliere.modules.annonces.services.TemoignageService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TemoignagePublicController implements TemoignagePublicAPI {

    private final TemoignageService temoignageService;

    public TemoignagePublicController(TemoignageService temoignageService) {
        this.temoignageService = temoignageService;
    }

    @Override
    public ResponseEntity<?> getAll() {
        return temoignageService.getAllActifs();
    }

    @Override
    public ResponseEntity<?> create(CreateTemoignagePublicDTO dto, HttpServletRequest request) {
        return temoignageService.createPublic(dto, request);
    }
}
