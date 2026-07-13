package com.immobilier.gestionImmobiliere.modules.localisation.controllers;

import com.immobilier.gestionImmobiliere.modules.localisation.apis.SecteurAPI;
import com.immobilier.gestionImmobiliere.modules.localisation.dto.requests.CreateSecteurDTO;
import com.immobilier.gestionImmobiliere.modules.localisation.dto.requests.UpdateSecteurDTO;
import com.immobilier.gestionImmobiliere.modules.localisation.services.SecteurService;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SecteurController implements SecteurAPI {

    private final SecteurService secteurService;

    public SecteurController(SecteurService secteurService) {
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

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<?> create(CreateSecteurDTO dto) {
        return secteurService.create(dto);
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<?> update(Integer id, UpdateSecteurDTO dto) {
        return secteurService.update(id, dto);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> delete(Integer id) {
        return secteurService.delete(id);
    }
}