package com.immobilier.gestionImmobiliere.modules.parametres.controllers;

import com.immobilier.gestionImmobiliere.modules.parametres.apis.ParametreAPI;
import com.immobilier.gestionImmobiliere.modules.parametres.dto.requests.UpdateParametreDTO;
import com.immobilier.gestionImmobiliere.modules.parametres.services.ParametreService;
import com.immobilier.gestionImmobiliere.modules.user.jwtService.UserDetailsImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ParametreController implements ParametreAPI {

    private final ParametreService parametreService;

    public ParametreController(ParametreService parametreService) {
        this.parametreService = parametreService;
    }

    @Override
    public ResponseEntity<?> getAll() {
        return parametreService.getAll();
    }

    @Override
    public ResponseEntity<?> mettreAJour(String cle, UpdateParametreDTO dto, UserDetailsImpl currentUser) {
        return parametreService.mettreAJour(cle, dto, currentUser.getIdUser());
    }
}