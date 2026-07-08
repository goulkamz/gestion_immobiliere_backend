package com.immobilier.gestionImmobiliere.modules.contrats.dto.responses;

import com.immobilier.gestionImmobiliere.donnees.contrats.model.StatutMandat;
import com.immobilier.gestionImmobiliere.donnees.contrats.model.TypeMandat;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data @Builder
public class ContratMandatResponseDTO {
    private Integer idMandat;
    private Integer idCour;
    private String referenceCours;
    private Integer idAgent;
    private String nomAgent;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private TypeMandat typeMandat;
    private BigDecimal commission;
    private StatutMandat statut;
    private String motifResiliation;
}