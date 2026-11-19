package com.immobilier.gestionImmobiliere.modules.depenses.dto;

import com.immobilier.gestionImmobiliere.donnees.depenses.model.CategorieDepense;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Modification d'un devis avant la réponse du bailleur, ou révision après une demande de renégociation
 * (le devis est alors soumis à nouveau). Les champs absents sont conservés.
 */
@Data
public class UpdateDevisDTO {
    @Size(max = 254) private String libelle;
    private CategorieDepense categorie;
    private String description;
    @Size(max = 254) private String prestataire;
    @Positive private BigDecimal montant;
    @Min(1) @Max(60) private Integer nombreMois;
}
