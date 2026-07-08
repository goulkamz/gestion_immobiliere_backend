package com.immobilier.gestionImmobiliere.modules.localisation.controllers;

import com.immobilier.gestionImmobiliere.modules.localisation.apis.VilleAPI;
import com.immobilier.gestionImmobiliere.modules.localisation.dto.requests.CreateVilleDTO;
import com.immobilier.gestionImmobiliere.modules.localisation.dto.requests.UpdateVilleDTO;
import com.immobilier.gestionImmobiliere.modules.localisation.services.VilleService;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class VilleController implements VilleAPI {

    private final VilleService villeService;

    public VilleController(VilleService villeService) {
        this.villeService = villeService;
    }

    @Override
    public ResponseEntity<?> getAll(Long idPays, Pageable pageable) {
        return villeService.getAll(idPays, pageable);
    }

    @Override
    public ResponseEntity<?> getById(Long id) {
        return villeService.getById(id);
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<?> create(CreateVilleDTO dto) {
        return villeService.create(dto);
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<?> update(Long id, UpdateVilleDTO dto) {
        return villeService.update(id, dto);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> delete(Long id) {
        return villeService.delete(id);
    }
}