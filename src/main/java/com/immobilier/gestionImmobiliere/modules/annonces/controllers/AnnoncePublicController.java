package com.immobilier.gestionImmobiliere.modules.annonces.controllers;

import com.immobilier.gestionImmobiliere.donnees.annonces.model.StatutAnnonce;
import com.immobilier.gestionImmobiliere.modules.annonces.apis.AnnoncePublicAPI;
import com.immobilier.gestionImmobiliere.modules.annonces.services.AnnonceService;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

public class AnnoncePublicController implements AnnoncePublicAPI {

    private final AnnonceService annonceService;

    public AnnoncePublicController(AnnonceService annonceService) {
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

}
