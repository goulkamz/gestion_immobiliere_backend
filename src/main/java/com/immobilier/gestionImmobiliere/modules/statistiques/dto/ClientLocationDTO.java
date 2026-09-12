package com.immobilier.gestionImmobiliere.modules.statistiques.dto;

import com.immobilier.gestionImmobiliere.donnees.biens.model.StatutLocationBienService;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @Builder
public class ClientLocationDTO {
    private Integer idLocation;
    private String libelleBien;
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;
    private BigDecimal montantTotal;
    private BigDecimal totalEncaisse;
    private BigDecimal solde;   // positif = reste à payer, négatif = trop-perçu
    private StatutLocationBienService statut;
}