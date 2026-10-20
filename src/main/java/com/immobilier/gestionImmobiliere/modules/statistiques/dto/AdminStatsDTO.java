package com.immobilier.gestionImmobiliere.modules.statistiques.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data @Builder
public class AdminStatsDTO {

    // --- Utilisateurs ---
    private long totalUtilisateurs;
    private Map<String, Long> utilisateursParRole;
    private long inscriptionsCeMois;
    private long connexionsRecentes7j;

    // --- Biens immobiliers ---
    private long totalCours;
    private Map<String, Long> maisonsParStatut;
    private BigDecimal tauxOccupationMaisons;
    private Map<String, Map<String, Long>> biensServiceParDisponibilite;

    // --- Contrats ---
    private Map<String, Long> mandatsParStatut;
    private Map<String, Long> contratsLocationParStatut;
    private long mandatsExpirantSous30Jours;

    // --- Finances : flux entrant (loyers) et flux sortant (reversements), jamais mélangés ---
    private LoyersStatsDTO loyers;
    private ReversementsStatsDTO reversements;

    // --- Finances : locations biens/services ---
    private Map<String, Long> locationsBienServiceParStatut;
    private BigDecimal totalEncaisseBienService;
    private BigDecimal totalRembourseBienService;

    // --- Annonces & mise en relation ---
    private Map<String, Long> annoncesParStatut;
    private Map<String, Long> demandesParStatut;
    private long offresNonTraitees;
    private long contactsNonLus;

}