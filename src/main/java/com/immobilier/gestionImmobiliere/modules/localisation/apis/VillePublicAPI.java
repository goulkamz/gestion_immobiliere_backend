package com.immobilier.gestionImmobiliere.modules.localisation.apis;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// Référentiel ville, lecture libre (formulaires publics : inscription, recherche, dépôt de demande)
@RequestMapping("/api/public/villes")
public interface VillePublicAPI {

    @GetMapping
    ResponseEntity<?> getAll(@RequestParam(required = false) Integer idPays, Pageable pageable);

    @GetMapping("/{id}")
    ResponseEntity<?> getById(@PathVariable Integer id);
}