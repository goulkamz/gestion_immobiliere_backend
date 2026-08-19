package com.immobilier.gestionImmobiliere.modules.biens.dto.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateBienServiceDTO {
    @NotNull private Integer idSecteur;
    @NotNull private Integer idCategorie;
    @NotNull
    private Integer idGestionnaire;
    @NotBlank
    private String libelle;
    private String description;
    private Double prixJournalier;
    private Double prixMensuel;
}