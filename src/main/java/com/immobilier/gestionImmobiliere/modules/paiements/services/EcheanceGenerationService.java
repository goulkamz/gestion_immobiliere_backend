package com.immobilier.gestionImmobiliere.modules.paiements.services;

import com.immobilier.gestionImmobiliere.donnees.biens.repository.MaisonRepository;
import com.immobilier.gestionImmobiliere.donnees.contrats.model.ContratLocation;
import com.immobilier.gestionImmobiliere.donnees.contrats.model.ContratMandat;
import com.immobilier.gestionImmobiliere.donnees.contrats.model.StatutLocation;
import com.immobilier.gestionImmobiliere.donnees.contrats.model.StatutMandat;
import com.immobilier.gestionImmobiliere.donnees.contrats.repository.ContratLocationRepository;
import com.immobilier.gestionImmobiliere.donnees.contrats.repository.ContratMandatRepository;
import com.immobilier.gestionImmobiliere.donnees.paiements.model.EcheanceLoyer;
import com.immobilier.gestionImmobiliere.donnees.paiements.model.StatutEcheance;
import com.immobilier.gestionImmobiliere.donnees.paiements.model.TypeEcheance;
import com.immobilier.gestionImmobiliere.donnees.paiements.repository.EcheanceLoyerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.chrono.ChronoLocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class EcheanceGenerationService {

    private static final int HORIZON_MOIS_SI_SANS_FIN = 12;

    private final EcheanceLoyerRepository echeanceRepository;
    private final ContratLocationRepository contratLocationRepository;

    public EcheanceGenerationService(EcheanceLoyerRepository echeanceRepository,
                                     ContratLocationRepository contratLocationRepository) {
        this.echeanceRepository = echeanceRepository;
        this.contratLocationRepository = contratLocationRepository;
    }

    /**
     * RG5 — échéances de loyer générées mensuellement, plafonnées au 31 décembre
     * de l'année en cours (ou à dateSortie si elle tombe avant).
     * Appelée à la création/activation d'un contrat de location.
     */
    @Transactional
    public void genererEcheancesLocation(ContratLocation location) {
        LocalDate debut = LocalDate.from(location.getDateEntree());
        LocalDate finAnnee = LocalDate.of(debut.getYear(), 12, 31);
        LocalDate finCalcul = location.getDateSortie() != null && LocalDate.from(location.getDateSortie()).isBefore(finAnnee)
                ? LocalDate.from(location.getDateSortie())
                : finAnnee;

        genererMoisManquants(location.getIdContratLocation(), debut, finCalcul, location.getMontantLoyer());
    }

    /**
     * Job annuel — à brancher sur un @Scheduled déclenché le 31 décembre (ou le 1er janvier).
     * Parcourt tous les contrats de location ACTIF et régénère leurs échéances
     * pour la nouvelle année en cours (janvier -> décembre, ou jusqu'à dateSortie si antérieure).
     */
    @Transactional
    public int regenererEcheancesAnnuellesLocations() {
        List<ContratLocation> actives = contratLocationRepository.findByStatut(StatutLocation.ACTIF);
        int compteur = 0;
        LocalDate anneeEnCours = LocalDate.now();
        LocalDate debutAnnee = LocalDate.of(anneeEnCours.getYear(), 1, 1);
        LocalDate finAnnee = LocalDate.of(anneeEnCours.getYear(), 12, 31);

        for (ContratLocation location : actives) {
            LocalDate debut = LocalDate.from(location.getDateEntree()).isAfter(debutAnnee)
                    ? LocalDate.from(location.getDateEntree())
                    : debutAnnee;
            LocalDate fin = location.getDateSortie() != null && LocalDate.from(location.getDateSortie()).isBefore(finAnnee)
                    ? LocalDate.from(location.getDateSortie())
                    : finAnnee;

            if (!fin.isBefore(debut)) {
                compteur += genererMoisManquants(location.getIdContratLocation(), debut, fin, location.getMontantLoyer());
            }
        }
        return compteur;
    }

    /**
     * Génère une échéance par mois entre debut et fin (inclus), en sautant les mois
     * pour lesquels une échéance existe déjà pour cette entité (évite les doublons
     * lors de la régénération annuelle).
     */
    // Au lieu de : courante = debut.plusMonths(1), incrémenté par mois
    // -> dateEcheance = toujours le 1er du mois suivant le mois d'occupation

    private int genererMoisManquants(Integer entiteId, LocalDate debutOccupation, LocalDate fin, BigDecimal montant) {
        LocalDate moisOccupe = debutOccupation.withDayOfMonth(1); // normalise au 1er du mois d'entrée
        LocalDate finNormalisee = fin.withDayOfMonth(fin.lengthOfMonth());

        Set<LocalDate> moisExistants = echeanceRepository
                .findByEntiteEcheanceTypeAndEntiteEcheanceIdAndDateEcheanceBetween(
                        TypeEcheance.LOCATION, entiteId, moisOccupe.plusMonths(1), finNormalisee.plusMonths(1))
                .stream().map(EcheanceLoyer::getDateEcheance).collect(Collectors.toSet());

        List<EcheanceLoyer> aCreer = new ArrayList<>();
        LocalDate courant = moisOccupe;
        while (!courant.isAfter(finNormalisee)) {
            LocalDate dateEcheance = courant.plusMonths(1).withDayOfMonth(1); // toujours le 1er du mois suivant
            boolean existeDeja = moisExistants.contains(dateEcheance);
            if (!existeDeja) {
                aCreer.add(EcheanceLoyer.builder()
                        .entiteEcheanceType(TypeEcheance.LOCATION)
                        .entiteEcheanceId(entiteId)
                        .dateEcheance(dateEcheance)
                        .montantDu(montant)
                        .montantPaye(BigDecimal.ZERO)
                        .commissionDeduite(BigDecimal.ZERO)
                        .statut(StatutEcheance.EN_ATTENTE)
                        .build());
            }
            courant = courant.plusMonths(1);
        }
        echeanceRepository.saveAll(aCreer);
        return aCreer.size();
    }
}