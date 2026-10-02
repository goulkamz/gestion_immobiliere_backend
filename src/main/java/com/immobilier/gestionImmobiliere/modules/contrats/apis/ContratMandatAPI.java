package com.immobilier.gestionImmobiliere.modules.contrats.apis;

import com.immobilier.gestionImmobiliere.donnees.contrats.model.StatutMandat;
import com.immobilier.gestionImmobiliere.modules.contrats.dto.requests.CreateContratMandatDTO;
import com.immobilier.gestionImmobiliere.modules.contrats.dto.requests.ResilierMandatDTO;
import com.immobilier.gestionImmobiliere.modules.contrats.dto.requests.UpdateContratMandatDTO;
import com.immobilier.gestionImmobiliere.modules.user.jwtService.UserDetailsImpl;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/contrats-mandat")
public interface ContratMandatAPI {

    @GetMapping
    ResponseEntity<?> getAll(@RequestParam(required = false) Integer idCour,
                             @RequestParam(required = false) StatutMandat statut,
                             Pageable pageable,@AuthenticationPrincipal UserDetailsImpl currentUser);

    @GetMapping("/{id}")
    ResponseEntity<?> getById(@PathVariable Integer id,@AuthenticationPrincipal UserDetailsImpl currentUser);

    @PostMapping
    ResponseEntity<?> create(@Valid @RequestBody CreateContratMandatDTO dto, @AuthenticationPrincipal UserDetailsImpl currentUser);

    @PatchMapping("/{id}/activer")
    ResponseEntity<?> activer(@PathVariable Integer id, @AuthenticationPrincipal UserDetailsImpl currentUser);

    // Modifie dateFin/commission/modeFacturation/typeMandat sans changer le statut —
    // réservé aux mandats EN_ATTENTE/ACTIF (pas RESILIE/EXPIRE).
    @PatchMapping("/{id}/modifier")
    ResponseEntity<?> modifier(@PathVariable Integer id, @Valid @RequestBody UpdateContratMandatDTO dto, @AuthenticationPrincipal UserDetailsImpl currentUser);

    @PatchMapping("/{id}/resilierContratLocation")
    ResponseEntity<?> resilier(@PathVariable Integer id, @Valid @RequestBody ResilierMandatDTO dto, @AuthenticationPrincipal UserDetailsImpl currentUser);

    @DeleteMapping("/{id}")
    ResponseEntity<?> delete(@PathVariable Integer id);
}