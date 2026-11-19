package com.immobilier.gestionImmobiliere.modules.depenses.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/** Accord d'un devis donné par le bailleur hors plateforme, enregistré par l'agent : qui, quand, par quel moyen. */
@Data
public class AccordHorsPlateformeDTO {
    @NotBlank @Size(max = 500) private String motif;
    @NotNull @PastOrPresent private LocalDate dateAccord;
}
