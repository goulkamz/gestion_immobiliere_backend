package com.immobilier.gestionImmobiliere.modules.biens.apis;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

// Consultation du catalogue (écran "Catalogue biens", accessible à "Tous")
@RequestMapping("/api/public/maisons")
public interface MaisonPublicAPI {
    // Ne retourne que les maisons DISPONIBLES dont la cour a un mandat ACTIF GESTION ou LOCATION
    @GetMapping
    ResponseEntity<?> getAll(@RequestParam(required = false) Integer idCour,
                             @RequestParam(required = false) String recherche, Pageable pageable);

    @GetMapping("/{id}")
    ResponseEntity<?> getById(@PathVariable Integer id);
}
