package com.immobilier.gestionImmobiliere.modules.localisation.controllers;

import com.immobilier.gestionImmobiliere.modules.localisation.apis.SecteurPublicAPI;
import com.immobilier.gestionImmobiliere.modules.localisation.services.SecteurService;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

public class SecteurPublicController implements SecteurPublicAPI {

    private final SecteurService secteurService;

    public SecteurPublicController(SecteurService secteurService) {
        this.secteurService = secteurService;
    }


    @Override
    public ResponseEntity<?> getAll(Integer idVille, Pageable pageable) {
        return secteurService.getAll(idVille, pageable);
    }

    @Override
    public ResponseEntity<?> getById(Integer id) {
        return secteurService.getById(id);
    }

}
