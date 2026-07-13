package com.immobilier.gestionImmobiliere.modules.annonces.dto.responses;

import com.immobilier.gestionImmobiliere.donnees.annonces.model.StatutContact;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data @Builder
public class ContactResponseDTO {
    private Integer idContact;
    private String nomComplet;
    private String email;
    private String telephone;
    private String sujet;
    private String message;
    private LocalDateTime dateEnvoi;
    private StatutContact statut;
}