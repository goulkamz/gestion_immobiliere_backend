package com.immobilier.gestionImmobiliere.modules.contrats.dto.requests;

import com.immobilier.gestionImmobiliere.donnees.contrats.model.TypeMandat;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreateContratMandatDTO {
    @NotNull private Integer idCour;
    @NotNull private Integer idAgent;
    @NotNull private LocalDate dateDebut;
    private LocalDate dateFin;
    @NotNull private TypeMandat typeMandat;
    private BigDecimal commission;
    private String modeFacturation;
}