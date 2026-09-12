package com.immobilier.gestionImmobiliere.modules.biens.dto.requests;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateBienServiceDTO {
    private Integer idSecteur;
    private Integer idCategorie;
    private String libelle;
    private String description;
    private BigDecimal prixJournalier;
    private BigDecimal prixMensuel;
}