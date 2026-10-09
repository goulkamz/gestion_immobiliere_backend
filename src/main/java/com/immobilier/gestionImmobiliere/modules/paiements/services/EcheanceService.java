package com.immobilier.gestionImmobiliere.modules.paiements.services;

import com.immobilier.gestionImmobiliere.donnees.contrats.model.ContratLocation;
import com.immobilier.gestionImmobiliere.donnees.contrats.model.ContratMandat;
import com.immobilier.gestionImmobiliere.donnees.contrats.model.StatutMandat;
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

@Service
public class EcheanceService {

    private final EcheanceLoyerRepository echeanceRepository;
    private final ContratLocationRepository contratLocationRepository;
    private final ContratMandatRepository contratMandatRepository;
    private final PaiementService paiementService;
    private final ParametreService parametreService;

    public EcheanceService(EcheanceLoyerRepository echeanceRepository, ContratLocationRepository contratLocationRepository, ContratMandatRepository contratMandatRepository, PaiementService paiementService, ParametreService parametreService) {
        this.echeanceRepository = echeanceRepository;
        this.contratLocationRepository = contratLocationRepository;
        this.contratMandatRepository = contratMandatRepository;
        this.paiementService = paiementService;
        this.parametreService = parametreService;
    }

    // Remplace getAll() par une version filtrée par rôle
    public ResponseEntity<?> getAllForCurrentUser(TypeEcheance type, Integer entiteId, StatutEcheance statut,
                                                  String recherche, String referenceCour, String periode,
                                                  Integer currentUserId, boolean isAdminOrAgent, boolean isBailleur,
                                                  Pageable pageable) {
        Page<EcheanceLoyer> page;

        if (isAdminOrAgent) {
            // recherche, referenceCour et periode sont reserves a l'agent/admin et appliques avant la pagination
            page = echeanceRepository.findAll(filtrerAgent(type, entiteId, statut, recherche, referenceCour, periode), pageable);
        } else if (isBailleur) {
            List<Integer> locationIds = contratLocationRepository.findIdsByProprietaire(currentUserId);
            List<Integer> mandatIds = contratMandatRepository.findIdsByProprietaire(currentUserId);
            page = echeanceRepository.findForBailleurFiltered(
                    locationIds.isEmpty() ? List.of(-1) : locationIds,
                    mandatIds.isEmpty() ? List.of(-1) : mandatIds,
                    type, statut,
                    pageable);
        } else {
            // CLIENT : uniquement les échéances de ses propres contrats de location
            // (type forcé à LOCATION côté serveur, jamais piloté par le client)
            List<Integer> locationIds = contratLocationRepository.findIdsByLocataire(currentUserId);
            page = echeanceRepository.findLocationPourClient(
                    locationIds.isEmpty() ? List.of(-1) : locationIds, statut, pageable);
        }

        return buildSuccessResponse(HttpStatus.OK, "Liste des échéances", "ECHEANCE_LIST", page.map(this::toDtoEnrichi));
    }

    private static final int[] MOIS = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12};

    /**
     * Filtres de la liste agent. La recherche couvre le libelle du mois ("mars 2026"), la maison et le locataire
     * (echeances LOCATION) ou la cour et le proprietaire (echeances MANDAT), sans jointure cote navigateur.
     */
    private Specification<EcheanceLoyer> filtrerAgent(TypeEcheance type, Integer entiteId, StatutEcheance statut,
                                                      String recherche, String referenceCour, String periode) {
        return (root, query, cb) -> {
            List<Predicate> predicats = new ArrayList<>();
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
            if (referenceCour != null && !referenceCour.isBlank()) {
                Subquery<Integer> sub = query.subquery(Integer.class);
                Root<ContratMandat> m = sub.from(ContratMandat.class);
                sub.select(m.get("idMandat")).where(
                        cb.equal(m.get("idMandat"), root.get("entiteEcheanceId")),
                        cb.equal(m.get("cour").get("referenceCour"), referenceCour.trim()));
                predicats.add(cb.and(cb.equal(root.get("entiteEcheanceType"), TypeEcheance.MANDAT), cb.exists(sub)));
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

        if (mandat.getDateDebut() != null && debutMois.isBefore(mandat.getDateDebut().toLocalDate().withDayOfMonth(1))) {
            throw new IllegalStateException("La période demandée est antérieure au début du mandat");
        }
        if (echeanceRepository.countLocationParCourEtMois(mandat.getCour().getIdCour(), debutMois) == 0) {
            throw new IllegalStateException("Aucune maison de ce mandat n'est en location sur cette période");
        }

        BigDecimal loyersDus = echeanceRepository.sumMontantDuLocationParCourEtMois(mandat.getCour().getIdCour(), debutMois);
        loyersDus = loyersDus != null ? loyersDus : BigDecimal.ZERO;

        BigDecimal pourcentage = mandat.getCommission() != null ? mandat.getCommission() : BigDecimal.ZERO;
        BigDecimal commission = loyersDus
                .multiply(pourcentage)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        BigDecimal montantNetAReverser = loyersDus.subtract(commission);

        EcheanceLoyer echeance = EcheanceLoyer.builder()
                .entiteEcheanceType(TypeEcheance.MANDAT)
                .entiteEcheanceId(idMandat)
                .dateEcheance(debutMois)
                .montantDu(montantNetAReverser)
                .montantPaye(BigDecimal.ZERO)
                .commissionDeduite(commission)
                .statut(StatutEcheance.EN_ATTENTE)
                .userCreate(currentAgentId)
                .build();
        echeanceRepository.save(echeance);

        return buildSuccessResponse(HttpStatus.CREATED, "Montant à reverser calculé", "ECHEANCE_MANDAT_CALCULEE",
                toMandatDto(echeance, loyersDus));
    }

    /**
     * Calcul groupé : parcourt tous les mandats ACTIF côté serveur (aucun plafond de pagination).
     * Chaque mandat est calculé isolément ; ceux déjà calculés ou sans maison en location sur la
     * période sont ignorés, avec le motif regroupé dans la réponse.
     */
    public ResponseEntity<?> calculerReversementsTousMandats(LocalDate periode, Integer currentAgentId) {
        List<ContratMandat> mandats = contratMandatRepository.findByStatut(StatutMandat.ACTIF);
        int succes = 0;
        java.util.Map<String, Integer> raisons = new java.util.LinkedHashMap<>();
        for (ContratMandat mandat : mandats) {
            try {
                calculerReversementMandat(mandat.getIdMandat(), periode, currentAgentId);
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