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
import com.immobilier.gestionImmobiliere.donnees.depenses.model.DevisTravaux;
import com.immobilier.gestionImmobiliere.donnees.documents.model.TypeDocument;
import com.immobilier.gestionImmobiliere.donnees.documents.model.TypeEntiteDocument;
import com.immobilier.gestionImmobiliere.donnees.documents.repository.DocumentRepository;
import com.immobilier.gestionImmobiliere.modules.depenses.dto.AccordHorsPlateformeDTO;
import com.immobilier.gestionImmobiliere.donnees.depenses.model.StatutDevis;
import com.immobilier.gestionImmobiliere.donnees.depenses.repository.DepenseBienRepository;
import com.immobilier.gestionImmobiliere.donnees.depenses.repository.DevisTravauxRepository;
import com.immobilier.gestionImmobiliere.exceptions.ResourceNotFoundException;
import com.immobilier.gestionImmobiliere.modules.depenses.dto.AccepterDevisDTO;
import com.immobilier.gestionImmobiliere.modules.depenses.dto.CreateDevisDTO;
import com.immobilier.gestionImmobiliere.modules.depenses.dto.DepenseDepuisDevisDTO;
import com.immobilier.gestionImmobiliere.modules.depenses.dto.DevisResponseDTO;
import com.immobilier.gestionImmobiliere.modules.depenses.dto.ReponseDevisDTO;
import com.immobilier.gestionImmobiliere.modules.depenses.dto.UpdateDevisDTO;
import com.immobilier.gestionImmobiliere.donnees.parametres.model.CleParametre;
import com.immobilier.gestionImmobiliere.modules.notifications.services.AlerteAgentService;
import com.immobilier.gestionImmobiliere.modules.parametres.services.ParametreService;
import com.immobilier.gestionImmobiliere.modules.notifications.services.AlerteUtilisateurService;
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
 * Devis de travaux soumis au bailleur. L'agence propose (montant, prestataire, étalement), le bailleur
 * accepte, demande une renégociation (l'agence révise et soumet à nouveau) ou rejette (les travaux se
 * règlent alors hors plateforme). Un devis accepté donne lieu à une seule dépense, dont le montant ne
 * dépasse jamais celui que le bailleur a accepté.
 */
@Service
public class DevisService {

    private final DevisTravauxRepository devisRepository;
    private final DepenseBienRepository depenseRepository;
    private final ContratMandatRepository mandatRepository;
    private final CourRepository courRepository;
    private final MaisonRepository maisonRepository;
    private final DepenseService depenseService;
    private final AlerteUtilisateurService alerteUtilisateurService;
    private final AlerteAgentService alerteAgentService;
    private final ParametreService parametreService;
    private final DocumentRepository documentRepository;
    private final DevisPieceJointeService pieceJointeService;

    public DevisService(DevisTravauxRepository devisRepository, DepenseBienRepository depenseRepository,
                        ContratMandatRepository mandatRepository, CourRepository courRepository,
                        MaisonRepository maisonRepository, DepenseService depenseService,
                        AlerteUtilisateurService alerteUtilisateurService, AlerteAgentService alerteAgentService,
                        ParametreService parametreService, DocumentRepository documentRepository,
                        DevisPieceJointeService pieceJointeService) {
        this.pieceJointeService = pieceJointeService;
        this.documentRepository = documentRepository;
        this.parametreService = parametreService;
        this.devisRepository = devisRepository;
        this.depenseRepository = depenseRepository;
        this.mandatRepository = mandatRepository;
        this.courRepository = courRepository;
        this.maisonRepository = maisonRepository;
        this.depenseService = depenseService;
        this.alerteUtilisateurService = alerteUtilisateurService;
        this.alerteAgentService = alerteAgentService;
    }

    // ==================================================================
    // Lecture
    // ==================================================================

    /** ADMIN/AGENT : tous les devis. BAILLEUR : uniquement ceux de ses propres cours. */
    @Transactional(readOnly = true)
    public ResponseEntity<?> getAllForCurrentUser(Integer idCour, StatutDevis statut, String recherche, Integer currentUserId,
                                                  boolean isAdminOrAgent, Pageable pageable) {
        Specification<DevisTravaux> filtre = (root, query, cb) -> {
            List<Predicate> predicats = new ArrayList<>();
            if (idCour != null) predicats.add(cb.equal(root.get("cour").get("idCour"), idCour));
            if (statut != null) predicats.add(cb.equal(root.get("statut"), statut));
            if (recherche != null && !recherche.isBlank()) {
                // Texte libre : libellé, prestataire, référence de la cour, nom de la maison, nom du bailleur (jokers SQL neutralisés)
                String motif = "%" + recherche.trim().toLowerCase().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
                jakarta.persistence.criteria.Join<Object, Object> cour = root.join("cour", jakarta.persistence.criteria.JoinType.LEFT);
                jakarta.persistence.criteria.Join<Object, Object> maison = root.join("maison", jakarta.persistence.criteria.JoinType.LEFT);
                jakarta.persistence.criteria.Join<Object, Object> proprietaire = cour.join("proprietaire", jakarta.persistence.criteria.JoinType.LEFT);
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
        Page<DevisTravaux> page = devisRepository.findAll(filtre, pageable);
        return buildSuccessResponse(HttpStatus.OK, "Liste des devis", "DEVIS_LIST", page.map(this::toDto));
    }

    @Transactional(readOnly = true)
    public ResponseEntity<?> getByIdForCurrentUser(Integer id, Integer currentUserId, boolean isAdminOrAgent) {
        DevisTravaux devis = findOrThrow(id);
        if (!isAdminOrAgent && !estProprietaire(devis, currentUserId)) {
            throw new AccessDeniedException("Vous n'avez pas accès à ce devis");
        }
        return buildSuccessResponse(HttpStatus.OK, "Devis trouvé", "DEVIS_FOUND", toDto(devis));
    }

    // ==================================================================
    // Agence : soumettre, réviser, retirer, enregistrer la dépense
    // ==================================================================

    @Transactional
    public ResponseEntity<?> create(CreateDevisDTO dto, Integer currentUserId) {
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
        // Comme une dépense : sans mandat de gestion actif, rien ne pourrait être retenu sur des reversements
        ContratMandat mandat = mandatRepository
                .findFirstByCour_IdCourAndStatutAndTypeMandat(cour.getIdCour(), StatutMandat.ACTIF, TypeMandat.GESTION)
                .orElseThrow(() -> new IllegalStateException(
                        "Aucun mandat de gestion actif sur cette cour : un devis ne peut pas y être soumis"));

        int nombreMois = dto.getNombreMois() != null ? dto.getNombreMois() : 1;
        verifierEtalement(dto.getMontant(), nombreMois);

        DevisTravaux devis = DevisTravaux.builder()
                .mandat(mandat)
                .cour(cour)
                .maison(maison)
                .libelle(dto.getLibelle().trim())
                .categorie(dto.getCategorie())
                .description(dto.getDescription())
                .prestataire(dto.getPrestataire())
                .montant(dto.getMontant())
                .nombreMois(nombreMois)
                .statut(StatutDevis.EN_ATTENTE)
                .userCreate(currentUserId)
                .build();
        devisRepository.save(devis);
        alerteUtilisateurService.devisSoumis(devis);
        return buildSuccessResponse(HttpStatus.CREATED, "Devis soumis au bailleur", "DEVIS_CREATED", toDto(devis));
    }

    /**
     * Avant la réponse du bailleur : simple correction. Après une demande de renégociation ou une expiration
     * sans réponse : le devis révisé repart en attente de réponse (nouveau délai) et le bailleur en est prévenu.
     */
    @Transactional
    public ResponseEntity<?> update(Integer id, UpdateDevisDTO dto, Integer currentUserId) {
        DevisTravaux devis = findOrThrow(id);
        if (devis.getStatut() != StatutDevis.EN_ATTENTE && devis.getStatut() != StatutDevis.A_RENEGOCIER
                && devis.getStatut() != StatutDevis.EXPIRE) {
            throw new IllegalStateException("Ce devis est " + libelle(devis.getStatut()) + " : il ne peut plus être modifié");
        }
        if (devis.getMandat().getStatut() != StatutMandat.ACTIF) {
            throw new IllegalStateException("Le mandat de gestion de cette cour n'est plus actif : ce devis ne peut plus être soumis");
        }

        if (dto.getLibelle() != null && !dto.getLibelle().isBlank()) devis.setLibelle(dto.getLibelle().trim());
        if (dto.getCategorie() != null) devis.setCategorie(dto.getCategorie());
        if (dto.getDescription() != null) devis.setDescription(dto.getDescription());
        if (dto.getPrestataire() != null) devis.setPrestataire(dto.getPrestataire());
        if (dto.getMontant() != null) devis.setMontant(dto.getMontant());
        if (dto.getNombreMois() != null) devis.setNombreMois(dto.getNombreMois());
        verifierEtalement(devis.getMontant(), devis.getNombreMois());

        boolean revision = devis.getStatut() == StatutDevis.A_RENEGOCIER || devis.getStatut() == StatutDevis.EXPIRE;
        if (revision) {
            devis.setStatut(StatutDevis.EN_ATTENTE);
            devis.setNombreSoumissions(devis.getNombreSoumissions() + 1);
            devis.setDateSoumission(LocalDateTime.now());
        }
        devis.setUserUpdate(currentUserId);
        devisRepository.save(devis);
        if (revision) alerteUtilisateurService.devisSoumis(devis);
        return buildSuccessResponse(HttpStatus.OK, revision ? "Devis révisé et soumis à nouveau au bailleur" : "Devis modifié",
                "DEVIS_UPDATED", toDto(devis));
    }

    /** Retrait par l'agence tant que la dépense n'est pas enregistrée (y compris après l'accord du bailleur). */
    @Transactional
    public ResponseEntity<?> annuler(Integer id, ReponseDevisDTO dto, Integer currentUserId) {
        DevisTravaux devis = findOrThrow(id);
        if (!STATUTS_OUVERTS.contains(devis.getStatut())) {
            throw new IllegalStateException("Ce devis est " + libelle(devis.getStatut()) + " : il ne peut plus être retiré");
        }
        boolean etaitAccepte = devis.getStatut() == StatutDevis.ACCEPTE;
        devis.setStatut(StatutDevis.ANNULE);
        devis.setMotifAnnulation(dto.getCommentaire().trim());
        devis.setUserUpdate(currentUserId);
        devisRepository.save(devis);
        if (!etaitAccepte) pieceJointeService.supprimerSiPresent(devis.getIdDevis());
        alerteUtilisateurService.devisAnnule(devis);
        return buildSuccessResponse(HttpStatus.OK, "Devis retiré", "DEVIS_CANCELLED", toDto(devis));
    }

    /**
     * Le bailleur a donné son accord en dehors de la plateforme (téléphone, en personne) : l'agent l'enregistre en précisant
     * qui, quand et comment. Le devis passe à « accepté », le bailleur en est prévenu et peut le contester tant que la
     * dépense n'est pas enregistrée ({@link #contesterAccord}).
     */
    @Transactional
    public ResponseEntity<?> enregistrerAccordHorsPlateforme(Integer id, AccordHorsPlateformeDTO dto, Integer currentUserId) {
        DevisTravaux devis = findOrThrow(id);
        if (devis.getStatut() != StatutDevis.EN_ATTENTE && devis.getStatut() != StatutDevis.A_RENEGOCIER
                && devis.getStatut() != StatutDevis.EXPIRE) {
            throw new IllegalStateException("Ce devis est " + libelle(devis.getStatut()) + " : un accord ne peut plus y être enregistré");
        }
        if (devis.getMandat().getStatut() != StatutMandat.ACTIF) {
            throw new IllegalStateException("Le mandat de gestion de cette cour n'est plus actif : ce devis ne peut plus être accepté");
        }
        devis.setStatut(StatutDevis.ACCEPTE);
        devis.setAccordHorsPlateforme(true);
        devis.setAccordMotif(dto.getMotif().trim());
        devis.setAccordDate(dto.getDateAccord());
        devis.setAccordUser(currentUserId);
        devis.setCommentaireBailleur(null);
        devis.setDateReponse(LocalDateTime.now());
        devis.setUserUpdate(currentUserId);
        devisRepository.save(devis);
        alerteUtilisateurService.devisAccordHorsPlateforme(devis);
        return buildSuccessResponse(HttpStatus.OK, "Accord du bailleur enregistré", "DEVIS_ACCEPTED_OFFLINE", toDto(devis));
    }

    /** Devis accepté : la dépense est enregistrée, au montant accepté ou moins si la facture finale est inférieure. */
    @Transactional
    public ResponseEntity<?> enregistrerDepense(Integer id, DepenseDepuisDevisDTO dto, Integer currentUserId) {
        DevisTravaux devis = findOrThrow(id);
        if (devis.getStatut() != StatutDevis.ACCEPTE) {
            throw new IllegalStateException(devis.getStatut() == StatutDevis.REALISE
                    ? "La dépense de ce devis a déjà été enregistrée"
                    : "Ce devis est " + libelle(devis.getStatut()) + " : la dépense ne peut être enregistrée qu'après l'accord du bailleur");
        }
        BigDecimal montant = dto != null && dto.getMontant() != null ? dto.getMontant() : devis.getMontant();
        if (montant.compareTo(devis.getMontant()) > 0) {
            throw new IllegalStateException("Le montant ne peut pas dépasser celui du devis accepté par le bailleur ("
                    + devis.getMontant().stripTrailingZeros().toPlainString() + ") : soumettez un nouveau devis pour le supplément");
        }
        LocalDate dateDepense = dto != null && dto.getDateDepense() != null ? dto.getDateDepense() : LocalDate.now();

        DepenseBien depense = depenseService.creerDepuisDevis(devis, dateDepense, montant, currentUserId);
        devis.setStatut(StatutDevis.REALISE);
        devis.setUserUpdate(currentUserId);
        devisRepository.save(devis);
        return buildSuccessResponse(HttpStatus.CREATED, "Dépense enregistrée à partir du devis", "DEVIS_DEPENSE_CREATED",
                toDto(devis, depense.getIdDepense()));
    }

    // ==================================================================
    // Bailleur : accepter, demander une renégociation, rejeter
    // ==================================================================

    @Transactional
    public ResponseEntity<?> accepter(Integer id, AccepterDevisDTO dto, Integer currentUserId) {
        DevisTravaux devis = pourReponse(id, currentUserId);
        // L'accord vaut pour ce que le bailleur a sous les yeux : si l'agence a corrigé le devis entre-temps, il le relit
        if (dto.getMontant().compareTo(devis.getMontant()) != 0 || !dto.getNombreMois().equals(devis.getNombreMois())) {
            throw new IllegalStateException("Ce devis a été modifié par l'agence depuis son affichage : rechargez la page avant de répondre");
        }
        repondre(devis, StatutDevis.ACCEPTE, null);
        return buildSuccessResponse(HttpStatus.OK, "Devis accepté", "DEVIS_ACCEPTED", toDto(devis));
    }

    @Transactional
    public ResponseEntity<?> renegocier(Integer id, ReponseDevisDTO dto, Integer currentUserId) {
        DevisTravaux devis = pourReponse(id, currentUserId);
        repondre(devis, StatutDevis.A_RENEGOCIER, dto.getCommentaire().trim());
        return buildSuccessResponse(HttpStatus.OK, "Demande de renégociation transmise à l'agence", "DEVIS_RENEGOTIATION", toDto(devis));
    }

    @Transactional
    public ResponseEntity<?> rejeter(Integer id, ReponseDevisDTO dto, Integer currentUserId) {
        DevisTravaux devis = pourReponse(id, currentUserId);
        repondre(devis, StatutDevis.REJETE, dto.getCommentaire().trim());
        return buildSuccessResponse(HttpStatus.OK, "Devis rejeté", "DEVIS_REJECTED", toDto(devis));
    }

    /**
     * Le bailleur ne reconnaît pas l'accord que l'agence a enregistré hors plateforme : le devis repart en renégociation
     * (l'agence devra le soumettre à nouveau et obtenir une vraie réponse). Impossible une fois la dépense enregistrée.
     */
    @Transactional
    public ResponseEntity<?> contesterAccord(Integer id, ReponseDevisDTO dto, Integer currentUserId) {
        DevisTravaux devis = findOrThrow(id);
        if (!estProprietaire(devis, currentUserId)) {
            throw new AccessDeniedException("Seul le bailleur de la cour peut contester cet accord");
        }
        if (devis.getStatut() != StatutDevis.ACCEPTE || !Boolean.TRUE.equals(devis.getAccordHorsPlateforme())) {
            throw new IllegalStateException("Seul un accord enregistré par l'agence hors plateforme peut être contesté, "
                    + "tant que la dépense n'est pas enregistrée");
        }
        devis.setAccordHorsPlateforme(false);
        repondre(devis, StatutDevis.A_RENEGOCIER, "Accord enregistré hors plateforme contesté : " + dto.getCommentaire().trim());
        return buildSuccessResponse(HttpStatus.OK, "Accord contesté : le devis repart en renégociation", "DEVIS_OFFLINE_CONTESTED", toDto(devis));
    }

    // ==================================================================
    // Expiration et résiliation
    // ==================================================================

    /**
     * Job quotidien : l'agent est rappelé qu'un devis accepté (sur la plateforme ou hors plateforme) n'a toujours pas donné
     * lieu à une dépense, au bout de DELAI_RAPPEL_DEVIS_ACCEPTE_JOURS puis à chaque période écoulée. Le devis n'expire pas :
     * l'accord du bailleur reste valable tant que l'agence ne le retire pas.
     *
     * @return le nombre de rappels créés
     */
    @Transactional
    public int rappelerDevisAcceptesNonRealises() {
        int delai = Math.max(1, parametreService.getEntier(CleParametre.DELAI_RAPPEL_DEVIS_ACCEPTE_JOURS, 30));
        LocalDateTime maintenant = LocalDateTime.now();
        int rappels = 0;
        for (DevisTravaux devis : devisRepository.findByStatutAndDateReponseBefore(StatutDevis.ACCEPTE, maintenant.minusDays(delai))) {
            long jours = java.time.temporal.ChronoUnit.DAYS.between(devis.getDateReponse(), maintenant);
            if (alerteAgentService.devisAccepteNonRealise(devis, jours, jours / delai)) rappels++;
        }
        return rappels;
    }

    /**
     * Job quotidien : un devis en attente depuis plus de DELAI_REPONSE_DEVIS_JOURS (30 par défaut) expire ; le
     * bailleur est relancé une fois, quelques jours avant (7, ou la moitié du délai s'il est plus court).
     * Le délai repart de zéro à chaque nouvelle soumission.
     *
     * @return le nombre de devis expirés
     */
    @Transactional
    public int expirerDevisSansReponse() {
        int delai = delaiReponseJours();
        LocalDateTime maintenant = LocalDateTime.now();
        int expires = 0;
        for (DevisTravaux devis : devisRepository.findByStatutAndDateSoumissionBefore(StatutDevis.EN_ATTENTE, maintenant.minusDays(delai))) {
            devis.setStatut(StatutDevis.EXPIRE);
            devisRepository.save(devis);
            alerteUtilisateurService.devisExpire(devis);
            alerteAgentService.devisExpire(devis);
            expires++;
        }
        int preavis = Math.min(JOURS_RAPPEL_AVANT_EXPIRATION, delai / 2);
        if (preavis > 0) {
            for (DevisTravaux devis : devisRepository.findByStatutAndDateSoumissionBefore(StatutDevis.EN_ATTENTE, maintenant.minusDays(delai - preavis))) {
                alerteUtilisateurService.devisRappel(devis, devis.getDateSoumission().plusDays(delai));
            }
        }
        return expires;
    }

    /** Résiliation du mandat : les devis encore ouverts sont retirés (aucune dépense ne peut plus être retenue). */
    @Transactional
    public void retirerApresResiliation(ContratMandat mandat) {
        for (DevisTravaux devis : devisRepository.findByMandat_IdMandatAndStatutIn(mandat.getIdMandat(), STATUTS_OUVERTS)) {
            boolean attenduParLeBailleur = devis.getStatut() != StatutDevis.EXPIRE;
            boolean etaitAccepte = devis.getStatut() == StatutDevis.ACCEPTE;
            devis.setStatut(StatutDevis.ANNULE);
            devis.setMotifAnnulation("Mandat résilié");
            devisRepository.save(devis);
            if (!etaitAccepte) pieceJointeService.supprimerSiPresent(devis.getIdDevis());
            if (attenduParLeBailleur) alerteUtilisateurService.devisAnnule(devis);
        }
    }

    // ==================================================================
    // Internes
    // ==================================================================

    private static final int JOURS_RAPPEL_AVANT_EXPIRATION = 7;

    /** Devis qui peuvent encore aboutir à une dépense (ou être soumis à nouveau) : l'agence peut les retirer. */
    private static final List<StatutDevis> STATUTS_OUVERTS =
            List.of(StatutDevis.EN_ATTENTE, StatutDevis.A_RENEGOCIER, StatutDevis.ACCEPTE, StatutDevis.EXPIRE);

    private int delaiReponseJours() {
        return Math.max(1, parametreService.getEntier(CleParametre.DELAI_REPONSE_DEVIS_JOURS, 30));
    }

    /** Seul le propriétaire de la cour répond, et seulement à un devis en attente de sa réponse. */
    private DevisTravaux pourReponse(Integer id, Integer currentUserId) {
        DevisTravaux devis = findOrThrow(id);
        if (!estProprietaire(devis, currentUserId)) {
            throw new AccessDeniedException("Seul le bailleur de la cour peut répondre à ce devis");
        }
        if (devis.getStatut() != StatutDevis.EN_ATTENTE) {
            throw new IllegalStateException("Ce devis est " + libelle(devis.getStatut()) + " : il n'attend plus de réponse");
        }
        return devis;
    }

    private void repondre(DevisTravaux devis, StatutDevis statut, String commentaire) {
        devis.setStatut(statut);
        devis.setCommentaireBailleur(commentaire);
        devis.setDateReponse(LocalDateTime.now());
        devisRepository.save(devis);
        alerteAgentService.devisRepondu(devis);
    }

    private void verifierEtalement(BigDecimal montant, int nombreMois) {
        if (montant.compareTo(BigDecimal.valueOf(nombreMois)) < 0) {
            throw new IllegalArgumentException("Le montant est trop faible pour être étalé sur " + nombreMois + " mois");
        }
    }

    private boolean estProprietaire(DevisTravaux devis, Integer currentUserId) {
        return devis.getCour().getProprietaire() != null && devis.getCour().getProprietaire().getIdUser().equals(currentUserId);
    }

    private static String libelle(StatutDevis statut) {
        return switch (statut) {
            case EN_ATTENTE -> "en attente de réponse";
            case A_RENEGOCIER -> "à renégocier";
            case ACCEPTE -> "accepté";
            case REJETE -> "rejeté";
            case ANNULE -> "retiré";
            case REALISE -> "réalisé";
            case EXPIRE -> "expiré";
        };
    }

    private DevisTravaux findOrThrow(Integer id) {
        return devisRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("devis", id));
    }

    private DevisResponseDTO toDto(DevisTravaux d) {
        Integer idDepense = d.getStatut() == StatutDevis.REALISE
                ? depenseRepository.findFirstByDevis_IdDevis(d.getIdDevis()).map(DepenseBien::getIdDepense).orElse(null)
                : null;
        return toDto(d, idDepense);
    }

    private DevisResponseDTO toDto(DevisTravaux d, Integer idDepense) {
        Cour cour = d.getCour();
        return DevisResponseDTO.builder()
                .idDevis(d.getIdDevis())
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
                .montant(d.getMontant())
                .nombreMois(d.getNombreMois())
                .mensualite(d.getMontant().divide(BigDecimal.valueOf(d.getNombreMois()), 2, RoundingMode.HALF_UP))
                .plafondDepenseSansAccord(d.getMandat().getPlafondDepenseSansAccord())
                .statut(d.getStatut())
                .commentaireBailleur(d.getCommentaireBailleur())
                .dateReponse(d.getDateReponse())
                .nombreSoumissions(d.getNombreSoumissions())
                .motifAnnulation(d.getMotifAnnulation())
                .dateSoumission(d.getDateSoumission())
                .dateLimiteReponse(d.getStatut() == StatutDevis.EN_ATTENTE ? d.getDateSoumission().plusDays(delaiReponseJours()) : null)
                .idDepense(idDepense)
                .accordHorsPlateforme(Boolean.TRUE.equals(d.getAccordHorsPlateforme()))
                .accordMotif(Boolean.TRUE.equals(d.getAccordHorsPlateforme()) ? d.getAccordMotif() : null)
                .accordDate(Boolean.TRUE.equals(d.getAccordHorsPlateforme()) ? d.getAccordDate() : null)
                .pieceJointe(documentRepository.existsByTypeDocumentAndEntiteTypeAndEntiteId(
                        TypeDocument.DEVIS_PIECE_JOINTE, TypeEntiteDocument.DEVIS_TRAVAUX, d.getIdDevis()))
                .build();
    }
}
