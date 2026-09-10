package com.immobilier.gestionImmobiliere.modules.statistiques.apis;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/api/stats/public")
public interface PublicStatsAPI {

    @GetMapping
    ResponseEntity<?> getStats();

    @GetMapping("/annonces")
    ResponseEntity<?> getDernieresAnnonces();
}