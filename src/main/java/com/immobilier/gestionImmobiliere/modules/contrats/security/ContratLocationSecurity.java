package com.immobilier.gestionImmobiliere.modules.contrats.security;

import com.immobilier.gestionImmobiliere.donnees.biens.repository.MaisonRepository;
import org.springframework.stereotype.Component;

@Component("contratLocationSecurity")
public class ContratLocationSecurity {

    private final MaisonRepository maisonRepository;

    public ContratLocationSecurity(MaisonRepository maisonRepository) {
        this.maisonRepository = maisonRepository;
    }

    /**
     * Vérifie que l'utilisateur (bailleur) est bien le propriétaire
     * de la cour rattachée à la maison concernée par le contrat.
     */
    public boolean isProprietaire(Integer idMaison, Integer idUser) {
        if (idMaison == null || idUser == null) return false;
        return maisonRepository.findById(idMaison)
                .map(m -> m.getCour().getProprietaire().getIdUser().equals(idUser))
                .orElse(false);
    }
}