package com.immobilier.gestionImmobiliere.modules.statistiques.controllers;

import com.immobilier.gestionImmobiliere.modules.statistiques.apis.ClientStatsAPI;
import com.immobilier.gestionImmobiliere.modules.statistiques.services.ClientStatsService;
import com.immobilier.gestionImmobiliere.modules.user.jwtService.UserDetailsImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ClientStatsController implements ClientStatsAPI {

    private final ClientStatsService clientStatsService;

    public ClientStatsController(ClientStatsService clientStatsService) {
        this.clientStatsService = clientStatsService;
    }

    @Override
    public ResponseEntity<?> getMesLocations(UserDetailsImpl currentUser) {
        return clientStatsService.getMesLocations(currentUser.getIdUser());
    }

    @Override
    public ResponseEntity<?> getMesEcheances(UserDetailsImpl currentUser) {
        return clientStatsService.getMesEcheances(currentUser.getIdUser());
    }

    @Override
    public ResponseEntity<?> getMesRemboursements(UserDetailsImpl currentUser) {
        return clientStatsService.getMesRemboursements(currentUser.getIdUser());
    }
}