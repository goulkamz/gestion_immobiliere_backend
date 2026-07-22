package com.immobilier.gestionImmobiliere.modules.localisation.controllers;

import com.immobilier.gestionImmobiliere.modules.localisation.apis.VillePublicAPI;
import com.immobilier.gestionImmobiliere.modules.localisation.services.VilleService;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

public class VillePublicController implements VillePublicAPI {

    private final VilleService villeService;

    public VillePublicController(VilleService villeService) {
        this.villeService = villeService;
    }

    @Override
    public ResponseEntity<?> getAll(Integer idPays, Pageable pageable) {
        return villeService.getAll(idPays, pageable);
    }

    @Override
    public ResponseEntity<?> getById(Integer id) {
        return villeService.getById(id);
    }

}
