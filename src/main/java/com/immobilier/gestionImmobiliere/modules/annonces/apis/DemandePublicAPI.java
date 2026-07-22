package com.immobilier.gestionImmobiliere.modules.annonces.apis;

import com.immobilier.gestionImmobiliere.modules.annonces.dto.requests.CreateDemandeDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

// POST public — dépôt de besoin par un visiteur (F18)
@RequestMapping("/api/public/demandes")
public interface DemandePublicAPI {
    @PostMapping
    ResponseEntity<?> create(@Valid @RequestBody CreateDemandeDTO dto);
}