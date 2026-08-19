package com.immobilier.gestionImmobiliere.modules.biens.dto.responses;

import com.immobilier.gestionImmobiliere.donnees.biens.model.StatutLocationBienService;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data @Builder
public class LocationBienServiceResponseDTO {
    private Integer idLocationBienService;
    private Integer idClient;
    private String nomClient;
    private Integer idBienService;
    private String libelleBienService;
    private Integer idPaiement;
    private String destination;
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;
    private Integer duree;
    private Double montantTotal;
    private StatutLocationBienService statut;
    private List<PaiementLocationBienServiceResponseDTO> historiquePaiements;

}