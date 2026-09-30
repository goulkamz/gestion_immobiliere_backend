package com.immobilier.gestionImmobiliere.modules.parametres.apis;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

// Lecture seule, ouverte à tous (couverte par /api/public/** dans WebSecurityConfig) :
// permet au frontend de lire les constantes métier (délais, tolérances...) sans les
// dupliquer en dur, sans nécessiter le rôle ADMIN requis par ParametreAPI.
@RequestMapping("/api/public/parametres")
public interface ParametrePublicAPI {

    @GetMapping
    ResponseEntity<?> getAll();
}
