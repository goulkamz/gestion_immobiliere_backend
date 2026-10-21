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
    public ResponseEntity<?> getStatsParVille(LocalDate debut, LocalDate fin) {
        return adminStatsService.getStatsParVille(debut, fin);
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

    @Override
    public ResponseEntity<?> getFinancesPeriode(LocalDate debut, LocalDate fin) { return adminStatsService.getFinancesPeriode(debut, fin); }

    @Override
    public ResponseEntity<?> viderCacheStats() { return adminStatsService.viderCache(); }

    @Override
    public ResponseEntity<?> getActivitePeriode(LocalDate debut, LocalDate fin) { return adminStatsService.getActivitePeriode(debut, fin); }

    @Override
    public ResponseEntity<?> getEvolutionMensuelle(int mois) { return adminStatsService.getEvolutionMensuelle(mois); }
}