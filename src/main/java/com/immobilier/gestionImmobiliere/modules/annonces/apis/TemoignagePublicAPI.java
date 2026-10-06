package com.immobilier.gestionImmobiliere.modules.annonces.apis;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

// Lecture seule, sans compte : temoignages affiches sur la page d'accueil (tableau brut, sans pagination)
@RequestMapping("/api/temoignages")
public interface TemoignagePublicAPI {

    @GetMapping
    ResponseEntity<?> getAll();
}
