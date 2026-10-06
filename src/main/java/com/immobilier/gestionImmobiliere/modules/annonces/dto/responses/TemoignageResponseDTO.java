package com.immobilier.gestionImmobiliere.modules.annonces.dto.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.immobilier.gestionImmobiliere.donnees.annonces.model.RoleTemoignage;
import com.immobilier.gestionImmobiliere.donnees.annonces.model.StatutTemoignage;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

// statut n'est renseigne que dans les reponses admin ; null (donc absent) cote public
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
    private StatutTemoignage statut;
}
