package com.immobilier.gestionImmobiliere.modules.contrats.security;

import com.immobilier.gestionImmobiliere.donnees.biens.repository.MaisonRepository;
import com.immobilier.gestionImmobiliere.donnees.contrats.model.ContratLocation;
import com.immobilier.gestionImmobiliere.donnees.contrats.repository.ContratLocationRepository;
import org.springframework.stereotype.Component;

@Component("contratLocationSecurity")
public class ContratLocationSecurity {

    private final MaisonRepository maisonRepository;
    private final ContratLocationRepository contratLocationRepository;

    public ContratLocationSecurity(MaisonRepository maisonRepository, ContratLocationRepository contratLocationRepository) {
        this.maisonRepository = maisonRepository;
        this.contratLocationRepository = contratLocationRepository;
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

    /**
     * Vérification directe par idContrat (pas via returnObject) — pour les
     * méthodes avec effet de bord où l'autorisation doit se faire AVANT exécution.
     * Locataire du contrat OU propriétaire de la maison concernée.
     */
    public boolean isAccessible(Integer idContrat, Integer idUser) {
        ContratLocation contrat = contratLocationRepository.findById(idContrat).orElse(null);
        if (contrat == null) return false;

        if (contrat.getLocataire().getIdUser().equals(idUser)) return true;

        return contrat.getMaison().getCour().getProprietaire().getIdUser().equals(idUser);
    }
}