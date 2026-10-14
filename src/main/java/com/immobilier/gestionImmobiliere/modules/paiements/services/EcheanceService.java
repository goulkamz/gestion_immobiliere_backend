package com.immobilier.gestionImmobiliere.modules.paiements.services;

import com.immobilier.gestionImmobiliere.donnees.contrats.model.ContratLocation;
import com.immobilier.gestionImmobiliere.donnees.contrats.model.ContratMandat;
import com.immobilier.gestionImmobiliere.donnees.contrats.model.ModeFacturation;
import com.immobilier.gestionImmobiliere.donnees.contrats.model.StatutMandat;
import com.immobilier.gestionImmobiliere.donnees.contrats.model.TypeMandat;
import com.immobilier.gestionImmobiliere.donnees.contrats.repository.ContratLocationRepository;
import com.immobilier.gestionImmobiliere.donnees.contrats.repository.ContratMandatRepository;
import com.immobilier.gestionImmobiliere.donnees.paiements.model.EcheanceLoyer;
import com.immobilier.gestionImmobiliere.donnees.paiements.model.SensPaiement;
import com.immobilier.gestionImmobiliere.donnees.paiements.model.StatutEcheance;
import com.immobilier.gestionImmobiliere.donnees.paiements.model.TypeEcheance;
import com.immobilier.gestionImmobiliere.donnees.paiements.repository.EcheanceLoyerRepository;
import com.immobilier.gestionImmobiliere.donnees.parametres.model.CleParametre;
import com.immobilier.gestionImmobiliere.exceptions.ResourceNotFoundException;
import com.immobilier.gestionImmobiliere.modules.paiements.dto.requests.ConfirmerVirementDTO;
import com.immobilier.gestionImmobiliere.modules.paiements.dto.responses.EcheanceMandatResponseDTO;
import com.immobilier.gestionImmobiliere.modules.paiements.dto.responses.EcheanceResponseDTO;
import com.immobilier.gestionImmobiliere.modules.parametres.services.ParametreService;
import com.immobilier.gestionImmobiliere.utils.DateUtils;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.immobilier.gestionImmobiliere.utils.BuildSuccessResponse.buildSuccessResponse;
import com.immobilier.gestionImmobiliere.modules.notifications.services.AlerteUtilisateurService;

@Service
public class EcheanceService {

    private final EcheanceLoyerRepository echeanceRepository;
    private final ContratLocationRepository contratLocationRepository;
    private final ContratMandatRepository contratMandatRepository;
    private final PaiementService paiementService;
    private final ParametreService parametreService;

    private final AlerteUtilisateurService alerteUtilisateurService;

    public EcheanceService(EcheanceLoyerRepository echeanceRepository, ContratLocationRepository contratLocationRepository, ContratMandatRepository contratMandatRepository, PaiementService paiementService, ParametreService parametreService, AlerteUtilisateurService alerteUtilisateurService) {
        this.alerteUtilisateurService = alerteUtilisateurService;
        this.echeanceRepository = echeanceRepository;
        this.contratLocationRepository = contratLocationRepository;
        this.contratMandatRepository = contratMandatRepository;
        this.paiementService = paiementService;
        this.parametreService = parametreService;
    }

    // Remplace getAll() par une version filtrée par rôle
    public ResponseEntity<?> getAllForCurrentUser(TypeEcheance type, Integer entiteId, StatutEcheance statut,
                                                  String recherche, String referenceCour, String periode,
                                                  ModeFacturation modeFacturation,
                                                  Integer currentUserId, boolean isAdminOrAgent, boolean isBailleur,
                                                  Pageable pageable) {
        Page<EcheanceLoyer> page;

        if (isAdminOrAgent) {
            // recherche, referenceCour et periode sont reserves a l'agent/admin et appliques avant la pagination
            page = echeanceRepository.findAll(filtrer(type, entiteId, statut, recherche, referenceCour, periode, modeFacturation, null, null), pageable);
        } else if (isBailleur) {
            // Périmètre du bailleur imposé côté serveur ; recherche, cour, période et statut s'appliquent avant la pagination
            List<Integer> locationIds = contratLocationRepository.findIdsByProprietaire(currentUserId);
            List<Integer> mandatIds = contratMandatRepository.findIdsByProprietaire(currentUserId);
            page = echeanceRepository.findAll(filtrer(type, entiteId, statut, recherche, referenceCour, periode, modeFacturation,
                    locationIds.isEmpty() ? List.of(-1) : locationIds,
                    mandatIds.isEmpty() ? List.of(-1) : mandatIds), pageable);
        } else {
            // CLIENT : uniquement les échéances de ses propres contrats de location
            // (type forcé à LOCATION côté serveur, jamais piloté par le client)
            List<Integer> locationIds = contratLocationRepository.findIdsByLocataire(currentUserId);
            page = echeanceRepository.findAll(filtrer(TypeEcheance.LOCATION, entiteId, statut, recherche, referenceCour, periode, null,
                    locationIds.isEmpty() ? List.of(-1) : locationIds, List.of(-1)), pageable);
        }

        return buildSuccessResponse(HttpStatus.OK, "Liste des échéances", "ECHEANCE_LIST", page.map(this::toDtoEnrichi));
    }

    private static final int[] MOIS = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12};

    /**
     * Filtres de la liste agent. La recherche couvre le libelle du mois ("mars 2026"), la maison et le locataire
     * (echeances LOCATION) ou la cour et le proprietaire (echeances MANDAT), sans jointure cote navigateur.
     */
    private Specification<EcheanceLoyer> filtrer(TypeEcheance type, Integer entiteId, StatutEcheance statut,
                                                 String recherche, String referenceCour, String periode,
                                                 ModeFacturation modeFacturation,
                                                 List<Integer> idsLocation, List<Integer> idsMandat) {
        return (root, query, cb) -> {
            List<Predicate> predicats = new ArrayList<>();
            // Périmètre imposé (bailleur / client) : null = aucune restriction (agent, admin)
            if (idsLocation != null && idsMandat != null) {
                predicats.add(cb.or(
                        cb.and(cb.equal(root.get("entiteEcheanceType"), TypeEcheance.LOCATION), root.get("entiteEcheanceId").in(idsLocation)),
                        cb.and(cb.equal(root.get("entiteEcheanceType"), TypeEcheance.MANDAT), root.get("entiteEcheanceId").in(idsMandat))));
            }
            if (type != null) {
                predicats.add(cb.equal(root.get("entiteEcheanceType"), type));
            }
            if (entiteId != null) {
                predicats.add(cb.equal(root.get("entiteEcheanceId"), entiteId));
            }
            if (statut != null) {
                predicats.add(cb.equal(root.get("statut"), statut));
            }
            if (periode != null && !periode.isBlank()) {
                YearMonth mois = YearMonth.parse(periode.trim());
                predicats.add(cb.between(root.<LocalDate>get("dateEcheance"), mois.atDay(1), mois.atEndOfMonth()));
            }
            if (modeFacturation != null) {
                Subquery<Integer> sub = query.subquery(Integer.class);
                Root<ContratMandat> m = sub.from(ContratMandat.class);
                sub.select(m.get("idMandat")).where(
                        cb.equal(m.get("idMandat"), root.get("entiteEcheanceId")),
                        cb.equal(m.get("modeFacturation"), modeFacturation));
                predicats.add(cb.and(cb.equal(root.get("entiteEcheanceType"), TypeEcheance.MANDAT), cb.exists(sub)));
            }
            if (referenceCour != null && !referenceCour.isBlank()) {
                Subquery<Integer> sub = query.subquery(Integer.class);
                Root<ContratMandat> m = sub.from(ContratMandat.class);
                sub.select(m.get("idMandat")).where(
                        cb.equal(m.get("idMandat"), root.get("entiteEcheanceId")),
                        cb.equal(m.get("cour").get("referenceCour"), referenceCour.trim()));
                Subquery<Integer> subLoc = query.subquery(Integer.class);
                Root<ContratLocation> c = subLoc.from(ContratLocation.class);
                subLoc.select(c.get("idContratLocation")).where(
                        cb.equal(c.get("idContratLocation"), root.get("entiteEcheanceId")),
                        cb.equal(c.get("maison").get("cour").get("referenceCour"), referenceCour.trim()));
                predicats.add(cb.or(
                        cb.and(cb.equal(root.get("entiteEcheanceType"), TypeEcheance.MANDAT), cb.exists(sub)),
                        cb.and(cb.equal(root.get("entiteEcheanceType"), TypeEcheance.LOCATION), cb.exists(subLoc))));
            }
            if (recherche != null && !recherche.isBlank()) {
                String terme = recherche.trim().toLowerCase();
                String motif = "%" + terme + "%";
                List<Predicate> alternatives = new ArrayList<>();

                // Echeances LOCATION : maison ou locataire
                Subquery<Integer> subLoc = query.subquery(Integer.class);
                Root<ContratLocation> cl = subLoc.from(ContratLocation.class);
                subLoc.select(cl.get("idContratLocation")).where(
                        cb.equal(cl.get("idContratLocation"), root.get("entiteEcheanceId")),
                        cb.or(
                                cb.like(cb.lower(cl.get("maison").<String>get("nomCommunMaison")), motif),
                                cb.like(cb.lower(cb.concat(cb.concat(cl.get("locataire").<String>get("nom"), " "), cl.get("locataire").<String>get("prenom"))), motif)));
                alternatives.add(cb.and(cb.equal(root.get("entiteEcheanceType"), TypeEcheance.LOCATION), cb.exists(subLoc)));

                // Echeances MANDAT : cour ou proprietaire
                Subquery<Integer> subMandat = query.subquery(Integer.class);
                Root<ContratMandat> cm = subMandat.from(ContratMandat.class);
                subMandat.select(cm.get("idMandat")).where(
                        cb.equal(cm.get("idMandat"), root.get("entiteEcheanceId")),
                        cb.or(
                                cb.like(cb.lower(cm.get("cour").<String>get("referenceCour")), motif),
                                cb.like(cb.lower(cb.concat(cb.concat(cm.get("cour").get("proprietaire").<String>get("nom"), " "), cm.get("cour").get("proprietaire").<String>get("prenom"))), motif)));
                alternatives.add(cb.and(cb.equal(root.get("entiteEcheanceType"), TypeEcheance.MANDAT), cb.exists(subMandat)));

                // Libelle du mois : "mars", "mars 2026" ou "2026"
                Predicate parMois = predicatMois(root, cb, terme);
                if (parMois != null) {
                    alternatives.add(parMois);
                }
                predicats.add(cb.or(alternatives.toArray(new Predicate[0])));
            }
            return cb.and(predicats.toArray(new Predicate[0]));
        };
    }

    // "aout" doit retrouver "août"
    private static String sansAccents(String texte) {
        return Normalizer.normalize(texte, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }

    // Le libelle du mois (nom francais + annee) n'est pas stocke : on retrouve les mois dont le nom contient le terme
    private Predicate predicatMois(Root<EcheanceLoyer> root, CriteriaBuilder cb, String terme) {
        String nom = terme;
        Integer annee = null;
        Matcher m = Pattern.compile("^(.*?)\\s*(\\d{4})$").matcher(terme);
        if (m.matches()) {
            nom = m.group(1).trim();
            annee = Integer.valueOf(m.group(2));
        }
        List<Predicate> conditions = new ArrayList<>();
        if (!nom.isEmpty()) {
            // date_part (PostgreSQL) renvoie un double : on compare donc avec des Double
            List<Double> mois = new ArrayList<>();
            for (int numero : MOIS) {
                if (sansAccents(DateUtils.nomMoisFrancais(LocalDate.of(2000, numero, 1)).toLowerCase()).contains(sansAccents(nom))) {
                    mois.add((double) numero);
                }
            }
            if (mois.isEmpty()) {
                return null;
            }
            conditions.add(cb.function("date_part", Double.class, cb.literal("month"), root.<LocalDate>get("dateEcheance")).in(mois));
        }
        if (annee != null) {
            conditions.add(cb.equal(cb.function("date_part", Double.class, cb.literal("year"), root.<LocalDate>get("dateEcheance")), annee.doubleValue()));
        }
        return conditions.isEmpty() ? null : cb.and(conditions.toArray(new Predicate[0]));
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
                enRetard.stream().map(e -> toMandatDto(e, e.getMontantDu().add(
                        e.getCommissionDeduite() != null ? e.getCommissionDeduite() : BigDecimal.ZERO))).toList());
    }

    /**
     * Job de bascule EN_ATTENTE -> EN_RETARD (à brancher sur un @Scheduled quotidien).
     */
    @Transactional
    public void marquerEcheanceLocationEnRetard() {
        int tolerance = parametreService.getEntier(CleParametre.TOLERANCE_LOCATION_JOURS, 4);
        LocalDate seuil = LocalDate.now().minusDays(tolerance);
        List<EcheanceLoyer> expirees = echeanceRepository.findByEntiteEcheanceTypeAndStatutAndDateEcheanceBefore(TypeEcheance.LOCATION,StatutEcheance.EN_ATTENTE, seuil);
        //expirees.forEach(e -> e.setStatut(StatutEcheance.EN_RETARD));
        BigDecimal montantPenalite = parametreService.getDecimal(CleParametre.PENALITE_RETARD_MONTANT, BigDecimal.ZERO);
        for (EcheanceLoyer e : expirees) {
            e.setStatut(StatutEcheance.EN_RETARD);
            e.setPenalite(montantPenalite);
        }
        echeanceRepository.saveAll(expirees);
    }

    @Transactional
    public void marquerEcheanceMandatEnRetard() {
        int tolerance = parametreService.getEntier(CleParametre.TOLERANCE_MANDAT_JOURS, 10);
        LocalDate seuil = LocalDate.now().minusDays(tolerance);
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

    /** Résultat d'un calcul de reversement pour un mois : l'échéance créée et le total des loyers dus. */
    private record CalculMois(EcheanceLoyer echeance, BigDecimal loyersDus) {}

    /**
     * À la demande de l'agent (jamais automatique) — calcule pour un mandat GESTION en mode
     * MENSUEL et pour un mois donné :
     * - les loyers dus ce mois (montant_du des échéances LOCATION des maisons de la cour, que le
     *   locataire ait payé ou non : le bailleur est dû tant que la maison est louée) ;
     * - la commission de l'agence, déduite selon le pourcentage du mandat ;
     * - le net à reverser au bailleur.
     * Crée une échéance MANDAT unique pour ce mandat/mois (anti-doublon).
     * montantDu = net à reverser au bailleur | commissionDeduite = gain de l'agence.
     * Un mandat en mode CUMUL passe par {@link #calculerReversementCumulMandat}.
     */
    @Transactional
    public ResponseEntity<?> calculerReversementMandat(Integer idMandat, LocalDate periode, Integer currentAgentId) {
        ContratMandat mandat = chargerMandatGestion(idMandat, ModeFacturation.MENSUEL);
        LocalDate debutMois = periode.withDayOfMonth(1);

        // Même règle que le calcul cumulé : un mandat résilié reste redevable jusqu'à sa résiliation, jamais au-delà
        if (mandat.getStatut() != StatutMandat.ACTIF && mandat.getStatut() != StatutMandat.RESILIE) {
            throw new IllegalStateException("Le reversement ne peut être calculé que pour un mandat actif ou résilié");
        }
        if (mandat.getDateResiliation() != null
                && debutMois.isAfter(mandat.getDateResiliation().toLocalDate().withDayOfMonth(1))) {
            throw new IllegalStateException("La période demandée est postérieure à la résiliation du mandat");
        }
        if (!echeancesMandatDuMois(idMandat, debutMois).isEmpty()) {
            throw new IllegalStateException("Le montant a déjà été calculé pour ce mandat sur cette période");
        }
        if (mandat.getDateDebut() != null && debutMois.isBefore(mandat.getDateDebut().toLocalDate().withDayOfMonth(1))) {
            throw new IllegalStateException("La période demandée est antérieure au début du mandat");
        }
        if (echeanceRepository.countLocationParCourEtMois(mandat.getCour().getIdCour(), debutMois) == 0) {
            throw new IllegalStateException("Aucune maison de ce mandat n'est en location sur cette période");
        }

        CalculMois calcul = creerEcheanceMandat(mandat, debutMois, currentAgentId);
        alerteUtilisateurService.reversementCalcule(mandat, calcul.echeance().getMontantDu(),
                "le mois de " + debutMois.format(java.time.format.DateTimeFormatter.ofPattern("MM/yyyy")),
                "REVERSEMENT_CALCULE:" + calcul.echeance().getIdEcheance());
        return buildSuccessResponse(HttpStatus.CREATED, "Montant à reverser calculé", "ECHEANCE_MANDAT_CALCULEE",
                toMandatDto(calcul.echeance(), calcul.loyersDus()));
    }

    /**
     * Calcul cumulé pour un mandat GESTION en mode CUMUL : l'agent verse au bailleur plusieurs
     * mois d'un coup (annuel, trimestriel, semestriel : c'est lui qui choisit jusqu'à quel mois).
     * Refait le calcul mensuel, mois par mois, depuis le début du mandat jusqu'au mois de
     * {@code jusquA} inclus (borné à la résiliation), et crée une échéance MANDAT par mois.
     * Les mois déjà calculés ou sans maison en location sont sautés : un second calcul ne couvre
     * donc que les mois restants. Jusqu'au premier mois seulement, le résultat est identique à
     * celui du calcul mensuel.
     */
    @Transactional
    public ResponseEntity<?> calculerReversementCumulMandat(Integer idMandat, LocalDate jusquA, Integer currentAgentId) {
        ContratMandat mandat = chargerMandatGestion(idMandat, ModeFacturation.CUMUL);
        if (mandat.getStatut() != StatutMandat.ACTIF && mandat.getStatut() != StatutMandat.RESILIE) {
            throw new IllegalStateException("Le calcul n'est possible que pour un mandat actif ou résilié");
        }

        LocalDate dernierMois = jusquA.withDayOfMonth(1);
        if (mandat.getDateResiliation() != null) {
            LocalDate moisResiliation = mandat.getDateResiliation().toLocalDate().withDayOfMonth(1);
            if (moisResiliation.isBefore(dernierMois)) {
                dernierMois = moisResiliation;
            }
        }
        LocalDate premierMois = mandat.getDateDebut() != null
                ? mandat.getDateDebut().toLocalDate().withDayOfMonth(1)
                : dernierMois;
        if (java.time.temporal.ChronoUnit.MONTHS.between(premierMois, dernierMois) > 60) {
            throw new IllegalStateException("La période demandée est trop étendue (60 mois maximum)");
        }

        Integer idCour = mandat.getCour().getIdCour();
        List<CalculMois> calculs = new java.util.ArrayList<>();
        for (LocalDate mois = premierMois; !mois.isAfter(dernierMois); mois = mois.plusMonths(1)) {
            if (!echeancesMandatDuMois(idMandat, mois).isEmpty()) {
                continue;
            }
            if (echeanceRepository.countLocationParCourEtMois(idCour, mois) == 0) {
                continue;
            }
            calculs.add(creerEcheanceMandat(mandat, mois, currentAgentId));
        }
        if (calculs.isEmpty()) {
            throw new IllegalStateException("Aucun mois à calculer pour ce mandat jusqu'à cette période");
        }

        BigDecimal totalLoyers = calculs.stream().map(CalculMois::loyersDus).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCommission = calculs.stream().map(c -> c.echeance().getCommissionDeduite()).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalNet = calculs.stream().map(c -> c.echeance().getMontantDu()).reduce(BigDecimal.ZERO, BigDecimal::add);

        alerteUtilisateurService.reversementCalcule(mandat, totalNet,
                calculs.size() + " mois jusqu'à " + dernierMois.format(java.time.format.DateTimeFormatter.ofPattern("MM/yyyy")),
                "REVERSEMENT_CUMUL:" + calculs.get(0).echeance().getIdEcheance());
        java.util.Map<String, Object> resultat = new java.util.LinkedHashMap<>();
        resultat.put("idMandat", idMandat);
        resultat.put("nombreMois", calculs.size());
        resultat.put("montantLoyersDus", totalLoyers);
        resultat.put("commissionDeduite", totalCommission);
        resultat.put("montantNetAReverser", totalNet);
        resultat.put("echeances", calculs.stream().map(c -> toMandatDto(c.echeance(), c.loyersDus())).toList());
        return buildSuccessResponse(HttpStatus.CREATED, "Montant cumulé à reverser calculé", "ECHEANCES_MANDAT_CUMUL_CALCULEES", resultat);
    }

    /**
     * Calcul groupé mensuel : parcourt les mandats ACTIF GESTION en mode MENSUEL (les autres
     * ne sont pas concernés). Chaque mandat est calculé isolément ; ceux déjà calculés ou sans
     * maison en location sur la période sont ignorés, avec le motif regroupé dans la réponse.
     */
    public ResponseEntity<?> calculerReversementsTousMandats(LocalDate periode, Integer currentAgentId) {
        List<ContratMandat> mandats = mandatsActifsGestion(ModeFacturation.MENSUEL);
        return calculerEnGroupe(mandats, m -> calculerReversementMandat(m.getIdMandat(), periode, currentAgentId));
    }

    /**
     * Calcul groupé cumulé : même principe, pour les mandats ACTIF GESTION en mode CUMUL,
     * jusqu'au mois de {@code jusquA} inclus.
     */
    public ResponseEntity<?> calculerReversementsCumulTousMandats(LocalDate jusquA, Integer currentAgentId) {
        List<ContratMandat> mandats = mandatsActifsGestion(ModeFacturation.CUMUL);
        return calculerEnGroupe(mandats, m -> calculerReversementCumulMandat(m.getIdMandat(), jusquA, currentAgentId));
    }

    private List<ContratMandat> mandatsActifsGestion(ModeFacturation mode) {
        return contratMandatRepository.findByStatut(StatutMandat.ACTIF).stream()
                .filter(m -> m.getTypeMandat() == TypeMandat.GESTION && m.getModeFacturation() == mode)
                .toList();
    }

    private ResponseEntity<?> calculerEnGroupe(List<ContratMandat> mandats, java.util.function.Consumer<ContratMandat> calcul) {
        int succes = 0;
        java.util.Map<String, Integer> raisons = new java.util.LinkedHashMap<>();
        for (ContratMandat mandat : mandats) {
            try {
                calcul.accept(mandat);
                succes++;
            } catch (IllegalStateException | ResourceNotFoundException e) {
                raisons.merge(e.getMessage(), 1, Integer::sum);
            }
        }
        List<java.util.Map<String, Object>> detail = raisons.entrySet().stream()
                .<java.util.Map<String, Object>>map(e -> java.util.Map.of("message", e.getKey(), "nombre", e.getValue()))
                .toList();
        java.util.Map<String, Object> resultat = new java.util.LinkedHashMap<>();
        resultat.put("total", mandats.size());
        resultat.put("succes", succes);
        resultat.put("ignores", mandats.size() - succes);
        resultat.put("raisons", detail);
        return buildSuccessResponse(HttpStatus.OK, "Calcul groupé terminé", "ECHEANCES_MANDAT_CALCULEES", resultat);
    }

    /** Charge un mandat et vérifie qu'il est de type GESTION avec le mode de facturation attendu. */
    private ContratMandat chargerMandatGestion(Integer idMandat, ModeFacturation modeAttendu) {
        ContratMandat mandat = contratMandatRepository.findById(idMandat)
                .orElseThrow(() -> new ResourceNotFoundException("mandat", idMandat));
        if (mandat.getTypeMandat() != TypeMandat.GESTION) {
            throw new IllegalStateException("Le calcul du reversement n'est disponible que pour les mandats de gestion");
        }
        if (mandat.getModeFacturation() != modeAttendu) {
            throw new IllegalStateException(modeAttendu == ModeFacturation.MENSUEL
                    ? "Ce mandat est en facturation cumulée : utilisez le calcul cumulé"
                    : "Ce mandat est en facturation mensuelle : utilisez le calcul mensuel");
        }
        return mandat;
    }

    private List<EcheanceLoyer> echeancesMandatDuMois(Integer idMandat, LocalDate debutMois) {
        return echeanceRepository.findByEntiteEcheanceTypeAndEntiteEcheanceIdAndDateEcheanceBetween(
                TypeEcheance.MANDAT, idMandat, debutMois, debutMois.withDayOfMonth(debutMois.lengthOfMonth()));
    }

    /** Calcule et enregistre l'échéance MANDAT d'un mois : loyers dus, commission de l'agence, net au bailleur. */
    private CalculMois creerEcheanceMandat(ContratMandat mandat, LocalDate debutMois, Integer currentAgentId) {
        BigDecimal loyersDus = echeanceRepository.sumMontantDuLocationParCourEtMois(mandat.getCour().getIdCour(), debutMois);
        loyersDus = loyersDus != null ? loyersDus : BigDecimal.ZERO;

        BigDecimal pourcentage = mandat.getCommission() != null ? mandat.getCommission() : BigDecimal.ZERO;
        BigDecimal commission = loyersDus
                .multiply(pourcentage)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        EcheanceLoyer echeance = EcheanceLoyer.builder()
                .entiteEcheanceType(TypeEcheance.MANDAT)
                .entiteEcheanceId(mandat.getIdMandat())
                .dateEcheance(debutMois)
                .montantDu(loyersDus.subtract(commission))
                .montantPaye(BigDecimal.ZERO)
                .commissionDeduite(commission)
                .statut(StatutEcheance.EN_ATTENTE)
                .userCreate(currentAgentId)
                .build();
        echeanceRepository.save(echeance);
        return new CalculMois(echeance, loyersDus);
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

        if (echeance.getMontantDu().signum() <= 0) {
            // Rien à reverser (aucun loyer ce mois) : on clôture sans créer de paiement à 0.
            echeance.setStatut(StatutEcheance.PAYE);
            echeanceRepository.save(echeance);
            return buildSuccessResponse(HttpStatus.OK, "Aucun montant à reverser, échéance clôturée", "VIREMENT_MANDAT_CONFIRME", null);
        }

        paiementService.soldeEcheances(
                List.of(idEcheance),
                echeance.getMontantDu(),
                dto.getModeVersement(),
                dto.getReference(),
                SensPaiement.SORTIE,
                currentAgentId,
                LocalDateTime.now());

        alerteUtilisateurService.virementMandatConfirme(echeance);
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

    // toDto + libelles de l'entite liee (maison/locataire ou cour/proprietaire)
    private EcheanceResponseDTO toDtoEnrichi(EcheanceLoyer e) {
        EcheanceResponseDTO dto = toDto(e);
        if (e.getEntiteEcheanceType() == TypeEcheance.LOCATION) {
            contratLocationRepository.findById(e.getEntiteEcheanceId()).ifPresent(c -> {
                dto.setNomCommunMaison(c.getMaison().getNomCommunMaison());
                dto.setNomLocataire(c.getLocataire().getNom() + " " + c.getLocataire().getPrenom());
            });
        } else {
            contratMandatRepository.findById(e.getEntiteEcheanceId()).ifPresent(c -> {
                dto.setReferenceCour(c.getCour().getReferenceCour());
                dto.setNomProprietaire(c.getCour().getProprietaire().getNom() + " " + c.getCour().getProprietaire().getPrenom());
                dto.setCommissionMandat(c.getCommission());
            });
        }
        return dto;
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
                .penalite(e.getPenalite())
                .commissionDeduite(e.getCommissionDeduite())
                .statut(e.getStatut())
                .build();
    }

    private EcheanceMandatResponseDTO toMandatDto(EcheanceLoyer e, BigDecimal loyersDus) {
        return EcheanceMandatResponseDTO.builder()
                .idEcheance(e.getIdEcheance())
                .idMandat(e.getEntiteEcheanceId())
                .referenceCour(contratMandatRepository.findById(e.getEntiteEcheanceId())
                        .map(c -> c.getCour().getReferenceCour()).orElse(null))
                .periodeMois(e.getDateEcheance())
                .montantLoyersDus(loyersDus)
                .commissionDeduite(e.getCommissionDeduite())
                .montantNetAReverser(e.getMontantDu())
                .statut(e.getStatut())
                .build();
    }
}