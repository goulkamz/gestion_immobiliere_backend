package com.immobilier.gestionImmobiliere.modules.statistiques.apis;

import com.immobilier.gestionImmobiliere.modules.user.jwtService.UserDetailsImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/api/stats/agent")
public interface AgentStatsAPI {

    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    @GetMapping("/demandes-en-attente")
    ResponseEntity<?> getDemandesEnAttente();

    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    @GetMapping("/reservations-en-attente")
    ResponseEntity<?> getReservationsEnAttente();

    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    @GetMapping("/mes-mandats")
    ResponseEntity<?> getMesMandats(@AuthenticationPrincipal UserDetailsImpl currentUser);

    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    @GetMapping("/echeances-retard")
    ResponseEntity<?> getEcheancesEnRetard(@AuthenticationPrincipal UserDetailsImpl currentUser);
}