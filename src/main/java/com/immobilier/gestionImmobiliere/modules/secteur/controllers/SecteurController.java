package com.immobilier.gestionImmobiliere.modules.secteur.controllers;

import com.immobilier.gestionImmobiliere.modules.secteur.apis.SecteurAPI;
import com.immobilier.gestionImmobiliere.modules.secteur.dto.requests.CreateSecteurDTO;
import com.immobilier.gestionImmobiliere.modules.secteur.dto.requests.UpdateSecteurDTO;
import com.immobilier.gestionImmobiliere.modules.secteur.services.SecteurService;
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
    public ResponseEntity<?> getAll(Long idVille, Pageable pageable) {
        return secteurService.getAll(idVille, pageable);
    }

    @Override
    public ResponseEntity<?> getById(Long id) {
        return secteurService.getById(id);
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<?> create(CreateSecteurDTO dto) {
        return secteurService.create(dto);
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<?> update(Long id, UpdateSecteurDTO dto) {
        return secteurService.update(id, dto);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> delete(Long id) {
        return secteurService.delete(id);
    }
}