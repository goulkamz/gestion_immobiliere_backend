package com.immobilier.gestionImmobiliere.modules.localisation.apis;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// Référentiel secteur, lecture libre
@RequestMapping("/api/public/secteurs")
public interface SecteurPublicAPI {

    @GetMapping
    ResponseEntity<?> getAll(@RequestParam(required = false) Integer idVille, Pageable pageable);

    @GetMapping("/{id}")
    ResponseEntity<?> getById(@PathVariable Integer id);
}