package com.immobilier.gestionImmobiliere.modules.statistiques.apis;

import com.immobilier.gestionImmobiliere.modules.user.jwtService.UserDetailsImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/api/stats/bailleur")
public interface BailleurStatsAPI {

    @PreAuthorize("hasAnyRole('BAILLEUR','ADMIN')")
    @GetMapping("/patrimoine")
    ResponseEntity<?> getPatrimoine(@AuthenticationPrincipal UserDetailsImpl currentUser);

    @PreAuthorize("hasAnyRole('BAILLEUR','ADMIN')")
    @GetMapping("/revenus")
    ResponseEntity<?> getRevenus(@AuthenticationPrincipal UserDetailsImpl currentUser);

    @PreAuthorize("hasAnyRole('BAILLEUR','ADMIN')")
    @GetMapping("/mandat")
    ResponseEntity<?> getMandatActif(@AuthenticationPrincipal UserDetailsImpl currentUser);
}