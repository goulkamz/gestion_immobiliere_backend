package com.immobilier.gestionImmobiliere.modules.annonces.dto.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.immobilier.gestionImmobiliere.donnees.annonces.model.RoleTemoignage;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

// Reponse publique : flagActif n'est volontairement pas expose
@Data @Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TemoignageResponseDTO {
    private Integer idTemoignage;
    private String nomAuteur;
    private RoleTemoignage role;
    private String texte;
    private Short note;
    private String photoUrl;
    private LocalDate date;
}
