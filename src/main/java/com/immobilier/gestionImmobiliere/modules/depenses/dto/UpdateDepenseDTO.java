package com.immobilier.gestionImmobiliere.modules.depenses.dto;

import com.immobilier.gestionImmobiliere.donnees.depenses.model.CategorieDepense;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Modification d'une dépense tant que rien n'a encore été retenu ; les champs absents sont conservés. */
@Data
public class UpdateDepenseDTO {
    @Size(max = 254) private String libelle;
    private CategorieDepense categorie;
    private String description;
    @Size(max = 254) private String prestataire;
    private LocalDate dateDepense;
    @Positive private BigDecimal montant;
    @Min(1) @Max(60) private Integer nombreMois;
    private Boolean accordBailleur;
    /** Qui a donné l'accord, quand et comment ; obligatoire quand l'accord est attesté au-dessus du plafond. */
    @Size(max = 500) private String accordMotif;
}
