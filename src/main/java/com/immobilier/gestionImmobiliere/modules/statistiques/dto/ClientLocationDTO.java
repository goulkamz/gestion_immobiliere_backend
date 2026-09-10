package com.immobilier.gestionImmobiliere.modules.statistiques.dto;

import com.immobilier.gestionImmobiliere.donnees.biens.model.StatutLocationBienService;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data @Builder
public class ClientLocationDTO {
    private Integer idLocation;
    private String libelleBien;
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;
    private Double montantTotal;
    private Double totalEncaisse;
    private Double solde;   // positif = reste à payer, négatif = trop-perçu
    private StatutLocationBienService statut;
}