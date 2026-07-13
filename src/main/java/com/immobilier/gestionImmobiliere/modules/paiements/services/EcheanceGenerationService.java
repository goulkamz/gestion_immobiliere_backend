package com.immobilier.gestionImmobiliere.modules.paiements.services;

import com.immobilier.gestionImmobiliere.donnees.biens.repository.MaisonRepository;
import com.immobilier.gestionImmobiliere.donnees.contrats.model.ContratLocation;
import com.immobilier.gestionImmobiliere.donnees.contrats.model.ContratMandat;
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

    public EcheanceGenerationService(EcheanceLoyerRepository echeanceRepository, MaisonRepository maisonRepository) {
        this.echeanceRepository = echeanceRepository;
        this.maisonRepository = maisonRepository;
    }

    /**
     * RG5 — échéances de loyer générées mensuellement entre date_entree et date_sortie.
     * Si date_sortie est nulle, génère un horizon de 12 mois par défaut.
     */
    @Transactional
    public void genererEcheancesLocation(ContratLocation location) {
        LocalDate debut = LocalDate.from(location.getDateEntree());
        LocalDateTime fin = location.getDateSortie() != null
                ? location.getDateSortie()
                : debut.plusMonths(HORIZON_MOIS_SI_SANS_FIN).atStartOfDay();

        List<EcheanceLoyer> echeances = new ArrayList<>();
        LocalDate courante = debut;
        while (!courante.isAfter(ChronoLocalDate.from(fin))) {
            echeances.add(EcheanceLoyer.builder()
                    .entiteEcheanceType(TypeEcheance.LOCATION)
                    .entiteEcheanceId(location.getIdContratLocation())
                    .dateEcheance(courante)
                    .montantDu(location.getMontantLoyer())
                    .montantPaye(0.0)
                    .statut(StatutEcheance.EN_ATTENTE)
                    .dateCreate(LocalDateTime.now())
                    .build());
            courante = courante.plusMonths(1);
        }
        echeanceRepository.saveAll(echeances);
    }

    /**
     * Génère les échéances de commission d'un mandat, sur la même logique mensuelle.
     *
     * ⚠️ HYPOTHÈSE À VALIDER : le champ `commission` de ContratMandat est traité ici
     * comme un montant fixe par échéance. S'il s'agit d'un pourcentage appliqué aux
     * loyers perçus sur la cour, le calcul doit agréger les loyers des maisons de la
     * cour sur la période — logique non présente dans le cahier des charges actuel.
     */
    @Transactional
    public void genererEcheancesMandat(ContratMandat mandat) {
        LocalDate debut = LocalDate.from(mandat.getDateDebut());
        LocalDateTime fin = mandat.getDateFin() != null
                ? mandat.getDateFin()
                : debut.plusMonths(HORIZON_MOIS_SI_SANS_FIN).atStartOfDay();

        Double totalLoyer = maisonRepository.sumLoyerMaisonsLoueesByCour(mandat.getCour().getIdCour());
        BigDecimal pourcentage = mandat.getCommission() != null ? mandat.getCommission() : BigDecimal.ZERO;

        double montantEcheance = BigDecimal.valueOf(totalLoyer)
                .multiply(pourcentage)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                .doubleValue();

        List<EcheanceLoyer> echeances = new ArrayList<>();
        LocalDate courante = debut;
        while (!courante.isAfter(ChronoLocalDate.from(fin))) {
            echeances.add(EcheanceLoyer.builder()
                    .entiteEcheanceType(TypeEcheance.MANDAT)
                    .entiteEcheanceId(mandat.getIdMandat())
                    .dateEcheance(courante)
                    .montantDu(montantEcheance)
                    .montantPaye(0.0)
                    .statut(StatutEcheance.EN_ATTENTE)
                    .dateCreate(LocalDateTime.now())
                    .build());
            courante = courante.plusMonths(1);
        }
        echeanceRepository.saveAll(echeances);
    }
}