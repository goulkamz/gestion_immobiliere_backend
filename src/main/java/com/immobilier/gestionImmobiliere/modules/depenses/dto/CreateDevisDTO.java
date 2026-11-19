package com.immobilier.gestionImmobiliere.modules.depenses.dto;

import com.immobilier.gestionImmobiliere.donnees.depenses.model.CategorieDepense;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateDevisDTO {
    @NotNull private Integer idCour;
    /** Facultatif : la maison concernée, sinon les travaux portent sur la cour entière. */
    private Integer idMaison;
    @NotBlank @Size(max = 254) private String libelle;
    @NotNull private CategorieDepense categorie;
    private String description;
    @Size(max = 254) private String prestataire;
    @NotNull @Positive private BigDecimal montant;
    /** Étalement proposé en nombre de reversements ; 1 par défaut (retenue d'un coup). */
    @Min(1) @Max(60) private Integer nombreMois;
}
