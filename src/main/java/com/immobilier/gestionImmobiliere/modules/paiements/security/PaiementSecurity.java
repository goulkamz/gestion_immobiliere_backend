package com.immobilier.gestionImmobiliere.modules.paiements.security;

import com.immobilier.gestionImmobiliere.donnees.biens.model.LocationBienService;
import com.immobilier.gestionImmobiliere.donnees.biens.repository.LocationBienServiceRepository;
import com.immobilier.gestionImmobiliere.donnees.paiements.model.EcheanceLoyer;
import com.immobilier.gestionImmobiliere.donnees.paiements.model.PaiementEcheance;
import com.immobilier.gestionImmobiliere.donnees.paiements.model.PaiementLocationBienService;
import com.immobilier.gestionImmobiliere.donnees.paiements.model.TypeEcheance;
import com.immobilier.gestionImmobiliere.donnees.paiements.repository.EcheanceLoyerRepository;
import com.immobilier.gestionImmobiliere.donnees.paiements.repository.PaiementEcheanceRepository;
import com.immobilier.gestionImmobiliere.donnees.paiements.repository.PaiementLocationBienServiceRepository;
import com.immobilier.gestionImmobiliere.modules.contrats.security.ContratLocationSecurity;
import com.immobilier.gestionImmobiliere.modules.contrats.security.ContratMandatSecurity;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("paiementSecurity")
public class PaiementSecurity {

    private final PaiementEcheanceRepository paiementEcheanceRepository;
    private final EcheanceLoyerRepository echeanceLoyerRepository;
    private final PaiementLocationBienServiceRepository paiementLocationBienServiceRepository;
    private final LocationBienServiceRepository locationBienServiceRepository;
    private final ContratLocationSecurity contratLocationSecurity;
    private final ContratMandatSecurity contratMandatSecurity;

    public PaiementSecurity(PaiementEcheanceRepository paiementEcheanceRepository,
                             EcheanceLoyerRepository echeanceLoyerRepository,
                             PaiementLocationBienServiceRepository paiementLocationBienServiceRepository,
                             LocationBienServiceRepository locationBienServiceRepository,
                             ContratLocationSecurity contratLocationSecurity,
                             ContratMandatSecurity contratMandatSecurity) {
        this.paiementEcheanceRepository = paiementEcheanceRepository;
        this.echeanceLoyerRepository = echeanceLoyerRepository;
        this.paiementLocationBienServiceRepository = paiementLocationBienServiceRepository;
        this.locationBienServiceRepository = locationBienServiceRepository;
        this.contratLocationSecurity = contratLocationSecurity;
        this.contratMandatSecurity = contratMandatSecurity;
    }

    /**
     * Un paiement est lié soit à une échéance (loyer ou commission de mandat),
     * soit à une location de bien/service — jamais les deux. L'accès remonte
     * jusqu'à l'entité concernée pour vérifier la propriété.
     */
    public boolean isAccessible(Integer idPaiement, Integer idUser) {
        List<PaiementEcheance> liensEcheance = paiementEcheanceRepository.findByIdPaiement(idPaiement);
        if (!liensEcheance.isEmpty()) {
            EcheanceLoyer echeance = echeanceLoyerRepository.findById(liensEcheance.get(0).getIdEcheance()).orElse(null);
            if (echeance == null) return false;

            if (echeance.getEntiteEcheanceType() == TypeEcheance.LOCATION) {
                return contratLocationSecurity.isAccessible(echeance.getEntiteEcheanceId(), idUser);
            }
            if (echeance.getEntiteEcheanceType() == TypeEcheance.MANDAT) {
                return contratMandatSecurity.isAccessible(echeance.getEntiteEcheanceId(), idUser);
            }
            return false;
        }

        List<PaiementLocationBienService> liensBienService = paiementLocationBienServiceRepository.findByIdPaiement(idPaiement);
        if (!liensBienService.isEmpty()) {
            LocationBienService location = locationBienServiceRepository.findById(liensBienService.get(0).getIdLocationBienService()).orElse(null);
            if (location == null) return false;
            return location.getClient().getIdUser().equals(idUser);
        }

        return false;
    }
}
