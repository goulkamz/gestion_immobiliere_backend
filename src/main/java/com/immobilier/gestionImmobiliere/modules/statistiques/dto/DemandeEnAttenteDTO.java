package com.immobilier.gestionImmobiliere.modules.statistiques.dto;

import com.immobilier.gestionImmobiliere.donnees.biens.model.StatutLocationBienService;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data @Builder
public class DemandeEnAttenteDTO {
    private Integer idLocation;
    private String libelleBien;
    private String nomClient;
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;
    private Double montantEstime;
    private StatutLocationBienService statut;
}