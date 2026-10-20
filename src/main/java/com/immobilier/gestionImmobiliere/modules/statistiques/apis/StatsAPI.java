package com.immobilier.gestionImmobiliere.modules.statistiques.apis;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@RequestMapping("/api/stats")
public interface StatsAPI {

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin")
    ResponseEntity<?> getAdminStats();

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/villes")
    ResponseEntity<?> getStatsParVille(@RequestParam(required = false) LocalDate debut,
                                       @RequestParam(required = false) LocalDate fin);

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/bailleurs/top")
    ResponseEntity<?> getTopBailleurs(@RequestParam(defaultValue = "10") int limite);

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/locataires/creances")
    ResponseEntity<?> getLocatairesEnCreance();

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/bailleurs/creanciers")
    ResponseEntity<?> getBailleursCreanciers();

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/gain-mensuel")
    ResponseEntity<?> getGainAgenceDuMois(@RequestParam(required = false) LocalDate periode);

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/finances")
    ResponseEntity<?> getFinancesPeriode(@RequestParam(required = false) LocalDate debut,
                                         @RequestParam(required = false) LocalDate fin);

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/cache/vider")
    ResponseEntity<?> viderCacheStats();

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/evolution")
    ResponseEntity<?> getEvolutionMensuelle(@RequestParam(defaultValue = "12") int mois);

}