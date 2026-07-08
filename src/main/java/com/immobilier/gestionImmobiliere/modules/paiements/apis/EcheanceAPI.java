package com.immobilier.gestionImmobiliere.modules.paiements.apis;

import com.immobilier.gestionImmobiliere.donnees.paiements.model.StatutEcheance;
import com.immobilier.gestionImmobiliere.donnees.paiements.model.TypeEcheance;
import com.immobilier.gestionImmobiliere.modules.user.jwtService.UserDetailsImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/echeances")
public interface EcheanceAPI {

    @GetMapping
    ResponseEntity<?> getAll(@RequestParam(required = false) TypeEcheance type,
                             @RequestParam(required = false) Integer entiteId,
                             @RequestParam(required = false) StatutEcheance statut,
                             Pageable pageable,
                             @AuthenticationPrincipal UserDetailsImpl currentUser);

    @GetMapping("/{id}")
    ResponseEntity<?> getById(@PathVariable Integer id);

    @GetMapping("/en-retard")
    ResponseEntity<?> getEnRetard();
}