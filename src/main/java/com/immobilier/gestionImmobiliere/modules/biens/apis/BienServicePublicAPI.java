package com.immobilier.gestionImmobiliere.modules.biens.apis;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RequestMapping("/api/public/biens-services")
public interface BienServicePublicAPI {
    // Catalogue consultable par tous (filtres facultatifs)
    @GetMapping
    ResponseEntity<?> getAll(@RequestParam(required = false) Integer idSecteur,
                             @RequestParam(required = false) Integer idCategorie,
                             Pageable pageable);

    @GetMapping("/{id}")
    ResponseEntity<?> getById(@PathVariable Integer id);
}
