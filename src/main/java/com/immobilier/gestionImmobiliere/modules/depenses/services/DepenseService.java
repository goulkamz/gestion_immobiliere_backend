package com.immobilier.gestionImmobiliere.modules.depenses.services;

import com.immobilier.gestionImmobiliere.donnees.biens.model.Cour;
import com.immobilier.gestionImmobiliere.donnees.biens.model.Maison;
import com.immobilier.gestionImmobiliere.donnees.biens.repository.CourRepository;
import com.immobilier.gestionImmobiliere.donnees.biens.repository.MaisonRepository;
import com.immobilier.gestionImmobiliere.donnees.contrats.model.ContratMandat;
import com.immobilier.gestionImmobiliere.donnees.contrats.model.StatutMandat;
import com.immobilier.gestionImmobiliere.donnees.contrats.model.TypeMandat;
import com.immobilier.gestionImmobiliere.donnees.contrats.repository.ContratMandatRepository;
import com.immobilier.gestionImmobiliere.donnees.depenses.model.DepenseBien;
import com.immobilier.gestionImmobiliere.donnees.depenses.model.DepenseImputation;
import com.immobilier.gestionImmobiliere.donnees.depenses.model.DevisTravaux;
import com.immobilier.gestionImmobiliere.donnees.depenses.model.StatutDepense;
import com.immobilier.gestionImmobiliere.donnees.depenses.model.StatutDevis;
import com.immobilier.gestionImmobiliere.donnees.depenses.repository.DepenseBienRepository;
import com.immobilier.gestionImmobiliere.donnees.depenses.repository.DepenseImputationRepository;
import com.immobilier.gestionImmobiliere.donnees.paiements.model.EcheanceLoyer;
import com.immobilier.gestionImmobiliere.donnees.paiements.model.TypeEcheance;
import com.immobilier.gestionImmobiliere.donnees.paiements.repository.EcheanceLoyerRepository;
import com.immobilier.gestionImmobiliere.exceptions.ResourceNotFoundException;
import com.immobilier.gestionImmobiliere.modules.depenses.dto.AbandonnerDepenseDTO;
import com.immobilier.gestionImmobiliere.modules.depenses.dto.AnnulerDepenseDTO;
import com.immobilier.gestionImmobiliere.modules.depenses.dto.AnnulerReglementDTO;
import com.immobilier.gestionImmobiliere.modules.depenses.dto.MandatEchuDepensesDTO;
import com.immobilier.gestionImmobiliere.modules.depenses.dto.MotifDTO;
import com.immobilier.gestionImmobiliere.modules.depenses.dto.CreateDepenseDTO;
import com.immobilier.gestionImmobiliere.modules.depenses.dto.DepenseImputationResponseDTO;
import com.immobilier.gestionImmobiliere.modules.depenses.dto.DepenseResponseDTO;
import com.immobilier.gestionImmobiliere.modules.depenses.dto.UpdateDepenseDTO;
import com.immobilier.gestionImmobiliere.modules.depenses.dto.DepenseReglementResponseDTO;
import com.immobilier.gestionImmobiliere.modules.depenses.dto.ReechelonnerDepenseDTO;
import com.immobilier.gestionImmobiliere.modules.depenses.dto.ReglementDepenseDTO;
import com.immobilier.gestionImmobiliere.donnees.depenses.model.DepenseReglement;
import com.immobilier.gestionImmobiliere.donnees.depenses.repository.DepenseReglementRepository;
import com.immobilier.gestionImmobiliere.donnees.paiements.model.StatutEcheance;
import com.immobilier.gestionImmobiliere.modules.notifications.services.AlerteAgentService;
import com.immobilier.gestionImmobiliere.modules.notifications.services.AlerteUtilisateurService;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.immobilier.gestionImmobiliere.utils.BuildSuccessResponse.buildSuccessResponse;

/**
 * Dépenses à la charge du bailleur (étanchéité, vidange, réparations...) avancées par l'agence.
 * Une dépense est rattachée au mandat de gestion actif de la cour et retenue sur ses reversements :
 * d'un coup ({@code nombreMois = 1}) ou étalée. Si le net d'un reversement ne suffit pas, la retenue
 * est plafonnée à ce net et le reste est rattrapé sur les reversements suivants.
 */
@Service
public class DepenseService {

    private final DepenseBienRepository depenseRepository;
    private final DepenseImputationRepository imputationRepository;
    private final ContratMandatRepository mandatRepository;
    private final CourRepository courRepository;
    private final MaisonRepository maisonRepository;
    private final EcheanceLoyerRepository echeanceRepository;
    private final AlerteUtilisateurService alerteUtilisateurService;
    private final AlerteAgentService alerteAgentService;
    private final DepenseReglementRepository reglementRepository;

    public DepenseService(DepenseBienRepository depenseRepository, DepenseImputationRepository imputationRepository,
                          ContratMandatRepository mandatRepository, CourRepository courRepository,
                          MaisonRepository maisonRepository, EcheanceLoyerRepository echeanceRepository,
                          AlerteUtilisateurService alerteUtilisateurService, AlerteAgentService alerteAgentService,
                          DepenseReglementRepository reglementRepository) {
        this.alerteAgentService = alerteAgentService;
        this.reglementRepository = reglementRepository;
        this.depenseRepository = depenseRepository;
        this.imputationRepository = imputationRepository;
        this.mandatRepository = mandatRepository;
        this.courRepository = courRepository;
        this.maisonRepository = maisonRepository;
        this.echeanceRepository = echeanceRepository;
        this.alerteUtilisateurService = alerteUtilisateurService;
    }

    // ==================================================================
    // Lecture
    // ==================================================================

    /** ADMIN/AGENT : toutes les dépenses. BAILLEUR : uniquement celles de ses propres cours. */
    @Transactional(readOnly = true)
    public ResponseEntity<?> getAllForCurrentUser(Integer idCour, Integer idMaison, Integer idMandat, StatutDepense statut, String recherche,
                                                  Integer currentUserId, boolean isAdminOrAgent, Pageable pageable) {
        Specification<DepenseBien> filtre = (root, query, cb) -> {
            List<Predicate> predicats = new ArrayList<>();
            if (idCour != null) predicats.add(cb.equal(root.get("cour").get("idCour"), idCour));
            if (idMaison != null) predicats.add(cb.equal(root.get("maison").get("idMaison"), idMaison));
            if (idMandat != null) predicats.add(cb.equal(root.get("mandat").get("idMandat"), idMandat));
            if (statut != null) predicats.add(cb.equal(root.get("statut"), statut));
            if (recherche != null && !recherche.isBlank()) {
                // Texte libre : libellé, prestataire, référence de la cour, nom de la maison, nom du bailleur
                String motif = "%" + recherche.trim().toLowerCase().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
                Join<Object, Object> cour = root.join("cour", JoinType.LEFT);
                Join<Object, Object> maison = root.join("maison", JoinType.LEFT);
                Join<Object, Object> proprietaire = cour.join("proprietaire", JoinType.LEFT);
                predicats.add(cb.or(
                        cb.like(cb.lower(root.<String>get("libelle")), motif, '\\'),
                        cb.like(cb.lower(cb.coalesce(root.<String>get("prestataire"), "")), motif, '\\'),
                        cb.like(cb.lower(cb.coalesce(cour.<String>get("referenceCour"), "")), motif, '\\'),
                        cb.like(cb.lower(cb.coalesce(maison.<String>get("nomCommunMaison"), "")), motif, '\\'),
                        cb.like(cb.lower(cb.concat(cb.concat(cb.coalesce(proprietaire.<String>get("nom"), ""), " "),
                                cb.coalesce(proprietaire.<String>get("prenom"), ""))), motif, '\\')));
            }
            if (!isAdminOrAgent) {
                predicats.add(cb.equal(root.get("cour").get("proprietaire").get("idUser"), currentUserId));
            }
            return cb.and(predicats.toArray(new Predicate[0]));
        };
        Page<DepenseBien> page = depenseRepository.findAll(filtre, pageable);
        return buildSuccessResponse(HttpStatus.OK, "Liste des dépenses", "DEPENSE_LIST", page.map(d -> toDto(d, false)));
    }

    @Transactional(readOnly = true)
    public ResponseEntity<?> getByIdForCurrentUser(Integer id, Integer currentUserId, boolean isAdminOrAgent) {
        DepenseBien depense = findOrThrow(id);
        verifierAcces(depense.getCour(), currentUserId, isAdminOrAgent);
        return buildSuccessResponse(HttpStatus.OK, "Dépense trouvée", "DEPENSE_FOUND", toDto(depense, true));
    }

    /**
     * Mandats actifs dont la date de fin est dépassée alors que des dépenses sont encore en cours de retenue :
     * l'agent doit les résilier (le solde est alors retenu d'un coup) ou les prolonger par avenant.
     */
    @Transactional(readOnly = true)
    public ResponseEntity<?> getMandatsEchus() {
        List<MandatEchuDepensesDTO> mandats = depenseRepository.mandatsEchusAvecDepensesEnCours(LocalDateTime.now()).stream()
                .map(ligne -> {
                    ContratMandat mandat = mandatRepository.findById((Integer) ligne[0]).orElse(null);
                    if (mandat == null) return null;
                    return MandatEchuDepensesDTO.builder()
                            .idMandat(mandat.getIdMandat())
                            .referenceCour(mandat.getCour().getReferenceCour())
                            .dateFin(mandat.getDateFin())
                            .nombreDepenses((Long) ligne[1])
                            .resteARetenir((BigDecimal) ligne[2])
                            .build();
                })
                .filter(java.util.Objects::nonNull)
                .sorted(java.util.Comparator.comparing(MandatEchuDepensesDTO::getDateFin))
                .toList();
        return buildSuccessResponse(HttpStatus.OK, "Mandats échus avec des dépenses en cours", "DEPENSE_MANDATS_ECHUS", mandats);
    }

    /** Détail des retenues faites sur un reversement (échéance MANDAT). */
    @Transactional(readOnly = true)
    public ResponseEntity<?> getImputationsReversement(Integer idEcheance, Integer currentUserId, boolean isAdminOrAgent) {
        EcheanceLoyer echeance = echeanceRepository.findById(idEcheance)
                .orElseThrow(() -> new ResourceNotFoundException("echeance", idEcheance));
        if (echeance.getEntiteEcheanceType() != TypeEcheance.MANDAT) {
            throw new IllegalStateException("Cette échéance n'est pas un reversement de mandat");
        }
        ContratMandat mandat = mandatRepository.findById(echeance.getEntiteEcheanceId())
                .orElseThrow(() -> new ResourceNotFoundException("mandat", echeance.getEntiteEcheanceId()));
        verifierAcces(mandat.getCour(), currentUserId, isAdminOrAgent);
        return buildSuccessResponse(HttpStatus.OK, "Retenues du reversement", "DEPENSE_IMPUTATION_LIST",
                imputationRepository.findByEcheance_IdEcheance(idEcheance).stream().map(this::toImputationDto).toList());
    }

    // ==================================================================
    // Écriture
    // ==================================================================

    @Transactional
    public ResponseEntity<?> create(CreateDepenseDTO dto, Integer currentUserId) {
        Cour cour = courRepository.findById(dto.getIdCour())
                .orElseThrow(() -> new ResourceNotFoundException("cour", dto.getIdCour()));
        Maison maison = null;
        if (dto.getIdMaison() != null) {
            maison = maisonRepository.findById(dto.getIdMaison())
                    .orElseThrow(() -> new ResourceNotFoundException("maison", dto.getIdMaison()));
            if (!maison.getCour().getIdCour().equals(cour.getIdCour())) {
                throw new IllegalArgumentException("Cette maison n'appartient pas à la cour indiquée");
            }
        }
        // Une dépense se rembourse sur les reversements : il faut un mandat de gestion actif (pas de reversement en LOCATION/VENTE)
        ContratMandat mandat = mandatRepository
                .findFirstByCour_IdCourAndStatutAndTypeMandat(cour.getIdCour(), StatutMandat.ACTIF, TypeMandat.GESTION)
                .orElseThrow(() -> new IllegalStateException(
                        "Aucun mandat de gestion actif sur cette cour : la dépense ne peut pas être retenue sur des reversements"));

        LocalDate dateDepense = dto.getDateDepense() != null ? dto.getDateDepense() : LocalDate.now();
        int nombreMois = dto.getNombreMois() != null ? dto.getNombreMois() : 1;
        boolean accord = Boolean.TRUE.equals(dto.getAccordBailleur());
        String accordMotif = vide(dto.getAccordMotif());
        verifierSaisie(mandat, dateDepense, dto.getMontant(), nombreMois, accord, accordMotif);

        DepenseBien depense = DepenseBien.builder()
                .mandat(mandat)
                .cour(cour)
                .maison(maison)
                .libelle(dto.getLibelle().trim())
                .categorie(dto.getCategorie())
                .description(dto.getDescription())
                .prestataire(dto.getPrestataire())
                .dateDepense(dateDepense)
                .montant(dto.getMontant())
                .nombreMois(nombreMois)
                .accordBailleur(accord)
                .accordMotif(accord ? accordMotif : null)
                .statut(StatutDepense.EN_COURS)
                .userCreate(currentUserId)
                .build();
        depenseRepository.save(depense);
        alerteUtilisateurService.depenseEnregistree(depense);
        return buildSuccessResponse(HttpStatus.CREATED, "Dépense enregistrée", "DEPENSE_CREATED", toDto(depense, false));
    }

    /**
     * Dépense issue d'un devis accepté par le bailleur (appelé par DevisService) : mêmes règles qu'une
     * saisie directe, l'accord du bailleur étant prouvé par le devis.
     */
    @Transactional
    public DepenseBien creerDepuisDevis(DevisTravaux devis, LocalDate dateDepense, BigDecimal montant, Integer currentUserId) {
        ContratMandat mandat = devis.getMandat();
        if (mandat.getStatut() != StatutMandat.ACTIF) {
            throw new IllegalStateException(
                    "Le mandat de gestion de cette cour n'est plus actif : la dépense ne peut pas être retenue sur des reversements");
        }
        // L'accord est prouvé par le devis : accepté sur la plateforme, ou attesté par l'agent (hors plateforme) avec son détail
        boolean horsPlateforme = Boolean.TRUE.equals(devis.getAccordHorsPlateforme());
        String accordMotif = horsPlateforme
                ? "Accord donné hors plateforme le " + devis.getAccordDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                        + " : " + devis.getAccordMotif()
                : null;
        verifierSaisie(mandat, dateDepense, montant, devis.getNombreMois(), true, accordMotif != null ? accordMotif : "devis accepté");

        DepenseBien depense = DepenseBien.builder()
                .mandat(mandat)
                .cour(devis.getCour())
                .maison(devis.getMaison())
                .devis(devis)
                .libelle(devis.getLibelle())
                .categorie(devis.getCategorie())
                .description(devis.getDescription())
                .prestataire(devis.getPrestataire())
                .dateDepense(dateDepense)
                .montant(montant)
                .nombreMois(devis.getNombreMois())
                .accordBailleur(true)
                .accordMotif(accordMotif != null && accordMotif.length() > 500 ? accordMotif.substring(0, 500) : accordMotif)
                .statut(StatutDepense.EN_COURS)
                .userCreate(currentUserId)
                .build();
        depenseRepository.save(depense);
        alerteUtilisateurService.depenseEnregistree(depense);
        return depense;
    }

    /** Tant qu'aucun reversement n'est passé sur la dépense : ensuite les montants retenus font foi. */
    @Transactional
    public ResponseEntity<?> update(Integer id, UpdateDepenseDTO dto, Integer currentUserId) {
        DepenseBien depense = findOrThrow(id);
        verifierNonEntamee(depense, "modifiée");

        if (dto.getLibelle() != null && !dto.getLibelle().isBlank()) depense.setLibelle(dto.getLibelle().trim());
        if (dto.getCategorie() != null) depense.setCategorie(dto.getCategorie());
        if (dto.getDescription() != null) depense.setDescription(dto.getDescription());
        if (dto.getPrestataire() != null) depense.setPrestataire(dto.getPrestataire());
        if (dto.getDateDepense() != null) depense.setDateDepense(dto.getDateDepense());
        if (dto.getMontant() != null) depense.setMontant(dto.getMontant());
        if (dto.getNombreMois() != null) depense.setNombreMois(dto.getNombreMois());
        if (dto.getAccordBailleur() != null && depense.getDevis() == null) depense.setAccordBailleur(dto.getAccordBailleur());
        if (dto.getAccordMotif() != null && depense.getDevis() == null) depense.setAccordMotif(vide(dto.getAccordMotif()));
        if (depense.getDevis() == null && !Boolean.TRUE.equals(depense.getAccordBailleur())) depense.setAccordMotif(null);
        if (depense.getDevis() != null) {
            if (depense.getMontant().compareTo(depense.getDevis().getMontant()) > 0) {
                throw new IllegalStateException("Le montant ne peut pas dépasser celui du devis accepté par le bailleur ("
                        + depense.getDevis().getMontant().stripTrailingZeros().toPlainString() + ")");
            }
            if (!depense.getNombreMois().equals(depense.getDevis().getNombreMois())) {
                throw new IllegalStateException("L'étalement a été accepté par le bailleur avec le devis : il ne peut plus être modifié");
            }
        }
        verifierSaisie(depense.getMandat(), depense.getDateDepense(), depense.getMontant(), depense.getNombreMois(),
                Boolean.TRUE.equals(depense.getAccordBailleur()),
                depense.getDevis() != null ? "devis accepté" : depense.getAccordMotif());

        depense.setUserUpdate(currentUserId);
        depenseRepository.save(depense);
        return buildSuccessResponse(HttpStatus.OK, "Dépense modifiée", "DEPENSE_UPDATED", toDto(depense, false));
    }

    @Transactional
    public ResponseEntity<?> annuler(Integer id, AnnulerDepenseDTO dto, Integer currentUserId) {
        DepenseBien depense = findOrThrow(id);
        verifierNonEntamee(depense, "annulée");
        depense.setStatut(StatutDepense.ANNULEE);
        depense.setMotifAnnulation(dto.getMotif().trim());
        // Dépense issue d'un devis : le devis est clos avec elle (un nouveau devis sera soumis si les travaux reprennent)
        if (depense.getDevis() != null) {
            depense.getDevis().setStatut(StatutDevis.ANNULE);
            depense.getDevis().setMotifAnnulation("Dépense annulée : " + dto.getMotif().trim());
        }
        depense.setUserUpdate(currentUserId);
        depenseRepository.save(depense);
        alerteUtilisateurService.depenseAnnulee(depense);
        return buildSuccessResponse(HttpStatus.OK, "Dépense annulée", "DEPENSE_CANCELLED", toDto(depense, false));
    }

    // ==================================================================
    // Retenue sur les reversements (appelé par EcheanceService)
    // ==================================================================

    /**
     * Retient sur un reversement qui vient d'être calculé les dépenses en cours du mandat, de la plus
     * ancienne à la plus récente, sans jamais dépasser {@code disponible} (loyers - commission).
     * Pour chaque dépense, la cible cumulée après k reversements est montant * k / nombreMois (le
     * dernier prend le reste des arrondis) : un mois plafonné faute de net est donc rattrapé au suivant.
     *
     * @return le total retenu, à déduire du net à reverser
     */
    @Transactional
    public BigDecimal imputerSurReversement(ContratMandat mandat, EcheanceLoyer echeance, BigDecimal disponible) {
        BigDecimal reste = disponible.max(BigDecimal.ZERO);
        BigDecimal total = BigDecimal.ZERO;
        if (reste.signum() == 0) return total;

        // Mandat résilié : plus d'étalement, tout le solde est retenu sur les derniers reversements dus
        boolean mandatTermine = mandat.getStatut() != StatutMandat.ACTIF;
        List<DepenseBien> depenses = depenseRepository.findByMandat_IdMandatAndStatutInOrderByDateDepenseAscIdDepenseAsc(
                mandat.getIdMandat(), STATUTS_A_RECUPERER);
        for (DepenseBien depense : depenses) {
            if (reste.signum() == 0) break;

            int rang = depense.getMoisEcoules() + 1;
            BigDecimal cible = mandatTermine ? depense.getMontant() : cibleCumulee(depense, rang);
            BigDecimal retenue = cible.subtract(depense.getMontantImpute()).min(restant(depense)).min(reste);
            if (retenue.signum() <= 0) continue;

            // Un reversement déjà calculé peut être complété à la résiliation : une seule ligne de retenue par reversement
            DepenseImputation existante = imputationRepository
                    .findFirstByDepense_IdDepenseAndEcheance_IdEcheance(depense.getIdDepense(), echeance.getIdEcheance())
                    .orElse(null);
            if (existante != null) {
                existante.setMontant(existante.getMontant().add(retenue));
                imputationRepository.save(existante);
            } else {
                imputationRepository.save(DepenseImputation.builder()
                        .depense(depense)
                        .echeance(echeance)
                        .montant(retenue)
                        .build());
                depense.setMoisEcoules(rang);
            }
            depense.setMontantImpute(depense.getMontantImpute().add(retenue));
            if (restant(depense).signum() <= 0) {
                depense.setStatut(StatutDepense.SOLDEE);
            }
            depenseRepository.save(depense);

            reste = reste.subtract(retenue);
            total = total.add(retenue);
        }
        return total;
    }

    /**
     * Annule les retenues portées par des reversements qui disparaissent (résiliation du mandat) :
     * les montants redeviennent à retenir sur la dépense.
     */
    @Transactional
    public void libererImputations(List<EcheanceLoyer> reversementsSupprimes) {
        for (EcheanceLoyer echeance : reversementsSupprimes) {
            for (DepenseImputation imputation : imputationRepository.findByEcheance_IdEcheance(echeance.getIdEcheance())) {
                DepenseBien depense = imputation.getDepense();
                depense.setMontantImpute(depense.getMontantImpute().subtract(imputation.getMontant()).max(BigDecimal.ZERO));
                depense.setMoisEcoules(Math.max(0, depense.getMoisEcoules() - 1));
                if (depense.getStatut() == StatutDepense.SOLDEE) {
                    depense.setStatut(depense.getMandat().getStatut() == StatutMandat.ACTIF
                            ? StatutDepense.EN_COURS : StatutDepense.A_REGLER);
                }
                depenseRepository.save(depense);
                imputationRepository.delete(imputation);
            }
        }
    }

    /**
     * Résiliation du mandat avec des dépenses non soldées (appelé après la suppression des reversements qui ne sont
     * plus dus) : l'étalement s'arrête. Le solde est d'abord retenu sur les reversements déjà calculés et pas encore
     * virés, dans la limite de leur net ; ce qui reste devient dû par le bailleur ({@link StatutDepense#A_REGLER}).
     * Les reversements calculés ensuite pour ce mandat retiennent eux aussi tout le solde
     * ({@link #imputerSurReversement}) ; le reliquat se règle hors plateforme ({@link #enregistrerReglement}).
     */
    @Transactional
    public void solderApresResiliation(ContratMandat mandat) {
        Integer idMandat = mandat.getIdMandat();
        if (depenseRepository.findByMandat_IdMandatAndStatutInOrderByDateDepenseAscIdDepenseAsc(idMandat, STATUTS_A_RECUPERER).isEmpty()) {
            return;
        }

        List<EcheanceLoyer> nonVires = echeanceRepository
                .findByEntiteEcheanceTypeAndEntiteEcheanceIdAndStatutIn(TypeEcheance.MANDAT, idMandat,
                        List.of(StatutEcheance.EN_ATTENTE, StatutEcheance.EN_RETARD)).stream()
                .filter(e -> e.getMontantPaye() == null || e.getMontantPaye().signum() == 0)
                .filter(e -> e.getMontantDu() != null && e.getMontantDu().signum() > 0)
                .sorted(java.util.Comparator.comparing(EcheanceLoyer::getDateEcheance))
                .toList();
        BigDecimal totalRetenu = BigDecimal.ZERO;
        for (EcheanceLoyer echeance : nonVires) {
            BigDecimal retenue = imputerSurReversement(mandat, echeance, echeance.getMontantDu());
            if (retenue.signum() > 0) {
                BigDecimal dejaRetenu = echeance.getRetenueDepenses() != null ? echeance.getRetenueDepenses() : BigDecimal.ZERO;
                echeance.setRetenueDepenses(dejaRetenu.add(retenue));
                echeance.setMontantDu(echeance.getMontantDu().subtract(retenue));
                echeanceRepository.save(echeance);
                totalRetenu = totalRetenu.add(retenue);
            }
        }

        BigDecimal totalRestant = BigDecimal.ZERO;
        for (DepenseBien depense : depenseRepository
                .findByMandat_IdMandatAndStatutInOrderByDateDepenseAscIdDepenseAsc(idMandat, STATUTS_A_RECUPERER)) {
            depense.setStatut(StatutDepense.A_REGLER);
            depenseRepository.save(depense);
            totalRestant = totalRestant.add(restant(depense));
        }
        if (totalRetenu.signum() > 0 || totalRestant.signum() > 0) {
            alerteUtilisateurService.depensesApresResiliation(mandat, totalRetenu, totalRestant);
        }
        if (totalRestant.signum() > 0) {
            alerteAgentService.depensesDuesApresResiliation(mandat, totalRestant);
        }
    }

    // ==================================================================
    // Rééchelonnement et règlement direct
    // ==================================================================

    /**
     * Étale le solde restant d'une dépense en cours sur un autre nombre de reversements. Rallonger est libre
     * (les retenues baissent) ; raccourcir les augmente et demande l'accord du bailleur, attesté par l'agent.
     * Les retenues déjà faites ne bougent pas.
     */
    @Transactional
    public ResponseEntity<?> reechelonner(Integer id, ReechelonnerDepenseDTO dto, Integer currentUserId) {
        DepenseBien depense = findOrThrow(id);
        if (depense.getStatut() != StatutDepense.EN_COURS) {
            throw new IllegalStateException("Cette dépense est " + libelle(depense.getStatut()) + " : son étalement ne peut plus être modifié");
        }
        int restantsActuels = moisRestants(depense);
        int nouveaux = dto.getNombreMoisRestants();
        if (nouveaux == restantsActuels) {
            throw new IllegalArgumentException("Le solde de cette dépense est déjà étalé sur " + restantsActuels + " reversement(s)");
        }
        if (nouveaux < restantsActuels && !Boolean.TRUE.equals(dto.getAccordBailleur())) {
            throw new IllegalStateException("Raccourcir l'étalement augmente les retenues : l'accord du bailleur est nécessaire");
        }
        if (depense.getMoisEcoules() + nouveaux > 60) {
            throw new IllegalArgumentException("Une dépense ne peut pas être étalée sur plus de 60 reversements au total");
        }
        if (restant(depense).compareTo(BigDecimal.valueOf(nouveaux)) < 0) {
            throw new IllegalArgumentException("Le solde restant est trop faible pour être étalé sur " + nouveaux + " mois");
        }

        if (depense.getNombreMoisInitial() == null) depense.setNombreMoisInitial(depense.getNombreMois());
        depense.setBaseMontant(depense.getMontantImpute());
        depense.setBaseMois(depense.getMoisEcoules());
        depense.setNombreMois(depense.getMoisEcoules() + nouveaux);
        depense.setMotifReechelonnement(dto.getMotif().trim());
        depense.setDateReechelonnement(java.time.LocalDateTime.now());
        depense.setUserUpdate(currentUserId);
        depenseRepository.save(depense);
        alerteUtilisateurService.depenseReechelonnee(depense, restantsActuels, nouveaux);
        return buildSuccessResponse(HttpStatus.OK, "Étalement de la dépense modifié", "DEPENSE_RESCHEDULED", toDto(depense, false));
    }

    /**
     * Règlement fait directement par le bailleur à l'agence pour une dépense due après la résiliation du mandat.
     * Partiel ou total ; la dépense est soldée quand retenues et règlements couvrent son montant.
     */
    @Transactional
    public ResponseEntity<?> enregistrerReglement(Integer id, ReglementDepenseDTO dto, Integer currentUserId) {
        DepenseBien depense = findOrThrow(id);
        if (depense.getStatut() == StatutDepense.EN_COURS) {
            throw new IllegalStateException("Cette dépense est retenue sur les reversements du mandat : "
                    + "un règlement direct ne s'enregistre qu'après la résiliation du mandat");
        }
        if (depense.getStatut() != StatutDepense.A_REGLER) {
            throw new IllegalStateException("Cette dépense est " + libelle(depense.getStatut()) + " : aucun règlement n'est attendu");
        }
        LocalDate dateReglement = dto.getDateReglement() != null ? dto.getDateReglement() : LocalDate.now();
        if (dateReglement.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La date du règlement ne peut pas être dans le futur");
        }
        BigDecimal restant = restant(depense);
        if (dto.getMontant().compareTo(restant) > 0) {
            throw new IllegalStateException("Le règlement dépasse le solde restant dû ("
                    + restant.stripTrailingZeros().toPlainString() + ")");
        }

        reglementRepository.save(DepenseReglement.builder()
                .depense(depense)
                .montant(dto.getMontant())
                .dateReglement(dateReglement)
                .modeReglement(vide(dto.getModeReglement()))
                .reference(vide(dto.getReference()))
                .commentaire(vide(dto.getCommentaire()))
                .userCreate(currentUserId)
                .build());
        depense.setMontantRegle(depense.getMontantRegle().add(dto.getMontant()));
        if (restant(depense).signum() <= 0) {
            depense.setStatut(StatutDepense.SOLDEE);
        }
        depense.setUserUpdate(currentUserId);
        depenseRepository.save(depense);
        alerteUtilisateurService.depenseReglee(depense, dto.getMontant());
        return buildSuccessResponse(HttpStatus.CREATED, "Règlement enregistré", "DEPENSE_PAYMENT_CREATED", toDto(depense, true));
    }

    /**
     * Annule un règlement saisi par erreur (mauvais montant, mauvaise dépense) : il reste en base, marqué annulé avec son
     * motif, mais ne compte plus. Si la dépense avait été soldée grâce à lui, elle redevient due par le bailleur.
     */
    @Transactional
    public ResponseEntity<?> annulerReglement(Integer idDepense, Integer idReglement, AnnulerReglementDTO dto, Integer currentUserId) {
        DepenseBien depense = findOrThrow(idDepense);
        DepenseReglement reglement = reglementRepository.findById(idReglement)
                .filter(r -> r.getDepense().getIdDepense().equals(idDepense))
                .orElseThrow(() -> new ResourceNotFoundException("reglement", idReglement));
        if (Boolean.TRUE.equals(reglement.getAnnule())) {
            throw new IllegalStateException("Ce règlement est déjà annulé");
        }

        reglement.setAnnule(true);
        reglement.setMotifAnnulation(dto.getMotif().trim());
        reglement.setDateAnnulation(LocalDateTime.now());
        reglement.setUserAnnulation(currentUserId);
        reglementRepository.save(reglement);

        depense.setMontantRegle(depense.getMontantRegle().subtract(reglement.getMontant()).max(BigDecimal.ZERO));
        if (depense.getStatut() == StatutDepense.SOLDEE && restant(depense).signum() > 0) {
            depense.setStatut(depense.getMandat().getStatut() == StatutMandat.ACTIF ? StatutDepense.EN_COURS : StatutDepense.A_REGLER);
        }
        depense.setUserUpdate(currentUserId);
        depenseRepository.save(depense);
        alerteUtilisateurService.reglementAnnule(depense, reglement);
        return buildSuccessResponse(HttpStatus.OK, "Règlement annulé", "DEPENSE_PAYMENT_CANCELLED", toDto(depense, true));
    }

    /**
     * Abandon de créance : l'agence renonce à réclamer le reste dû par le bailleur (introuvable, litige réglé à l'amiable,
     * reste dérisoire). Décision de l'admin, motivée ; la dépense est soldée mais l'abandon reste tracé (perte de l'agence).
     */
    @Transactional
    public ResponseEntity<?> abandonner(Integer id, AbandonnerDepenseDTO dto, Integer currentUserId) {
        DepenseBien depense = findOrThrow(id);
        if (depense.getStatut() != StatutDepense.A_REGLER) {
            throw new IllegalStateException("Cette dépense est " + libelle(depense.getStatut())
                    + " : seul un reste dû par le bailleur après la résiliation du mandat peut être abandonné");
        }
        BigDecimal restant = restant(depense);
        if (restant.signum() <= 0) {
            throw new IllegalStateException("Il ne reste rien à abandonner sur cette dépense");
        }

        depense.setMontantAbandonne(depense.getMontantAbandonne().add(restant));
        depense.setMotifAbandon(dto.getMotif().trim());
        depense.setDateAbandon(LocalDateTime.now());
        depense.setUserAbandon(currentUserId);
        depense.setStatut(StatutDepense.SOLDEE);
        depense.setUserUpdate(currentUserId);
        depenseRepository.save(depense);
        alerteUtilisateurService.creanceAbandonnee(depense, restant);
        return buildSuccessResponse(HttpStatus.OK, "Créance abandonnée", "DEPENSE_DEBT_WAIVED", toDto(depense, true));
    }

    /**
     * Rétablit une créance abandonnée par erreur (ou parce que le bailleur accepte finalement de payer) : le montant abandonné
     * redevient dû, la dépense repasse « due par le bailleur ». L'abandon reste dans l'historique (dates et motifs).
     */
    @Transactional
    public ResponseEntity<?> retablirCreance(Integer id, MotifDTO dto, Integer currentUserId) {
        DepenseBien depense = findOrThrow(id);
        if (depense.getMontantAbandonne().signum() <= 0) {
            throw new IllegalStateException("Aucune créance n'a été abandonnée sur cette dépense");
        }
        BigDecimal retabli = depense.getMontantAbandonne();
        depense.setMontantAbandonne(BigDecimal.ZERO);
        depense.setMotifRetablissement(dto.getMotif().trim());
        depense.setDateRetablissement(LocalDateTime.now());
        depense.setUserRetablissement(currentUserId);
        depense.setStatut(depense.getMandat().getStatut() == StatutMandat.ACTIF ? StatutDepense.EN_COURS : StatutDepense.A_REGLER);
        depense.setUserUpdate(currentUserId);
        depenseRepository.save(depense);
        alerteUtilisateurService.creanceRetablie(depense, retabli);
        return buildSuccessResponse(HttpStatus.OK, "Créance rétablie", "DEPENSE_DEBT_RESTORED", toDto(depense, true));
    }

    /**
     * Le bailleur signale qu'il conteste une dépense dont l'accord n'est pas prouvé sur la plateforme (saisie directe, ou accord
     * attesté par l'agent). Rien n'est annulé : l'agent du mandat et les admins sont alertés et répondent. Une seule contestation
     * par dépense.
     */
    @Transactional
    public ResponseEntity<?> contester(Integer id, MotifDTO dto, Integer currentUserId) {
        DepenseBien depense = findOrThrow(id);
        verifierAcces(depense.getCour(), currentUserId, false);
        if (depense.getStatut() == StatutDepense.ANNULEE) {
            throw new IllegalStateException("Cette dépense est annulée : il n'y a rien à contester");
        }
        if (depense.getDevis() != null && depense.getAccordMotif() == null) {
            throw new IllegalStateException("Vous avez accepté le devis dont cette dépense est issue : elle ne peut pas être contestée");
        }
        if (depense.getDateContestation() != null) {
            throw new IllegalStateException("Cette dépense a déjà été contestée");
        }
        depense.setMotifContestation(dto.getMotif().trim());
        depense.setDateContestation(LocalDateTime.now());
        depenseRepository.save(depense);
        alerteAgentService.depenseContestee(depense);
        return buildSuccessResponse(HttpStatus.OK, "Contestation transmise à l'agence", "DEPENSE_CONTESTED", toDto(depense, true));
    }

    /** L'agence répond à la contestation du bailleur (ce qui l'a décidé : annulation, maintien, explication...) et la clôt. */
    @Transactional
    public ResponseEntity<?> repondreContestation(Integer id, MotifDTO dto, Integer currentUserId) {
        DepenseBien depense = findOrThrow(id);
        if (depense.getDateContestation() == null || depense.getDateReponseContestation() != null) {
            throw new IllegalStateException("Cette dépense n'a aucune contestation en attente de réponse");
        }
        depense.setReponseContestation(dto.getMotif().trim());
        depense.setDateReponseContestation(LocalDateTime.now());
        depense.setUserReponseContestation(currentUserId);
        depense.setUserUpdate(currentUserId);
        depenseRepository.save(depense);
        alerteUtilisateurService.contestationRepondue(depense);
        return buildSuccessResponse(HttpStatus.OK, "Réponse envoyée au bailleur", "DEPENSE_CONTESTATION_ANSWERED", toDto(depense, true));
    }

    // ==================================================================
    // Internes
    // ==================================================================

    /**
     * Au-dessus du plafond du mandat, l'accord du bailleur est nécessaire : l'agent l'atteste en précisant qui l'a
     * donné, quand et comment ({@code accordMotif}), le bailleur étant informé de cette mention.
     */
    private void verifierSaisie(ContratMandat mandat, LocalDate dateDepense, BigDecimal montant, int nombreMois,
                                boolean accordBailleur, String accordMotif) {
        if (dateDepense.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La date de la dépense ne peut pas être dans le futur");
        }
        if (montant.compareTo(BigDecimal.valueOf(nombreMois)) < 0) {
            throw new IllegalArgumentException("Le montant est trop faible pour être étalé sur " + nombreMois + " mois");
        }
        BigDecimal plafond = mandat.getPlafondDepenseSansAccord();
        if (plafond != null && montant.compareTo(plafond) > 0) {
            String plafondTexte = plafond.stripTrailingZeros().toPlainString();
            if (!accordBailleur) {
                throw new IllegalStateException("Cette dépense dépasse le plafond convenu dans le mandat ("
                        + plafondTexte + ") : l'accord du bailleur est nécessaire");
            }
            if (accordMotif == null || accordMotif.isBlank()) {
                throw new IllegalStateException("Cette dépense dépasse le plafond convenu dans le mandat (" + plafondTexte
                        + ") : précisez qui a donné l'accord, quand et comment");
            }
        }
    }

    private void verifierNonEntamee(DepenseBien depense, String action) {
        if (depense.getStatut() != StatutDepense.EN_COURS) {
            throw new IllegalStateException("Cette dépense est " + libelle(depense.getStatut()) + " : elle ne peut plus être " + action);
        }
        if (depense.getMoisEcoules() > 0 || depense.getMontantImpute().signum() > 0) {
            throw new IllegalStateException("Une partie de cette dépense a déjà été retenue sur un reversement : elle ne peut plus être " + action);
        }
    }

    /** Dépenses dont un solde reste à récupérer : sur les reversements (en cours) ou après résiliation. */
    private static final List<StatutDepense> STATUTS_A_RECUPERER = List.of(StatutDepense.EN_COURS, StatutDepense.A_REGLER);

    private static String libelle(StatutDepense statut) {
        return switch (statut) {
            case EN_COURS -> "en cours";
            case A_REGLER -> "due par le bailleur depuis la résiliation du mandat";
            case SOLDEE -> "soldée";
            case ANNULEE -> "annulée";
        };
    }

    /** Ce qui reste à récupérer : ni retenu sur un reversement, ni réglé directement, ni abandonné. */
    private static BigDecimal restant(DepenseBien d) {
        return d.getMontant().subtract(d.getMontantImpute()).subtract(d.getMontantRegle()).subtract(d.getMontantAbandonne());
    }

    /** Reversements encore prévus pour solder une dépense en cours (au moins un tant qu'il reste un solde). */
    private static int moisRestants(DepenseBien d) {
        return Math.max(d.getNombreMois() - d.getMoisEcoules(), 1);
    }

    /**
     * Total qui doit avoir été retenu après le reversement de rang {@code rang} : part proportionnelle du solde
     * étalé depuis le dernier rééchelonnement (ou depuis l'origine), le dernier rang prenant le reste des arrondis.
     */
    private static BigDecimal cibleCumulee(DepenseBien d, int rang) {
        if (rang >= d.getNombreMois()) return d.getMontant();
        BigDecimal base = d.getBaseMontant();
        int moisBase = d.getBaseMois();
        return base.add(d.getMontant().subtract(base)
                .multiply(BigDecimal.valueOf(rang - moisBase))
                .divide(BigDecimal.valueOf(d.getNombreMois() - moisBase), 2, RoundingMode.HALF_UP));
    }

    private static String vide(String texte) {
        return texte == null || texte.isBlank() ? null : texte.trim();
    }

    private void verifierAcces(Cour cour, Integer currentUserId, boolean isAdminOrAgent) {
        if (!isAdminOrAgent && !cour.getProprietaire().getIdUser().equals(currentUserId)) {
            throw new AccessDeniedException("Vous n'avez pas accès à cette dépense");
        }
    }

    private DepenseBien findOrThrow(Integer id) {
        return depenseRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("depense", id));
    }

    private DepenseResponseDTO toDto(DepenseBien d, boolean avecImputations) {
        Cour cour = d.getCour();
        return DepenseResponseDTO.builder()
                .idDepense(d.getIdDepense())
                .idMandat(d.getMandat().getIdMandat())
                .idCour(cour.getIdCour())
                .referenceCour(cour.getReferenceCour())
                .idMaison(d.getMaison() != null ? d.getMaison().getIdMaison() : null)
                .nomMaison(d.getMaison() != null ? d.getMaison().getNomCommunMaison() : null)
                .nomProprietaire(cour.getProprietaire().getNom() + " " + cour.getProprietaire().getPrenom())
                .libelle(d.getLibelle())
                .categorie(d.getCategorie())
                .description(d.getDescription())
                .prestataire(d.getPrestataire())
                .dateDepense(d.getDateDepense())
                .montant(d.getMontant())
                .nombreMois(d.getNombreMois())
                .mensualite(d.getMontant().subtract(d.getBaseMontant())
                        .divide(BigDecimal.valueOf(Math.max(d.getNombreMois() - d.getBaseMois(), 1)), 2, RoundingMode.HALF_UP))
                .moisRestants(d.getStatut() == StatutDepense.EN_COURS ? moisRestants(d) : null)
                .montantImpute(d.getMontantImpute())
                .montantRegle(d.getMontantRegle())
                .montantRestant(d.getStatut() == StatutDepense.ANNULEE ? BigDecimal.ZERO : restant(d))
                .nombreMoisInitial(d.getNombreMoisInitial())
                .motifReechelonnement(d.getMotifReechelonnement())
                .dateReechelonnement(d.getDateReechelonnement())
                .accordBailleur(d.getAccordBailleur())
                .accordMotif(d.getAccordMotif())
                .montantAbandonne(d.getMontantAbandonne())
                .motifAbandon(d.getMotifAbandon())
                .dateAbandon(d.getDateAbandon())
                .motifRetablissement(d.getMotifRetablissement())
                .dateRetablissement(d.getDateRetablissement())
                .motifContestation(d.getMotifContestation())
                .dateContestation(d.getDateContestation())
                .reponseContestation(d.getReponseContestation())
                .dateReponseContestation(d.getDateReponseContestation())
                .contestationOuverte(d.getDateContestation() != null && d.getDateReponseContestation() == null)
                .mandatEchu(d.getStatut() == StatutDepense.EN_COURS && d.getMandat().getStatut() == StatutMandat.ACTIF
                        && d.getMandat().getDateFin() != null && d.getMandat().getDateFin().isBefore(LocalDateTime.now()))
                .dateFinMandat(d.getMandat().getDateFin())
                .idDevis(d.getDevis() != null ? d.getDevis().getIdDevis() : null)
                .statut(d.getStatut())
                .motifAnnulation(d.getMotifAnnulation())
                .imputations(avecImputations
                        ? imputationRepository.findByDepense_IdDepenseOrderByEcheance_DateEcheanceAsc(d.getIdDepense())
                                .stream().map(this::toImputationDto).toList()
                        : null)
                .reglements(avecImputations
                        ? reglementRepository.findByDepense_IdDepenseOrderByDateReglementAscIdReglementAsc(d.getIdDepense())
                                .stream().map(r -> DepenseReglementResponseDTO.builder()
                                        .idReglement(r.getIdReglement())
                                        .montant(r.getMontant())
                                        .dateReglement(r.getDateReglement())
                                        .modeReglement(r.getModeReglement())
                                        .reference(r.getReference())
                                        .commentaire(r.getCommentaire())
                                        .annule(r.getAnnule())
                                        .motifAnnulation(r.getMotifAnnulation())
                                        .dateAnnulation(r.getDateAnnulation())
                                        .build()).toList()
                        : null)
                .build();
    }

    private DepenseImputationResponseDTO toImputationDto(DepenseImputation i) {
        return DepenseImputationResponseDTO.builder()
                .idImputation(i.getIdImputation())
                .idDepense(i.getDepense().getIdDepense())
                .libelleDepense(i.getDepense().getLibelle())
                .idEcheance(i.getEcheance().getIdEcheance())
                .periodeMois(i.getEcheance().getDateEcheance())
                .montant(i.getMontant())
                .statutReversement(i.getEcheance().getStatut())
                .build();
    }
}
