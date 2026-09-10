package com.immobilier.gestionImmobiliere.modules.statistiques.controllers;

import com.immobilier.gestionImmobiliere.modules.statistiques.apis.StatsAPI;
import com.immobilier.gestionImmobiliere.modules.statistiques.services.AdminStatsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
public class StatsController implements StatsAPI {

    private final AdminStatsService adminStatsService;

    public StatsController(AdminStatsService adminStatsService) {
        this.adminStatsService = adminStatsService;
    }

    @Override
    public ResponseEntity<?> getAdminStats() { return adminStatsService.getStats(); }

    @Override
    public ResponseEntity<?> getStatsParVille() {
        return adminStatsService.getStatsParVille();
    }

    @Override
    public ResponseEntity<?> getTopBailleurs(int limite) {
        return adminStatsService.getTopBailleurs(limite);
    }

    @Override
    public ResponseEntity<?> getLocatairesEnCreance() {
        return adminStatsService.getLocatairesEnCreance();
    }

    @Override
    public ResponseEntity<?> getBailleursCreanciers() { return adminStatsService.getBailleursCreanciers(); }

    @Override
    public ResponseEntity<?> getGainAgenceDuMois(LocalDate periode) { return adminStatsService.getGainAgenceDuMois(periode); }
}