package com.immobilier.gestionImmobiliere.modules.annonces.apis;

import com.immobilier.gestionImmobiliere.donnees.annonces.model.StatutAnnonce;
import com.immobilier.gestionImmobiliere.modules.annonces.dto.requests.*;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/annonces")
public interface AnnonceAPI {

    @GetMapping
    ResponseEntity<?> getAll(@RequestParam(required = false) StatutAnnonce statut, Pageable pageable);

    @GetMapping("/{id}")
    ResponseEntity<?> getById(@PathVariable Integer id);

    @PostMapping
    ResponseEntity<?> create(@Valid @RequestBody CreateAnnonceDTO dto);

    @PutMapping("/{id}")
    ResponseEntity<?> update(@PathVariable Integer id, @Valid @RequestBody UpdateAnnonceDTO dto);

    @PatchMapping("/{id}/statut")
    ResponseEntity<?> updateStatut(@PathVariable Integer id, @Valid @RequestBody UpdateStatutAnnonceDTO dto);

    @DeleteMapping("/{id}")
    ResponseEntity<?> delete(@PathVariable Integer id);
}