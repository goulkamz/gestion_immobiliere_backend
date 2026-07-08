package com.immobilier.gestionImmobiliere.modules.biens.controllers;

import com.immobilier.gestionImmobiliere.modules.biens.apis.CourAPI;
import com.immobilier.gestionImmobiliere.modules.biens.dto.requests.CreateCourDTO;
import com.immobilier.gestionImmobiliere.modules.biens.dto.requests.UpdateCourDTO;
import com.immobilier.gestionImmobiliere.modules.biens.services.CourService;
import com.immobilier.gestionImmobiliere.modules.user.jwtService.UserDetailsImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CourController implements CourAPI {

    private final CourService courService;

    public CourController(CourService courService) {
        this.courService = courService;
    }

    @Override
    public ResponseEntity<?> getAll(Long idSecteur, Pageable pageable) {
        return courService.getAll(idSecteur, pageable);
    }

    @Override
    public ResponseEntity<?> getById(Integer id) {
        return courService.getById(id);
    }

    @Override
    public ResponseEntity<?> create(CreateCourDTO dto) {
        return courService.create(dto);
    }

    @Override
    public ResponseEntity<?> update(Integer id, UpdateCourDTO dto) {
        return courService.update(id,dto);
    }

    @Override
    @PreAuthorize("hasRole('AGENT')")
    public ResponseEntity<?> create(CreateCourDTO dto, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return courService.create(dto, currentUser.getIdUser());
    }

    @Override
    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    public ResponseEntity<?> update(Integer id, UpdateCourDTO dto, @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return courService.update(id, dto, currentUser.getIdUser());
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> delete(Integer id) {
        return courService.delete(id);
    }
}