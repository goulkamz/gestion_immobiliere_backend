package com.immobilier.gestionImmobiliere.modules.annonces.apis;

import com.immobilier.gestionImmobiliere.donnees.annonces.model.StatutAnnonce;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// Lecture seule, accessible sans compte : consultation du catalogue d'annonces
@RequestMapping("/api/public/annonces")
public interface AnnoncePublicAPI {

    @GetMapping
    ResponseEntity<?> getAll(@RequestParam(required = false) StatutAnnonce statut, Pageable pageable);

    @GetMapping("/{id}")
    ResponseEntity<?> getById(@PathVariable Integer id);
}