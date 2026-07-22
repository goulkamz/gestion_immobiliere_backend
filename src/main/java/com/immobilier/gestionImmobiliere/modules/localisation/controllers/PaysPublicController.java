package com.immobilier.gestionImmobiliere.modules.localisation.controllers;

import com.immobilier.gestionImmobiliere.modules.localisation.apis.PaysPublicAPI;
import com.immobilier.gestionImmobiliere.modules.localisation.dto.requests.CreatePaysDTO;
import com.immobilier.gestionImmobiliere.modules.localisation.dto.requests.UpdatePaysDTO;
import com.immobilier.gestionImmobiliere.modules.localisation.services.PaysService;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PaysPublicController implements PaysPublicAPI {

    private final PaysService paysService;

    public PaysPublicController(PaysService paysService) {
        this.paysService = paysService;
    }

    @Override
    public ResponseEntity<?> getAll(Pageable pageable) {
        return paysService.getAll(pageable);
    }

    @Override
    public ResponseEntity<?> getById(Integer id) {
        return paysService.getById(id);
    }


}