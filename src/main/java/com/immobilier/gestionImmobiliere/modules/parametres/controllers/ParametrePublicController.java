package com.immobilier.gestionImmobiliere.modules.parametres.controllers;

import com.immobilier.gestionImmobiliere.modules.parametres.apis.ParametrePublicAPI;
import com.immobilier.gestionImmobiliere.modules.parametres.services.ParametreService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ParametrePublicController implements ParametrePublicAPI {

    private final ParametreService parametreService;

    public ParametrePublicController(ParametreService parametreService) {
        this.parametreService = parametreService;
    }

    @Override
    public ResponseEntity<?> getAll() {
        return parametreService.getPublics();
    }
}
