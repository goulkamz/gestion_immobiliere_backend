package com.immobilier.gestionImmobiliere.modules.paiements.security;

import com.immobilier.gestionImmobiliere.donnees.contrats.model.ContratLocation;
import com.immobilier.gestionImmobiliere.donnees.contrats.repository.ContratLocationRepository;
import com.immobilier.gestionImmobiliere.donnees.paiements.model.EcheanceLoyer;
import com.immobilier.gestionImmobiliere.donnees.paiements.model.TypeEcheance;
import com.immobilier.gestionImmobiliere.donnees.paiements.repository.EcheanceLoyerRepository;
import org.springframework.stereotype.Component;

@Component("quittanceLoyerSecurity")
public class QuittanceLoyerSecurity {

    private final EcheanceLoyerRepository echeanceLoyerRepository;
    private final ContratLocationRepository contratLocationRepository;

    public QuittanceLoyerSecurity(EcheanceLoyerRepository echeanceLoyerRepository,
                                  ContratLocationRepository contratLocationRepository) {
        this.echeanceLoyerRepository = echeanceLoyerRepository;
        this.contratLocationRepository = contratLocationRepository;
    }

    public boolean isAccessible(Integer idEcheance, Integer idUser) {
        EcheanceLoyer echeance = echeanceLoyerRepository.findById(idEcheance).orElse(null);
        if (echeance == null || echeance.getEntiteEcheanceType() != TypeEcheance.LOCATION) return false;

        ContratLocation contrat = contratLocationRepository.findById(echeance.getEntiteEcheanceId()).orElse(null);
        if (contrat == null) return false;

        if (contrat.getLocataire().getIdUser().equals(idUser)) return true;

        return contrat.getMaison().getCour().getProprietaire().getIdUser().equals(idUser);
    }
}