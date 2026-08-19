package com.immobilier.gestionImmobiliere.modules.biens.apis;

import com.immobilier.gestionImmobiliere.modules.biens.dto.requests.CreateCategorieDTO;
import com.immobilier.gestionImmobiliere.modules.biens.dto.requests.UpdateCategorieDTO;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/categories")
public interface CategorieBienServiceAPI {

    // Lecture ouverte à tous les rôles authentifiés (catalogue consultable)
    @GetMapping
    ResponseEntity<?> getAll(Pageable pageable);

    @GetMapping("/{id}")
    ResponseEntity<?> getById(@PathVariable Integer id);

    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    @PostMapping
    ResponseEntity<?> create(@Valid @RequestBody CreateCategorieDTO dto);

    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    @PutMapping("/{id}")
    ResponseEntity<?> update(@PathVariable Integer id, @Valid @RequestBody UpdateCategorieDTO dto);

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    ResponseEntity<?> delete(@PathVariable Integer id);
}