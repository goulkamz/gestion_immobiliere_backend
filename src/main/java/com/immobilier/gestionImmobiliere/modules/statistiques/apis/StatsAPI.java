package com.immobilier.gestionImmobiliere.modules.statistiques.apis;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RequestMapping("/api/stats")
public interface StatsAPI {

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin")
    ResponseEntity<?> getAdminStats();

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/villes")
    ResponseEntity<?> getStatsParVille();

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/bailleurs/top")
    ResponseEntity<?> getTopBailleurs(@RequestParam(defaultValue = "10") int limite);

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/locataires/creances")
    ResponseEntity<?> getLocatairesEnCreance();
}