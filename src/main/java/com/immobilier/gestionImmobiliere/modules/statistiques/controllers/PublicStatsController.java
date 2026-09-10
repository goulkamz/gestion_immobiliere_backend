package com.immobilier.gestionImmobiliere.modules.statistiques.controllers;

import com.immobilier.gestionImmobiliere.modules.statistiques.apis.PublicStatsAPI;
import com.immobilier.gestionImmobiliere.modules.statistiques.services.PublicStatsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PublicStatsController implements PublicStatsAPI {

    private final PublicStatsService publicStatsService;

    public PublicStatsController(PublicStatsService publicStatsService) {
        this.publicStatsService = publicStatsService;
    }

    @Override
    public ResponseEntity<?> getStats() { return publicStatsService.getStats(); }

    @Override
    public ResponseEntity<?> getDernieresAnnonces() { return publicStatsService.getDernieresAnnonces(); }
}