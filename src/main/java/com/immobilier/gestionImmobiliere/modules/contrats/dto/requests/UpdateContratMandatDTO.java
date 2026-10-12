package com.immobilier.gestionImmobiliere.modules.contrats.dto.requests;

import com.immobilier.gestionImmobiliere.donnees.contrats.model.ModeFacturation;
import com.immobilier.gestionImmobiliere.donnees.contrats.model.TypeMandat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class UpdateContratMandatDTO {
    private LocalDateTime dateFin;
    private BigDecimal commission;
    private ModeFacturation modeFacturation;
    private TypeMandat typeMandat;
}
