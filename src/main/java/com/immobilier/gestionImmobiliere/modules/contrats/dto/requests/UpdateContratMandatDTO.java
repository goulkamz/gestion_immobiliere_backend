package com.immobilier.gestionImmobiliere.modules.contrats.dto.requests;

import com.immobilier.gestionImmobiliere.donnees.contrats.model.ModeFacturation;
import com.immobilier.gestionImmobiliere.donnees.contrats.model.TypeMandat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class UpdateContratMandatDTO {
    private LocalDateTime dateFin;
    private BigDecimal commission;
    private ModeFacturation modeFacturation;
    private TypeMandat typeMandat;

    // Avenant : obligatoires pour modifier un contrat ACTIF (le client doit avoir accepte la modification)
    private String motif;
    private Boolean accordClient;
    /** Date d'effet de la modification ; aujourd'hui par defaut, jamais dans le passe. */
    private LocalDate dateEffet;
}
