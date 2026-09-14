package com.immobilier.gestionImmobiliere.modules.contrats.dto.responses;

import com.immobilier.gestionImmobiliere.donnees.contrats.model.StatutDecompteSortie;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @Builder
public class DecompteSortieResponseDTO {
    private Integer idDecompte;
    private Integer idContratLocation;
    private LocalDateTime dateSortie;
    private BigDecimal montantAvanceReference;
    private BigDecimal montantCautionReference;
    private BigDecimal montantArrieres;
    private BigDecimal coutReparation;
    private BigDecimal montantDeduitAvance;
    private BigDecimal montantDeduitCaution;
    private BigDecimal montantManquant;
    private BigDecimal montantARembourser;
    private StatutDecompteSortie statut;
}