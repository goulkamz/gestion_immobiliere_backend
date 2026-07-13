package com.immobilier.gestionImmobiliere.modules.localisation.controllers;

import com.immobilier.gestionImmobiliere.modules.localisation.apis.PaysAPI;
import com.immobilier.gestionImmobiliere.modules.localisation.dto.requests.CreatePaysDTO;
import com.immobilier.gestionImmobiliere.modules.localisation.dto.requests.UpdatePaysDTO;
import com.immobilier.gestionImmobiliere.modules.localisation.services.PaysService;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PaysController implements PaysAPI {

    private final PaysService paysService;

    public PaysController(PaysService paysService) {
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

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<?> create(CreatePaysDTO dto) {
        return paysService.create(dto);
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<?> update(Integer id, UpdatePaysDTO dto) {
        return paysService.update(id, dto);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> delete(Integer id) {
        return paysService.delete(id);
    }
}