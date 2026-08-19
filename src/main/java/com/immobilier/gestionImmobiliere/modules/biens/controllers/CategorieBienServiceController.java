package com.immobilier.gestionImmobiliere.modules.biens.controllers;

import com.immobilier.gestionImmobiliere.modules.biens.apis.CategorieBienServiceAPI;
import com.immobilier.gestionImmobiliere.modules.biens.dto.requests.CreateCategorieDTO;
import com.immobilier.gestionImmobiliere.modules.biens.dto.requests.UpdateCategorieDTO;
import com.immobilier.gestionImmobiliere.modules.biens.services.CategorieBienServiceService;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CategorieBienServiceController implements CategorieBienServiceAPI {

    private final CategorieBienServiceService categorieService;

    public CategorieBienServiceController(CategorieBienServiceService categorieService) {
        this.categorieService = categorieService;
    }

    @Override
    public ResponseEntity<?> getAll(Pageable pageable) {
        return categorieService.getAll(pageable);
    }

    @Override
    public ResponseEntity<?> getById(Integer id) {
        return categorieService.getById(id);
    }

    @Override
    public ResponseEntity<?> create(CreateCategorieDTO dto) {
        return categorieService.create(dto);
    }

    @Override
    public ResponseEntity<?> update(Integer id, UpdateCategorieDTO dto) {
        return categorieService.update(id, dto);
    }

    @Override
    public ResponseEntity<?> delete(Integer id) {
        return categorieService.delete(id);
    }
}