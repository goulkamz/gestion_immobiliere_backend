package com.immobilier.gestionImmobiliere.modules.paiements.apis;

import com.immobilier.gestionImmobiliere.donnees.paiements.model.StatutEcheance;
import com.immobilier.gestionImmobiliere.donnees.paiements.model.TypeEcheance;
import com.immobilier.gestionImmobiliere.modules.paiements.dto.requests.ConfirmerVirementDTO;
import com.immobilier.gestionImmobiliere.modules.user.jwtService.UserDetailsImpl;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RequestMapping("/api/echeances")
public interface EcheanceAPI {

    @GetMapping
    ResponseEntity<?> getAll(@RequestParam(required = false) TypeEcheance type,
                             @RequestParam(required = false) Integer entiteId,
                             @RequestParam(required = false) StatutEcheance statut,
                             Pageable pageable,
                             @AuthenticationPrincipal UserDetailsImpl currentUser);

    @GetMapping("/{id}")
    ResponseEntity<?> getById(@PathVariable Integer id,@AuthenticationPrincipal UserDetailsImpl currentUser);

    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    @GetMapping("/en-retard-location")
    ResponseEntity<?> getEcheanceLocationEnRetard();

    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    @GetMapping("/en-retard-mandat")
    ResponseEntity<?> getEcheanceMandatEnRetard();

    @PostMapping("/mandats/{idMandat}/calculer")
    ResponseEntity<?> calculerReversementMandat(@PathVariable Integer idMandat, @RequestParam LocalDate periode, @AuthenticationPrincipal UserDetailsImpl currentUser);

    @PatchMapping("/{idEcheance}/confirmer-virement")
    ResponseEntity<?> confirmerVirementMandat(@PathVariable Integer idEcheance, @Valid @RequestBody ConfirmerVirementDTO dto, @AuthenticationPrincipal UserDetailsImpl currentUser);

    @GetMapping("/mandats/{idMandat}/en-attente")
    ResponseEntity<?> getReversementsEnAttente(@PathVariable Integer idMandat);

}