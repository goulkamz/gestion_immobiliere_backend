package com.immobilier.gestionImmobiliere.modules.paiements.services;

import com.immobilier.gestionImmobiliere.donnees.biens.repository.LocationBienServiceRepository;
import com.immobilier.gestionImmobiliere.donnees.contrats.model.ContratLocation;
import com.immobilier.gestionImmobiliere.donnees.contrats.model.ContratMandat;
import com.immobilier.gestionImmobiliere.donnees.contrats.repository.ContratLocationRepository;
import com.immobilier.gestionImmobiliere.donnees.contrats.repository.ContratMandatRepository;
import com.immobilier.gestionImmobiliere.donnees.paiements.model.EcheanceLoyer;
import com.immobilier.gestionImmobiliere.donnees.paiements.model.TypeEcheance;
import com.immobilier.gestionImmobiliere.donnees.paiements.repository.EcheanceLoyerRepository;
import com.immobilier.gestionImmobiliere.donnees.paiements.repository.PaiementEcheanceRepository;
import com.immobilier.gestionImmobiliere.donnees.paiements.repository.PaiementLocationBienServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("paiementOwnershipResolver")
@RequiredArgsConstructor
public class PaiementOwnershipResolver {

    private final PaiementEcheanceRepository paiementEcheanceRepository;
    private final EcheanceLoyerRepository echeanceRepository;
    private final ContratMandatRepository contratMandatRepository;
    private final ContratLocationRepository contratLocationRepository;
    private final PaiementLocationBienServiceRepository paiementLocationBienServiceRepository;
    private final LocationBienServiceRepository locationBienServiceRepository;

    // isBailleur=true -> vérifie côté bailleur (mandat ou location sur ses biens)
    // isBailleur=false -> vérifie côté client (locataire de la location)
    public boolean isPaiementAccessible(Integer idPaiement, Integer currentUserId, boolean isBailleur) {
        List<Integer> idEcheances = paiementEcheanceRepository.findByIdPaiement(idPaiement).stream()
                .map(pe -> pe.getIdEcheance()).toList();

        if (idEcheances.isEmpty()) return false;

        List<EcheanceLoyer> echeances = echeanceRepository.findByIdEcheanceIn(idEcheances);

        for (EcheanceLoyer e : echeances) {
            if (e.getEntiteEcheanceType() == TypeEcheance.MANDAT) {
                if (!isBailleur) continue; // un client n'est jamais concerné par une échéance de MANDAT
                ContratMandat mandat = contratMandatRepository.findById(e.getEntiteEcheanceId()).orElse(null);
                if (mandat != null && mandat.getCour().getProprietaire().getIdUser().equals(currentUserId)) {
                    return true;
                }
            } else {
                ContratLocation location = contratLocationRepository.findById(e.getEntiteEcheanceId()).orElse(null);
                if (location == null) continue;
                boolean match = isBailleur
                        ? location.getMaison().getCour().getProprietaire().getIdUser().equals(currentUserId)
                        : location.getLocataire().getIdUser().equals(currentUserId);
                if (match) return true;
            }
        }
        return false;
    }

    /**
     * Vérifie l'accès côté biens/services : le client est celui qui a
     * initié la location associée à ce paiement (INITIAL ou PROLONGATION).
     */
    public boolean isPaiementAccessibleBienService(Integer idPaiement, Integer currentUserId) {
        return paiementLocationBienServiceRepository.findByIdPaiement(idPaiement).stream()
                .anyMatch(pl -> {
                    var location = locationBienServiceRepository.findById(pl.getIdLocationBienService()).orElse(null);
                    return location != null && location.getClient().getIdUser().equals(currentUserId);
                });
    }
}