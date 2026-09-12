package com.immobilier.gestionImmobiliere.modules.biens.dto.responses;

import com.immobilier.gestionImmobiliere.donnees.paiements.model.TypePaiementLocationBienService;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @Builder
public class PaiementLocationBienServiceResponseDTO {
    private Integer idPaiement;
    private BigDecimal montant;
    private String modePaiement;
    private TypePaiementLocationBienService typePaiement;
    private LocalDateTime datePaiement;
}