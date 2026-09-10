package com.immobilier.gestionImmobiliere.modules.statistiques.apis;

import com.immobilier.gestionImmobiliere.modules.user.jwtService.UserDetailsImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/api/stats/client")
public interface ClientStatsAPI {

    @PreAuthorize("hasRole('CLIENT')")
    @GetMapping("/locations")
    ResponseEntity<?> getMesLocations(@AuthenticationPrincipal UserDetailsImpl currentUser);

    @PreAuthorize("hasRole('CLIENT')")
    @GetMapping("/echeances")
    ResponseEntity<?> getMesEcheances(@AuthenticationPrincipal UserDetailsImpl currentUser);

    @PreAuthorize("hasRole('CLIENT')")
    @GetMapping("/remboursements")
    ResponseEntity<?> getMesRemboursements(@AuthenticationPrincipal UserDetailsImpl currentUser);
}