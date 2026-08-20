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

@Service
public class EcheanceGenerationService {

    private static final int HORIZON_MOIS_SI_SANS_FIN = 12;

    private final EcheanceLoyerRepository echeanceRepository;
    private final MaisonRepository maisonRepository;
    private final ContratLocationRepository contratLocationRepository;
    private final ContratMandatRepository contratMandatRepository;

    public EcheanceGenerationService(EcheanceLoyerRepository echeanceRepository, MaisonRepository maisonRepository, ContratLocationRepository contratLocationRepository, ContratMandatRepository contratMandatRepository) {
        this.echeanceRepository = echeanceRepository;
        this.maisonRepository = maisonRepository;
        this.contratLocationRepository = contratLocationRepository;
        this.contratMandatRepository = contratMandatRepository;
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

        genererMoisManquants(TypeEcheance.LOCATION, location.getIdContratLocation(), debut, finCalcul, location.getMontantLoyer());
    }

    /**
     * Génère les échéances de commission d'un mandat, plafonnées au 31 décembre
     * de l'année en cours (ou à dateFin si elle tombe avant).
     */
    @Transactional
    public void genererEcheancesMandat(ContratMandat mandat) {
        LocalDate debut = LocalDate.from(mandat.getDateDebut());
        LocalDate finAnnee = LocalDate.of(debut.getYear(), 12, 31);
        LocalDate finCalcul = mandat.getDateFin() != null && LocalDate.from(mandat.getDateFin()).isBefore(finAnnee)
                ? LocalDate.from(mandat.getDateFin())
                : finAnnee;

        double montantEcheance = calculerMontantCommission(mandat);
        genererMoisManquants(TypeEcheance.MANDAT, mandat.getIdMandat(), debut, finCalcul, montantEcheance);
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
                compteur += genererMoisManquants(TypeEcheance.LOCATION, location.getIdContratLocation(), debut, fin, location.getMontantLoyer());
            }
        }
        return compteur;
    }

    /**
     * Job annuel — équivalent pour les mandats ACTIF.
     */
    @Transactional
    public int regenererEcheancesAnnuellesMandats() {
        List<ContratMandat> actifs = contratMandatRepository.findByStatut(StatutMandat.ACTIF);
        int compteur = 0;
        LocalDate anneeEnCours = LocalDate.now();
        LocalDate debutAnnee = LocalDate.of(anneeEnCours.getYear(), 1, 1);
        LocalDate finAnnee = LocalDate.of(anneeEnCours.getYear(), 12, 31);

        for (ContratMandat mandat : actifs) {
            LocalDate debut = LocalDate.from(mandat.getDateDebut()).isAfter(debutAnnee)
                    ? LocalDate.from(mandat.getDateDebut())
                    : debutAnnee;
            LocalDate fin = mandat.getDateFin() != null && LocalDate.from(mandat.getDateFin()).isBefore(finAnnee)
                    ? LocalDate.from(mandat.getDateFin())
                    : finAnnee;

            if (!fin.isBefore(debut)) {
                double montantEcheance = calculerMontantCommission(mandat);
                compteur += genererMoisManquants(TypeEcheance.MANDAT, mandat.getIdMandat(), debut, fin, montantEcheance);
            }
        }
        return compteur;
    }

    /**
     * Génère une échéance par mois entre debut et fin (inclus), en sautant les mois
     * pour lesquels une échéance existe déjà pour cette entité (évite les doublons
     * lors de la régénération annuelle).
     */
    private int genererMoisManquants(TypeEcheance type, Integer entiteId, LocalDate debut, LocalDate fin, Double montant) {
        List<LocalDate> moisExistants = echeanceRepository.findByEntiteEcheanceTypeAndEntiteEcheanceIdAndDateEcheanceBetween(type, entiteId,debut,fin)
                .stream().map(EcheanceLoyer::getDateEcheance).toList();

        List<EcheanceLoyer> aCreer = new ArrayList<>();
        LocalDate courante = debut;
        while (!courante.isAfter(fin)) {
            LocalDate finalCourante = courante;
            boolean existeDeja = moisExistants.stream()
                    .anyMatch(d -> d.getYear() == finalCourante.getYear() && d.getMonth() == finalCourante.getMonth());
            if (!existeDeja) {
                aCreer.add(EcheanceLoyer.builder()
                        .entiteEcheanceType(type)
                        .entiteEcheanceId(entiteId)
                        .dateEcheance(courante)
                        .montantDu(montant)
                        .montantPaye(0.0)
                        .statut(StatutEcheance.EN_ATTENTE)
                        .build());
            }
            courante = courante.plusMonths(1);
        }
        echeanceRepository.saveAll(aCreer);
        return aCreer.size();
    }

    private double calculerMontantCommission(ContratMandat mandat) {
        Double totalLoyer = maisonRepository.sumLoyerMaisonsLoueesByCour(mandat.getCour().getIdCour());
        BigDecimal pourcentage = mandat.getCommission() != null ? mandat.getCommission() : BigDecimal.ZERO;
        return BigDecimal.valueOf(totalLoyer != null ? totalLoyer : 0)
                .multiply(pourcentage)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                .doubleValue();
    }
//
//    /**
//     * RG5 — échéances de loyer générées mensuellement entre date_entree et date_sortie.
//     * Si date_sortie est nulle, génère un horizon de 12 mois par défaut.
//     */
//    @Transactional
//    public void genererEcheancesLocation(ContratLocation location) {
//        LocalDate debut = LocalDate.from(location.getDateEntree());
//        LocalDateTime fin = location.getDateSortie() != null
//                ? location.getDateSortie()
//                : debut.plusMonths(HORIZON_MOIS_SI_SANS_FIN).atStartOfDay();
//
//        List<EcheanceLoyer> echeances = new ArrayList<>();
//        LocalDate courante = debut;
//        while (!courante.isAfter(ChronoLocalDate.from(fin))) {
//            echeances.add(EcheanceLoyer.builder()
//                    .entiteEcheanceType(TypeEcheance.LOCATION)
//                    .entiteEcheanceId(location.getIdContratLocation())
//                    .dateEcheance(courante)
//                    .montantDu(location.getMontantLoyer())
//                    .montantPaye(0.0)
//                    .statut(StatutEcheance.EN_ATTENTE)
//                    .build());
//            courante = courante.plusMonths(1);
//        }
//        echeanceRepository.saveAll(echeances);
//    }


//    /**
//     * Génère les échéances de commission d'un mandat, sur la même logique mensuelle.
//     *
//     * ⚠️ HYPOTHÈSE À VALIDER : le champ `commission` de ContratMandat est traité ici
//     * comme un montant fixe par échéance. S'il s'agit d'un pourcentage appliqué aux
//     * loyers perçus sur la cour, le calcul doit agréger les loyers des maisons de la
//     * cour sur la période — logique non présente dans le cahier des charges actuel.
//     */
//    @Transactional
//    public void genererEcheancesMandat(ContratMandat mandat) {
//        LocalDate debut = LocalDate.from(mandat.getDateDebut());
//        LocalDateTime fin = mandat.getDateFin() != null
//                ? mandat.getDateFin()
//                : debut.plusMonths(HORIZON_MOIS_SI_SANS_FIN).atStartOfDay();
//
//        Double totalLoyer = maisonRepository.sumLoyerMaisonsLoueesByCour(mandat.getCour().getIdCour());
//        BigDecimal pourcentage = mandat.getCommission() != null ? mandat.getCommission() : BigDecimal.ZERO;
//
//        double montantEcheance = BigDecimal.valueOf(totalLoyer)
//                .multiply(pourcentage)
//                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
//                .doubleValue();
//
//        List<EcheanceLoyer> echeances = new ArrayList<>();
//        LocalDate courante = debut;
//        while (!courante.isAfter(ChronoLocalDate.from(fin))) {
//            echeances.add(EcheanceLoyer.builder()
//                    .entiteEcheanceType(TypeEcheance.MANDAT)
//                    .entiteEcheanceId(mandat.getIdMandat())
//                    .dateEcheance(courante)
//                    .montantDu(montantEcheance)
//                    .montantPaye(0.0)
//                    .statut(StatutEcheance.EN_ATTENTE)
//                    .build());
//            courante = courante.plusMonths(1);
//        }
//        echeanceRepository.saveAll(echeances);
//    }
}