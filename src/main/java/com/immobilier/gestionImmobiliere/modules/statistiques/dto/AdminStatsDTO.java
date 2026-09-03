package com.immobilier.gestionImmobiliere.modules.statistiques.dto.responses;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data @Builder
public class AdminStatsDTO {

    // --- Utilisateurs ---
    private long totalUtilisateurs;
    private Map<String, Long> utilisateursParRole;
    private long inscriptionsCeMois;

    // --- Biens immobiliers ---
    private long totalCours;
    private Map<String, Long> maisonsParStatut;
    private Double tauxOccupationMaisons;
    private Map<String, Long> biensServiceParDisponibilite;

    // --- Contrats ---
    private Map<String, Long> mandatsParStatut;
    private Map<String, Long> contratsLocationParStatut;
    private long mandatsExpirantSous30Jours;

    // --- Finances : loyers & commissions (echeance_loyer) ---
    private Double montantDuTotal;
    private Double montantPayeTotal;
    private long nombreEcheancesEnRetard;
    private Double montantEcheancesEnRetard;

    // --- Finances : locations biens/services ---
    private Map<String, Long> locationsBienServiceParStatut;
    private Double totalEncaisseBienService;
    private Double totalRembourseBienService;

    // --- Annonces & mise en relation ---
    private Map<String, Long> annoncesParStatut;
    private Map<String, Long> demandesParStatut;
    private long offresNonTraitees;
    private long contactsNonLus;

    // --- Activité système ---
    private long connexionsRecentes7j;
}