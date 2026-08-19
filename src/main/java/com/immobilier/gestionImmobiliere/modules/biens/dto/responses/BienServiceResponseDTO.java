package com.immobilier.gestionImmobiliere.modules.biens.dto.responses;

import com.immobilier.gestionImmobiliere.donnees.biens.model.StatutBienService;
import lombok.Builder;
import lombok.Data;

@Data @Builder
public class BienServiceResponseDTO {
    private Integer idBienService;
    private String libelle;
    private String description;
    private Double prixJournalier;
    private Double prixMensuel;
    private StatutBienService disponibilite;
    private Integer idSecteur;
    private String nomSecteur;
    private Integer idCategorie;
    private String libelleCategorie;
    private Integer idGestionnaire;
    private String nomGestionnaire;
}