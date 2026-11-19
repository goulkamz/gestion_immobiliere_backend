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
import java.time.LocalDate;

@Data
public class CreateDepenseDTO {
    @NotNull private Integer idCour;
    /** Facultatif : la maison concernée, sinon la dépense porte sur la cour entière. */
    private Integer idMaison;
    @NotBlank @Size(max = 254) private String libelle;
    @NotNull private CategorieDepense categorie;
    private String description;
    @Size(max = 254) private String prestataire;
    /** Aujourd'hui par défaut, jamais dans le futur. */
    private LocalDate dateDepense;
    @NotNull @Positive private BigDecimal montant;
    /** Étalement en nombre de reversements ; 1 par défaut (retenue d'un coup). */
    @Min(1) @Max(60) private Integer nombreMois;
    /** Obligatoire à vrai quand le montant dépasse le plafond convenu dans le mandat. */
    private Boolean accordBailleur;
    /** Qui a donné l'accord, quand et comment ; obligatoire quand l'accord est attesté au-dessus du plafond. */
    @Size(max = 500) private String accordMotif;
}
