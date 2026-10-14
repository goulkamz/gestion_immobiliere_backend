package com.immobilier.gestionImmobiliere.modules.biens.dto.requests;

import com.immobilier.gestionImmobiliere.donnees.biens.model.StatutLocationBienService;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateStatutLocationBienServiceDTO {
    @NotNull private StatutLocationBienService statut;
    /** Clôture anticipée uniquement : true = ramener la fin à aujourd'hui (trop-perçu à rembourser), sinon durée inchangée. */
    private Boolean raccourcir;
}