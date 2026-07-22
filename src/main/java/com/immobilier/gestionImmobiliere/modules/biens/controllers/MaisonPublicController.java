package com.immobilier.gestionImmobiliere.modules.biens.controllers;

import com.immobilier.gestionImmobiliere.donnees.biens.model.StatutMaison;
import com.immobilier.gestionImmobiliere.modules.biens.apis.MaisonPublicAPI;
import com.immobilier.gestionImmobiliere.modules.biens.services.MaisonService;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

public class MaisonPublicController implements MaisonPublicAPI {

    private final MaisonService maisonService;

    public MaisonPublicController(MaisonService maisonService) {
        this.maisonService = maisonService;
    }

    @Override
    public ResponseEntity<?> getAll(Integer idCour, StatutMaison statut, Pageable pageable) {
        return maisonService.getAll(idCour, statut, pageable);
    }

    @Override
    public ResponseEntity<?> getById(Integer id) {
        return maisonService.getById(id);
    }

}
