package com.immobilier.gestionImmobiliere.modules.biens.dto.responses;

import com.immobilier.gestionImmobiliere.donnees.biens.model.StatutLocationBienService;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data @Builder
public class LocationBienServiceResponseDTO {
    private Integer idLocationBienService;
    private Integer idClient;
    private String nomClient;
    private Integer idBienService;
    private String libelleBienService;
    private String destination;
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;
    private Long duree;
    private BigDecimal montantTotal;
    private StatutLocationBienService statut;
    private List<PaiementLocationBienServiceResponseDTO> historiquePaiements;

}