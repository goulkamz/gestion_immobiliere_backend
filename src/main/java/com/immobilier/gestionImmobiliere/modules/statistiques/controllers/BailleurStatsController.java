package com.immobilier.gestionImmobiliere.modules.statistiques.controllers;

import com.immobilier.gestionImmobiliere.modules.statistiques.apis.BailleurStatsAPI;
import com.immobilier.gestionImmobiliere.modules.statistiques.services.BailleurStatsService;
import com.immobilier.gestionImmobiliere.modules.user.jwtService.UserDetailsImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BailleurStatsController implements BailleurStatsAPI {

    private final BailleurStatsService bailleurStatsService;

    public BailleurStatsController(BailleurStatsService bailleurStatsService) {
        this.bailleurStatsService = bailleurStatsService;
    }

    @Override
    public ResponseEntity<?> getPatrimoine(UserDetailsImpl currentUser) {
        return bailleurStatsService.getPatrimoine(currentUser.getIdUser());
    }

    @Override
    public ResponseEntity<?> getRevenus(UserDetailsImpl currentUser) {
        return bailleurStatsService.getRevenus(currentUser.getIdUser());
    }

    @Override
    public ResponseEntity<?> getMandatActif(UserDetailsImpl currentUser) {
        return bailleurStatsService.getMandatActif(currentUser.getIdUser());
    }
}