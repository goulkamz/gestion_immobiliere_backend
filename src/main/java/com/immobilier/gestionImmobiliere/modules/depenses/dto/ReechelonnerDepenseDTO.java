package com.immobilier.gestionImmobiliere.modules.depenses.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Nouvel étalement du solde restant d'une dépense en cours. */
@Data
public class ReechelonnerDepenseDTO {
    /** Nombre de reversements sur lesquels le solde restant sera retenu. */
    @NotNull @Min(1) @Max(60) private Integer nombreMoisRestants;
    @NotBlank @Size(max = 254) private String motif;
    /** Obligatoire à vrai pour raccourcir l'étalement (retenues plus fortes). */
    private Boolean accordBailleur;
}
