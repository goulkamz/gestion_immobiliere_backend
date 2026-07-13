package com.immobilier.gestionImmobiliere.modules.contrats.dto.requests;

import com.immobilier.gestionImmobiliere.donnees.contrats.model.TypeMandat;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CreateContratMandatDTO {
    @NotNull private Integer idCour;
    @NotNull private Integer idAgent;
    @NotNull private LocalDateTime dateDebut;
    private LocalDateTime dateFin;
    @NotNull private TypeMandat typeMandat;
    private BigDecimal commission;
    private String modeFacturation;
}