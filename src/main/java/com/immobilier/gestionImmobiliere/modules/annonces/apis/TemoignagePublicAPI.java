package com.immobilier.gestionImmobiliere.modules.annonces.apis;

import com.immobilier.gestionImmobiliere.modules.annonces.dto.requests.CreateTemoignagePublicDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

// Sans compte : temoignages affiches sur la page d'accueil (tableau brut, sans pagination)
@RequestMapping("/api/temoignages")
public interface TemoignagePublicAPI {

    @GetMapping
    ResponseEntity<?> getAll();

    // Depot public, en attente de validation (rate limit strict par IP, voir TemoignageService)
    @PostMapping
    ResponseEntity<?> create(@Valid @RequestBody CreateTemoignagePublicDTO dto, HttpServletRequest request);
}
