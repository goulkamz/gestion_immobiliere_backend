package com.immobilier.gestionImmobiliere.modules.biens.dto.requests;

import lombok.Data;

@Data
public class UpdateBienServiceDTO {
    private Integer idSecteur;
    private Integer idCategorie;
    private String libelle;
    private String description;
    private Double prixJournalier;
    private Double prixMensuel;
}