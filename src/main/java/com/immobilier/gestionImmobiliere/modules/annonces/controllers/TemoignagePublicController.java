package com.immobilier.gestionImmobiliere.modules.annonces.controllers;

import com.immobilier.gestionImmobiliere.modules.annonces.apis.TemoignagePublicAPI;
import com.immobilier.gestionImmobiliere.modules.annonces.services.TemoignageService;
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
}
