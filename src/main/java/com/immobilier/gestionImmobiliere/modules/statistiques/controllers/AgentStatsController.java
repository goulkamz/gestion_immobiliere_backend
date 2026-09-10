package com.immobilier.gestionImmobiliere.modules.statistiques.controllers;

import com.immobilier.gestionImmobiliere.modules.statistiques.apis.AgentStatsAPI;
import com.immobilier.gestionImmobiliere.modules.statistiques.services.AgentStatsService;
import com.immobilier.gestionImmobiliere.modules.user.jwtService.UserDetailsImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AgentStatsController implements AgentStatsAPI {

    private final AgentStatsService agentStatsService;

    public AgentStatsController(AgentStatsService agentStatsService) {
        this.agentStatsService = agentStatsService;
    }

    @Override
    public ResponseEntity<?> getDemandesEnAttente() { return agentStatsService.getDemandesEnAttente(); }

    @Override
    public ResponseEntity<?> getReservationsEnAttente() { return agentStatsService.getReservationsEnAttente(); }

    @Override
    public ResponseEntity<?> getMesMandats(UserDetailsImpl currentUser) {
        return agentStatsService.getMesMandats(currentUser.getIdUser());
    }

    @Override
    public ResponseEntity<?> getEcheancesEnRetard(UserDetailsImpl currentUser) {
        return agentStatsService.getEcheancesEnRetard(currentUser.getIdUser());
    }
}