package com.immobilier.gestionImmobiliere.modules.paiements.services;

import com.immobilier.gestionImmobiliere.donnees.contrats.model.ContratLocation;
import com.immobilier.gestionImmobiliere.donnees.contrats.model.ContratMandat;
import com.immobilier.gestionImmobiliere.donnees.contrats.repository.ContratLocationRepository;
import com.immobilier.gestionImmobiliere.donnees.contrats.repository.ContratMandatRepository;
import com.immobilier.gestionImmobiliere.donnees.paiements.model.EcheanceLoyer;
import com.immobilier.gestionImmobiliere.donnees.paiements.model.SensPaiement;
import com.immobilier.gestionImmobiliere.donnees.paiements.model.StatutEcheance;
import com.immobilier.gestionImmobiliere.donnees.paiements.model.TypeEcheance;
import com.immobilier.gestionImmobiliere.donnees.paiements.repository.EcheanceLoyerRepository;
import com.immobilier.gestionImmobiliere.exceptions.ResourceNotFoundException;
import com.immobilier.gestionImmobiliere.modules.paiements.dto.requests.ConfirmerVirementDTO;
import com.immobilier.gestionImmobiliere.modules.paiements.dto.responses.EcheanceMandatResponseDTO;
import com.immobilier.gestionImmobiliere.modules.paiements.dto.responses.EcheanceResponseDTO;
import com.immobilier.gestionImmobiliere.utils.DateUtils;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static com.immobilier.gestionImmobiliere.utils.BuildSuccessResponse.buildSuccessResponse;

@Service
public class EcheanceService {

    @Value("${app.tolerance.location.jours}")
    private int TOLERANCE_LOCATION_JOURS;

    @Value("${app.tolerance.mandat.jours}")
    private int TOLERANCE_MANDAT_JOURS;

    private final EcheanceLoyerRepository echeanceRepository;
    private final ContratLocationRepository contratLocationRepository;
    private final ContratMandatRepository contratMandatRepository;
    private final PaiementService paiementService;

    public EcheanceService(EcheanceLoyerRepository echeanceRepository, ContratLocationRepository contratLocationRepository, ContratMandatRepository contratMandatRepository, PaiementService paiementService) {
        this.echeanceRepository = echeanceRepository;
        this.contratLocationRepository = contratLocationRepository;
        this.contratMandatRepository = contratMandatRepository;
        this.paiementService = paiementService;
    }

    // Remplace getAll() par une version filtrée par rôle
    public ResponseEntity<?> getAllForCurrentUser(TypeEcheance type, Integer entiteId, StatutEcheance statut,
                                                  Integer currentUserId, boolean isAdminOrAgent, boolean isBailleur,
                                                  Pageable pageable) {
        Page<EcheanceLoyer> page;

        if (isAdminOrAgent) {
            if (type != null && entiteId != null) {
                page = echeanceRepository.findByEntiteEcheanceTypeAndEntiteEcheanceId(type, entiteId, pageable);
            } else if (statut != null) {
                page = echeanceRepository.findByStatut(statut, pageable);
            } else {
                page = echeanceRepository.findAll(pageable);
            }
        } else if (isBailleur) {
            List<Integer> locationIds = contratLocationRepository.findIdsByProprietaire(currentUserId);
            List<Integer> mandatIds = contratMandatRepository.findIdsByProprietaire(currentUserId);
            page = echeanceRepository.findForBailleur(
                    locationIds.isEmpty() ? List.of(-1) : locationIds,
                    mandatIds.isEmpty() ? List.of(-1) : mandatIds,
                    pageable);
        } else {
            // CLIENT : uniquement les échéances de ses propres contrats de location
            List<Integer> locationIds = contratLocationRepository.findIdsByLocataire(currentUserId);
            page = echeanceRepository.findByEntiteEcheanceTypeAndEntiteEcheanceIdIn(
                    TypeEcheance.LOCATION, locationIds.isEmpty() ? List.of(-1) : locationIds, pageable);
        }

        return buildSuccessResponse(HttpStatus.OK, "Liste des échéances", "ECHEANCE_LIST", page.map(this::toDto));
    }


    public ResponseEntity<?> getByIdForCurrentUser(Integer id, Integer currentUserId,
                                                   boolean isAdminOrAgent, boolean isBailleur) {
        EcheanceLoyer echeance = findOrThrow(id);

        if (!isAdminOrAgent) {
            boolean autorise;
            if (echeance.getEntiteEcheanceType() == TypeEcheance.MANDAT) {
                ContratMandat mandat = contratMandatRepository.findById(echeance.getEntiteEcheanceId())
                        .orElseThrow(() -> new EntityNotFoundException("Mandat introuvable"));
                autorise = isBailleur && mandat.getCour().getProprietaire().getIdUser().equals(currentUserId);
            } else {
                ContratLocation location = contratLocationRepository.findById(echeance.getEntiteEcheanceId())
                        .orElseThrow(() -> new EntityNotFoundException("Location introuvable"));
                autorise = isBailleur
                        ? location.getMaison().getCour().getProprietaire().getIdUser().equals(currentUserId)
                        : location.getLocataire().getIdUser().equals(currentUserId); // CLIENT = locataire, ce champ-là reste "user"
            }
            if (!autorise) throw new AccessDeniedException("Vous n'avez pas accès à cette échéance");
        }

        return buildSuccessResponse(HttpStatus.OK, "Échéance trouvée", "ECHEANCE_FOUND", toDto(echeance));
    }

    /**
     * Supprime (soft delete) les échéances LOCATION des mois suivant la résiliation.
     * Le mois de la résiliation reste dû intégralement (le locataire occupait ce mois,
     * même partiellement) — seule l'échéance du mois de résiliation est CONSERVÉE.
     */
    @Transactional
    public void supprimerEcheancesFutures(Integer idContratLocation, LocalDate dateResiliation) {
        LocalDate moisResiliation = dateResiliation.withDayOfMonth(1);
        LocalDate premierMoisASupprimer = moisResiliation.plusMonths(2); // date d'échéance du mois SUIVANT celui de la résiliation

        List<EcheanceLoyer> aSupprimer = echeanceRepository
                .findByEntiteEcheanceTypeAndEntiteEcheanceIdAndDateEcheanceGreaterThanEqualAndStatutNot(
                        TypeEcheance.LOCATION, idContratLocation, premierMoisASupprimer, StatutEcheance.PAYE);

        echeanceRepository.deleteAll(aSupprimer);
    }


    public ResponseEntity<?> getEcheanceLocationEnRetard() {
        List<EcheanceLoyer> enRetard = echeanceRepository.findByEntiteEcheanceTypeAndStatutAndDateEcheanceBefore(TypeEcheance.LOCATION,StatutEcheance.EN_RETARD, LocalDate.now());
        return buildSuccessResponse(HttpStatus.OK, "Échéances en retard", "ECHEANCE_EN_RETARD_LIST",
                enRetard.stream().map(this::toDto).toList());
    }

    public ResponseEntity<?> getEcheanceMandatEnRetard() {
        List<EcheanceLoyer> enRetard = echeanceRepository.findByEntiteEcheanceTypeAndStatutAndDateEcheanceBefore(TypeEcheance.MANDAT,StatutEcheance.EN_RETARD, LocalDate.now());
        return buildSuccessResponse(HttpStatus.OK, "Échéances en retard", "ECHEANCE_EN_RETARD_LIST",
                enRetard.stream().map(this::toDto).toList());
    }

    /**
     * Job de bascule EN_ATTENTE -> EN_RETARD (à brancher sur un @Scheduled quotidien).
     */
    @Transactional
    public void marquerEcheanceLocationEnRetard() {
        LocalDate seuil = LocalDate.now().minusDays(TOLERANCE_LOCATION_JOURS);
        List<EcheanceLoyer> expirees = echeanceRepository.findByEntiteEcheanceTypeAndStatutAndDateEcheanceBefore(TypeEcheance.LOCATION,StatutEcheance.EN_ATTENTE, seuil);
        expirees.forEach(e -> e.setStatut(StatutEcheance.EN_RETARD));
        echeanceRepository.saveAll(expirees);
        //return expirees.size();
    }

    @Transactional
    public void marquerEcheanceMandatEnRetard() {
        LocalDate seuil = LocalDate.now().minusDays(TOLERANCE_MANDAT_JOURS);
        List<EcheanceLoyer> expirees =
                echeanceRepository.
                        findByEntiteEcheanceTypeAndStatutAndDateEcheanceBefore(TypeEcheance.MANDAT,StatutEcheance.EN_ATTENTE, seuil);
        expirees.forEach(e -> e.setStatut(StatutEcheance.EN_RETARD));
        echeanceRepository.saveAll(expirees);
        //return expirees.size();
    }

    // ==================================================================
    // MANDAT — calcul du reversement au bailleur
    // ==================================================================

    /**
     * À la demande de l'agent (jamais automatique) — calcule pour un mandat/mois donné :
     * - les loyers réellement encaissés ce mois (montant_paye des échéances LOCATION
     *   des maisons de la cour, pas le loyer théorique) ;
     * - la commission de l'agence, déduite selon le pourcentage du mandat ;
     * - le net à reverser au bailleur.
     * Crée une échéance MANDAT unique pour ce mandat/mois (anti-doublon).
     * montantDu = net à reverser au bailleur | commissionDeduite = gain de l'agence.
     */
    @Transactional
    public ResponseEntity<?> calculerReversementMandat(Integer idMandat, LocalDate periode, Integer currentAgentId) {
        LocalDate debutMois = periode.withDayOfMonth(1);
        LocalDate finMois = periode.withDayOfMonth(periode.lengthOfMonth());

        boolean dejaCalcule = !echeanceRepository
                .findByEntiteEcheanceTypeAndEntiteEcheanceIdAndDateEcheanceBetween(TypeEcheance.MANDAT, idMandat, debutMois, finMois)
                .isEmpty();
        if (dejaCalcule) {
            throw new IllegalStateException("Le montant a déjà été calculé pour ce mandat sur cette période");
        }

        ContratMandat mandat = contratMandatRepository.findById(idMandat)
                .orElseThrow(() -> new ResourceNotFoundException("mandat", idMandat));

        Double loyersDus = echeanceRepository.sumMontantDuLocationParCourEtMois(mandat.getCour().getIdCour(), debutMois);
        loyersDus = loyersDus != null ? loyersDus : 0.0;

        BigDecimal pourcentage = mandat.getCommission() != null ? mandat.getCommission() : BigDecimal.ZERO;
        double commission = BigDecimal.valueOf(loyersDus)
                .multiply(pourcentage)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                .doubleValue();
        double montantNetAReverser = loyersDus - commission;

        EcheanceLoyer echeance = EcheanceLoyer.builder()
                .entiteEcheanceType(TypeEcheance.MANDAT)
                .entiteEcheanceId(idMandat)
                .dateEcheance(debutMois)
                .montantDu(montantNetAReverser)
                .montantPaye(0.0)
                .commissionDeduite(commission)
                .statut(StatutEcheance.EN_ATTENTE)
                .userCreate(currentAgentId)
                .build();
        echeanceRepository.save(echeance);

        return buildSuccessResponse(HttpStatus.CREATED, "Montant à reverser calculé", "ECHEANCE_MANDAT_CALCULEE",
                toMandatDto(echeance, loyersDus));
    }

    /**
     * Confirme le virement réel au bailleur — déclenché quand l'agence le décide,
     * jamais automatique. Tout ou rien : le montant est celui de l'échéance,
     * aucun ajustement possible. Réutilise le moteur générique de PaiementService
     * (même traçabilité paiement_echeance que pour les loyers locataires).
     */
    @Transactional
    public ResponseEntity<?> confirmerVirementMandat(Integer idEcheance, ConfirmerVirementDTO dto, Integer currentAgentId) {
        EcheanceLoyer echeance = findOrThrow(idEcheance);

        if (echeance.getEntiteEcheanceType() != TypeEcheance.MANDAT) {
            throw new IllegalStateException("Cette échéance n'est pas de type MANDAT");
        }
        if (echeance.getStatut() == StatutEcheance.PAYE || echeance.getStatut() == StatutEcheance.ANNULE) {
            throw new IllegalStateException("Cette échéance ne peut etre réglée");
        }

        paiementService.soldeEcheances(
                List.of(idEcheance),
                echeance.getMontantDu(),
                dto.getModeVersement(),
                dto.getReference(),
                SensPaiement.SORTIE,
                currentAgentId,
                LocalDateTime.now());

        return buildSuccessResponse(HttpStatus.OK, "Virement confirmé", "VIREMENT_MANDAT_CONFIRME", null);
    }

    /**
     * Liste des échéances MANDAT EN_ATTENTE d'un mandat — utile pour l'agent
     * qui veut voir ce qui reste à reverser, mois par mois.
     */
    public ResponseEntity<?> getReversementsEnAttente(Integer idMandat) {
        List<EcheanceLoyer> liste = echeanceRepository
                .findByEntiteEcheanceTypeAndEntiteEcheanceIdAndStatut(TypeEcheance.MANDAT, idMandat, StatutEcheance.EN_ATTENTE);
        return buildSuccessResponse(HttpStatus.OK, "Reversements en attente", "ECHEANCES_MANDAT_EN_ATTENTE",
                liste.stream().map(e -> toMandatDto(e, null)).toList());
    }

    // ==================================================================
    // Utilitaires
    // ==================================================================

    EcheanceLoyer findOrThrow(Integer id) {
        return echeanceRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("échéance", id));
    }

    private EcheanceResponseDTO toDto(EcheanceLoyer e) {
        String moisLibelle = e.getDateEcheance() != null
                ? DateUtils.nomMoisFrancais(e.getDateEcheance()) + " " + e.getDateEcheance().getYear()
                : null;

        return EcheanceResponseDTO.builder()
                .idEcheance(e.getIdEcheance())
                .type(e.getEntiteEcheanceType())
                .entiteId(e.getEntiteEcheanceId())
                .dateEcheance(e.getDateEcheance())
                .moisLibelle(moisLibelle)
                .montantDu(e.getMontantDu())
                .montantPaye(e.getMontantPaye())
                .statut(e.getStatut())
                .build();
    }

    private EcheanceMandatResponseDTO toMandatDto(EcheanceLoyer e, Double loyersDus) {
        return EcheanceMandatResponseDTO.builder()
                .idEcheance(e.getIdEcheance())
                .idMandat(e.getEntiteEcheanceId())
                .periodeMois(e.getDateEcheance())
                .montantLoyersDus(loyersDus)
                .commissionDeduite(e.getCommissionDeduite())
                .montantNetAReverser(e.getMontantDu())
                .statut(e.getStatut())
                .build();
    }
}