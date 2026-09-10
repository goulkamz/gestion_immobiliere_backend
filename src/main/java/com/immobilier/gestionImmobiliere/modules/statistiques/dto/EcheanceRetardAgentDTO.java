package com.immobilier.gestionImmobiliere.modules.statistiques.dto;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class EcheanceRetardAgentDTO {
    private Integer idEcheance;
    private String type;          // LOCATION ou MANDAT
    private String libelleContrat; // ex: "Villa 3 pièces - Kone Ibrahim" ou "Mandat cour COUR-2026-001"
    private Double montantDu;
    private String dateEcheance;
}