package com.immobilier.gestionImmobiliere.modules.biens.controllers;

import com.immobilier.gestionImmobiliere.modules.biens.apis.MaisonPublicAPI;
import com.immobilier.gestionImmobiliere.modules.biens.services.MaisonService;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MaisonPublicController implements MaisonPublicAPI {

    private final MaisonService maisonService;

    public MaisonPublicController(MaisonService maisonService) {
        this.maisonService = maisonService;
    }

    @Override
    public ResponseEntity<?> getAll(Integer idCour, String recherche, Pageable pageable) {
        return maisonService.getAllPublic(idCour, recherche, pageable);
    }

    @Override
    public ResponseEntity<?> getById(Integer id) {
        return maisonService.getByIdPublic(id);
    }

}
