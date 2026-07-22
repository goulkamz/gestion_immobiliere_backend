package com.immobilier.gestionImmobiliere.modules.paiements.services;

import com.immobilier.gestionImmobiliere.donnees.contrats.model.ContratLocation;
import com.immobilier.gestionImmobiliere.donnees.contrats.model.ContratMandat;
import com.immobilier.gestionImmobiliere.donnees.contrats.repository.ContratLocationRepository;
import com.immobilier.gestionImmobiliere.donnees.contrats.repository.ContratMandatRepository;
import com.immobilier.gestionImmobiliere.donnees.paiements.model.EcheanceLoyer;
import com.immobilier.gestionImmobiliere.donnees.paiements.model.TypeEcheance;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EcheanceOwnershipResolver {

    private final ContratMandatRepository contratMandatRepository;
    private final ContratLocationRepository contraLocationRepository;

    // Retourne l'id_user du bailleur si MANDAT, du locataire si LOCATION
    public Integer resolveProprietaireOuLocataireId(EcheanceLoyer echeance) {
        if (echeance.getEntiteEcheanceType() == TypeEcheance.MANDAT) {
            ContratMandat mandat = contratMandatRepository.findById(echeance.getEntiteEcheanceId())
                    .orElseThrow(() -> new EntityNotFoundException("Mandat introuvable"));
            return mandat.getAgent().getIdUser();
        } else {
            ContratLocation location = contraLocationRepository.findById(echeance.getEntiteEcheanceId())
                    .orElseThrow(() -> new EntityNotFoundException("Location introuvable"));
            return location.getLocataire().getIdUser();
        }
    }
}