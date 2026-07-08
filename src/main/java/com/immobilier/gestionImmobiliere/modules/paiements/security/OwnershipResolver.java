package com.immobilier.gestionImmobiliere.modules.paiements.security;

import com.immobilier.gestionImmobiliere.donnees.contrats.repository.ContratLocationRepository;
import com.immobilier.gestionImmobiliere.donnees.contrats.repository.ContratMandatRepository;
import com.immobilier.gestionImmobiliere.donnees.paiements.model.TypeEcheance;
import com.immobilier.gestionImmobiliere.donnees.paiements.repository.EcheanceLoyerRepository;
import com.immobilier.gestionImmobiliere.donnees.paiements.repository.PaiementEcheanceRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("ownershipResolver")
public class OwnershipResolver {

    private final ContratLocationRepository locationRepository;
    private final ContratMandatRepository mandatRepository;
    private final EcheanceLoyerRepository echeanceRepository;
    private final PaiementEcheanceRepository paiementEcheanceRepository;

    public OwnershipResolver(ContratLocationRepository locationRepository, ContratMandatRepository mandatRepository,
                             EcheanceLoyerRepository echeanceRepository, PaiementEcheanceRepository paiementEcheanceRepository) {
        this.locationRepository = locationRepository;
        this.mandatRepository = mandatRepository;
        this.echeanceRepository = echeanceRepository;
        this.paiementEcheanceRepository = paiementEcheanceRepository;
    }

    public boolean isEcheanceOwnedByClient(TypeEcheance type, Integer entiteId, Integer userId) {
        if (type != TypeEcheance.LOCATION) return false;
        return locationRepository.findById(entiteId)
                .map(cl -> cl.getLocataire().getIdUser().equals(userId))
                .orElse(false);
    }

    public boolean isEcheanceOwnedByBailleur(TypeEcheance type, Integer entiteId, Integer userId) {
        if (type == TypeEcheance.LOCATION) {
            return locationRepository.findById(entiteId)
                    .map(cl -> cl.getMaison().getCour().getProprietaire().getIdUser().equals(userId))
                    .orElse(false);
        }
        return mandatRepository.findById(entiteId)
                .map(m -> m.getCour().getProprietaire().getIdUser().equals(userId))
                .orElse(false);
    }

    /**
     * Un paiement est accessible si AU MOINS UNE des échéances qu'il couvre
     * appartient à l'utilisateur (client ou bailleur).
     */
    public boolean isPaiementAccessible(Integer idPaiement, Integer userId, boolean isBailleur) {
        List<Integer> idEcheances = paiementEcheanceRepository.findByIdPaiement(idPaiement).stream()
                .map(pe -> pe.getIdEcheance()).toList();

        return echeanceRepository.findByIdEcheanceIn(idEcheances).stream()
                .anyMatch(e -> isBailleur
                        ? isEcheanceOwnedByBailleur(e.getEntiteEcheanceType(), e.getEntiteEcheanceId(), userId)
                        : isEcheanceOwnedByClient(e.getEntiteEcheanceType(), e.getEntiteEcheanceId(), userId));
    }
}