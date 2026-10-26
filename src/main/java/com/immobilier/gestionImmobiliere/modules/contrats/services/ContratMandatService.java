package com.immobilier.gestionImmobiliere.modules.contrats.services;

import com.immobilier.gestionImmobiliere.utils.VilleSpecification;
import com.immobilier.gestionImmobiliere.modules.notifications.services.AlerteUtilisateurService;
import com.immobilier.gestionImmobiliere.donnees.contrats.model.Avenant;
import com.immobilier.gestionImmobiliere.modules.documents.services.ContratMandatDocumentService;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.immobilier.gestionImmobiliere.donnees.biens.model.Cour;
import com.immobilier.gestionImmobiliere.donnees.biens.model.Maison;
import com.immobilier.gestionImmobiliere.donnees.biens.model.StatutMaison;
import com.immobilier.gestionImmobiliere.donnees.biens.repository.CourRepository;
import com.immobilier.gestionImmobiliere.donnees.contrats.model.ContratLocation;
import com.immobilier.gestionImmobiliere.donnees.contrats.model.ContratMandat;
import com.immobilier.gestionImmobiliere.donnees.contrats.model.StatutLocation;
import com.immobilier.gestionImmobiliere.donnees.contrats.model.ModeFacturation;
import com.immobilier.gestionImmobiliere.donnees.contrats.repository.ContratLocationRepository;
import com.immobilier.gestionImmobiliere.donnees.contrats.model.StatutMandat;
import com.immobilier.gestionImmobiliere.donnees.contrats.model.TypeMandat;
import com.immobilier.gestionImmobiliere.donnees.contrats.repository.ContratMandatRepository;
import com.immobilier.gestionImmobiliere.donnees.paiements.model.TypeEcheance;
import com.immobilier.gestionImmobiliere.donnees.user.model.User;
import com.immobilier.gestionImmobiliere.donnees.user.repository.UserRepository;
import com.immobilier.gestionImmobiliere.exceptions.InvalidStatutTransitionException;
import com.immobilier.gestionImmobiliere.exceptions.MandatActifExistantException;
import com.immobilier.gestionImmobiliere.exceptions.ResourceNotFoundException;
import com.immobilier.gestionImmobiliere.modules.contrats.dto.requests.CreateContratMandatDTO;
import com.immobilier.gestionImmobiliere.modules.contrats.dto.requests.ResilierMandatDTO;
import com.immobilier.gestionImmobiliere.modules.contrats.dto.requests.UpdateContratMandatDTO;

import com.immobilier.gestionImmobiliere.modules.contrats.dto.responses.ContratMandatResponseDTO;
import com.immobilier.gestionImmobiliere.modules.paiements.services.EcheanceService;
import com.immobilier.gestionImmobiliere.modules.user.jwtService.UserDetailsImpl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static com.immobilier.gestionImmobiliere.utils.BuildSuccessResponse.buildSuccessResponse;

@Service
public class ContratMandatService {

    private final ContratMandatRepository mandatRepository;
    private final CourRepository courRepository;
    private final UserRepository userRepository;
    private final EcheanceService echeanceService;
    private final ContratLocationRepository contratLocationRepository;
    private final AvenantService avenantService;
    private final AlerteUtilisateurService alerteUtilisateurService;
    private final ContratMandatDocumentService contratMandatDocumentService;


    public ContratMandatService(ContratMandatRepository mandatRepository, CourRepository courRepository, UserRepository userRepository, EcheanceService echeanceService, ContratLocationRepository contratLocationRepository, AvenantService avenantService, ContratMandatDocumentService contratMandatDocumentService, AlerteUtilisateurService alerteUtilisateurService) {
        this.mandatRepository = mandatRepository;
        this.courRepository = courRepository;
        this.userRepository = userRepository;
        this.echeanceService = echeanceService;
        this.contratLocationRepository = contratLocationRepository;
        this.avenantService = avenantService;
        this.alerteUtilisateurService = alerteUtilisateurService;
        this.contratMandatDocumentService = contratMandatDocumentService;
    }

    public ResponseEntity<?> getAllForCurrentUser(Integer idCour, StatutMandat statut, String recherche,
                                                  Integer currentUserId, boolean isAdminOrAgent,
                                                  Integer idVille, Boolean sansVille, Pageable pageable) {
        // BAILLEUR : uniquement les mandats sur ses propres cours
        Page<ContratMandat> page = mandatRepository.findAll(
                filtrer(idCour, statut, recherche, isAdminOrAgent ? null : currentUserId).and(VilleSpecification.parVille(idVille, sansVille, "cour", "secteur", "ville")), pageable);

        return buildSuccessResponse(HttpStatus.OK, "Liste des mandats", "MANDAT_LIST", page.map(this::toDto));
    }

    // Recherche insensible à la casse sur la référence ou le lot de la cour, ou le nom du bailleur
    private Specification<ContratMandat> filtrer(Integer idCour, StatutMandat statut, String recherche, Integer idProprietaire) {
        return (root, query, cb) -> {
            List<Predicate> predicats = new ArrayList<>();
            if (idCour != null) {
                predicats.add(cb.equal(root.get("cour").get("idCour"), idCour));
            }
            if (statut != null) {
                predicats.add(cb.equal(root.get("statut"), statut));
            }
            if (idProprietaire != null) {
                predicats.add(cb.equal(root.get("cour").get("proprietaire").get("idUser"), idProprietaire));
            }
            if (recherche != null && !recherche.isBlank()) {
                String motif = "%" + recherche.trim().toLowerCase() + "%";
                Join<Object, Object> cour = root.join("cour");
                Join<Object, Object> proprietaire = cour.join("proprietaire");
                predicats.add(cb.or(
                        cb.like(cb.lower(cour.<String>get("referenceCour")), motif),
                        cb.like(cb.lower(cour.<String>get("lotCour")), motif),
                        cb.like(cb.lower(cb.concat(cb.concat(proprietaire.<String>get("nom"), " "), proprietaire.<String>get("prenom"))), motif)));
            }
            return cb.and(predicats.toArray(new Predicate[0]));
        };
    }

    public ResponseEntity<?> getByIdForCurrentUser(Integer id, Integer currentUserId, boolean isAdminOrAgent) {
        ContratMandat mandat = findOrThrow(id);

        if (!isAdminOrAgent) {
            boolean autorise = mandat.getCour().getProprietaire().getIdUser().equals(currentUserId);
            if (!autorise) throw new AccessDeniedException("Vous n'avez pas accès à ce mandat");
        }

        return buildSuccessResponse(HttpStatus.OK, "Mandat trouvé", "MANDAT_FOUND", toDto(mandat));
    }
    @Transactional
    public ResponseEntity<?> create(CreateContratMandatDTO dto,Integer currentUserId) {
        if (dto.getDateFin() != null && !dto.getDateFin().isAfter(dto.getDateDebut())) {
            throw new IllegalArgumentException("La date de fin doit être postérieure à la date de début");
        }
        validerModeFacturation(dto.getTypeMandat(), dto.getModeFacturation());
        Cour cour = courRepository.findById(dto.getIdCour())
                .orElseThrow(() -> new ResourceNotFoundException("cour", dto.getIdCour()));
        User agent = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("agent", currentUserId));

        ContratMandat mandat = ContratMandat.builder()
                .cour(cour)
                .agent(agent)
                .dateDebut(dto.getDateDebut())
                .dateFin(dto.getDateFin())
                .typeMandat(dto.getTypeMandat())
                .commission(dto.getCommission())
                .modeFacturation(dto.getModeFacturation())
                .statut(StatutMandat.EN_ATTENTE)
                .build();
        mandatRepository.save(mandat);
        return buildSuccessResponse(HttpStatus.CREATED, "Mandat créé (en attente d'activation)", "MANDAT_CREATED", toDto(mandat));
    }

    @Transactional
    public ResponseEntity<?> activer(Integer id, UserDetailsImpl currentUser) {
        ContratMandat mandat = findOrThrow(id);

        if (mandat.getStatut() != StatutMandat.EN_ATTENTE) {
            throw new InvalidStatutTransitionException(mandat.getStatut().name(), StatutMandat.ACTIF.name());
        }
        // RG F13 — un seul mandat actif par cour
        if (mandatRepository.existsByCour_IdCourAndStatut(mandat.getCour().getIdCour(), StatutMandat.ACTIF)) {
            throw new MandatActifExistantException(mandat.getCour().getIdCour());
        }

        User agent = userRepository.findById(currentUser.getIdUser())
                .orElseThrow(() -> new ResourceNotFoundException("agent", currentUser.getIdUser()));

        mandat.setStatut(StatutMandat.ACTIF);
        mandat.setAgent(agent);
        mandatRepository.save(mandat);
        alerteUtilisateurService.mandatActive(mandat);
        return buildSuccessResponse(HttpStatus.OK, "Mandat activé", "MANDAT_ACTIVATED", toDto(mandat));
    }


    @Transactional
    public ResponseEntity<?> resilier(Integer id, ResilierMandatDTO dto,Integer currentUserId) {
        ContratMandat mandat = findOrThrow(id);

        if (mandat.getStatut() != StatutMandat.ACTIF) {
            throw new InvalidStatutTransitionException(mandat.getStatut().name(), StatutMandat.RESILIE.name());
        }

        // Tant qu'une maison de la cour est louée, le mandat ne peut pas être résilié : on met d'abord fin aux locations
        if (contratLocationRepository.existsByMaison_Cour_IdCourAndStatut(mandat.getCour().getIdCour(), StatutLocation.ACTIF)) {
            throw new IllegalStateException("Ce mandat ne peut pas être résilié : au moins une maison de la cour a un contrat de location actif");
        }

        User agent = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("agent", currentUserId));


        mandat.setStatut(StatutMandat.RESILIE);
        mandat.setAgent(agent);
        mandat.setMotifResiliation(dto.getMotifResiliation());
        mandat.setDateResiliation(LocalDateTime.now());
        mandatRepository.save(mandat);

        // Les reversements déjà calculés au-delà du mois de résiliation n'ont plus lieu d'être (celui qui paie le mois de résiliation reste)
        echeanceService.supprimerReversementsFuturs(mandat.getIdMandat(), LocalDate.now());
        alerteUtilisateurService.mandatResilie(mandat);

        return buildSuccessResponse(HttpStatus.OK, "Mandat résilié", "MANDAT_RESILIE", toDto(mandat));
    }

    /**
     * Modifie dateFin/commission/modeFacturation/typeMandat sans changer le statut.
     * Réservé aux mandats EN_ATTENTE/ACTIF : un mandat RESILIE/EXPIRE est figé.
     *
     * EN_ATTENTE : le PDF éventuellement déjà généré est un brouillon, il est supprimé et se
     * régénérera avec les nouvelles valeurs. ACTIF : le contrat d'origine reste figé (son PDF est
     * généré avant la modification) et un avenant numéroté enregistre ce qui change ; motif et accord
     * du client sont obligatoires. La modification s'applique dès l'enregistrement : la date d'effet ne
     * peut donc être qu'aujourd'hui.
     */
    @Transactional
    public ResponseEntity<?> modifier(Integer id, UpdateContratMandatDTO dto, Integer currentUserId) {
        ContratMandat mandat = findOrThrow(id);

        if (mandat.getStatut() != StatutMandat.EN_ATTENTE && mandat.getStatut() != StatutMandat.ACTIF) {
            throw new InvalidStatutTransitionException(mandat.getStatut().name(), "MODIFICATION_MANDAT");
        }

        List<AvenantService.Modification> modifications = new ArrayList<>();
        if (dto.getDateFin() != null) {
            if (!dto.getDateFin().isAfter(mandat.getDateDebut())) {
                throw new IllegalArgumentException("La date de fin doit être postérieure à la date de début");
            }
            if (!dto.getDateFin().equals(mandat.getDateFin())) {
                modifications.add(new AvenantService.Modification("dateFin",
                        mandat.getDateFin() != null ? mandat.getDateFin().toString() : null, dto.getDateFin().toString()));
            }
        }
        TypeMandat type = dto.getTypeMandat() != null ? dto.getTypeMandat() : mandat.getTypeMandat();
        ModeFacturation mode = dto.getModeFacturation() != null ? dto.getModeFacturation() : mandat.getModeFacturation();
        if (dto.getTypeMandat() != null || dto.getModeFacturation() != null) {
            validerModeFacturation(type, mode);
        }
        if (dto.getTypeMandat() != null && dto.getTypeMandat() != mandat.getTypeMandat()) {
            modifications.add(new AvenantService.Modification("typeMandat",
                    mandat.getTypeMandat() != null ? mandat.getTypeMandat().name() : null, dto.getTypeMandat().name()));
        }
        if (dto.getModeFacturation() != null && dto.getModeFacturation() != mandat.getModeFacturation()) {
            modifications.add(new AvenantService.Modification("modeFacturation",
                    mandat.getModeFacturation() != null ? mandat.getModeFacturation().name() : null, dto.getModeFacturation().name()));
        }
        if (dto.getCommission() != null
                && (mandat.getCommission() == null || dto.getCommission().compareTo(mandat.getCommission()) != 0)) {
            modifications.add(new AvenantService.Modification("commission",
                    mandat.getCommission() != null ? mandat.getCommission().toPlainString() : null,
                    dto.getCommission().toPlainString()));
        }
        if (modifications.isEmpty()) {
            throw new IllegalArgumentException("Aucune modification à enregistrer");
        }

        if (mandat.getStatut() == StatutMandat.ACTIF) {
            if (dto.getModeFacturation() != null && dto.getModeFacturation() != mandat.getModeFacturation()
                    && echeanceService.mandatADesReversements(id)) {
                throw new IllegalStateException("Le mode de facturation ne peut plus changer : des reversements ont déjà été calculés pour ce mandat");
            }
            if (dto.getDateFin() != null && echeanceService.reversementApres(id, dto.getDateFin().toLocalDate())) {
                throw new IllegalStateException("La date de fin ne peut pas précéder un mois pour lequel un reversement a déjà été calculé");
            }
        }

        boolean actif = mandat.getStatut() == StatutMandat.ACTIF;
        LocalDate dateEffet = null;
        if (actif) {
            dateEffet = avenantService.verifierSaisie(dto.getMotif(), dto.getAccordClient(), dto.getDateEffet());
            if (dateEffet.isAfter(LocalDate.now())) {
                throw new IllegalArgumentException("Pour un mandat, la modification s'applique dès son enregistrement : "
                        + "la date d'effet ne peut pas être postérieure à aujourd'hui");
            }
            // Fige le contrat d'origine tel qu'il a été signé, avant d'appliquer quoi que ce soit
            contratMandatDocumentService.genererOuRecuperer(id, currentUserId);
        } else {
            contratMandatDocumentService.supprimerBrouillon(id);
        }

        if (dto.getDateFin() != null) mandat.setDateFin(dto.getDateFin());
        if (dto.getCommission() != null) mandat.setCommission(dto.getCommission());
        mandat.setModeFacturation(mode);
        mandat.setTypeMandat(type);

        mandatRepository.save(mandat);

        ContratMandatResponseDTO reponse = toDto(mandat);
        String message = "Mandat modifié";
        if (actif) {
            Avenant avenant = avenantService.enregistrerPourMandat(mandat, dateEffet, dto.getMotif(), modifications, currentUserId);
            alerteUtilisateurService.avenantCree(avenant);
            reponse.setIdAvenant(avenant.getIdAvenant());
            reponse.setNumeroAvenant(avenant.getNumeroOrdre());
            message = "Mandat modifié, avenant n°" + avenant.getNumeroOrdre() + " créé";
        }
        return buildSuccessResponse(HttpStatus.OK, message, "MANDAT_UPDATED", reponse);
    }

    /** Modes de facturation proposés pour chaque type de mandat. */
    public ResponseEntity<?> getModesFacturation() {
        Map<TypeMandat, List<ModeFacturation>> modes = new EnumMap<>(TypeMandat.class);
        for (TypeMandat type : TypeMandat.values()) {
            if (!type.isDisponible()) continue;
            modes.put(type, type.getModesAutorises());
        }
        return buildSuccessResponse(HttpStatus.OK, "Modes de facturation par type de mandat", "MANDAT_MODES_FACTURATION", modes);
    }

    private void validerModeFacturation(TypeMandat type, ModeFacturation mode) {
        if (!type.isDisponible()) {
            throw new IllegalArgumentException("Les mandats de type " + type + " ne sont pas disponibles pour le moment");
        }
        if (mode == null) {
            throw new IllegalArgumentException("Le mode de facturation est obligatoire pour un mandat " + type
                    + " (modes possibles : " + type.getModesAutorises() + ")");
        }
        if (!type.accepte(mode)) {
            throw new IllegalArgumentException("Le mode de facturation " + mode + " n'est pas autorisé pour un mandat "
                    + type + " (modes possibles : " + type.getModesAutorises() + ")");
        }
    }

    @Transactional
    public ResponseEntity<?> delete(Integer id) {
        mandatRepository.delete(findOrThrow(id));
        return buildSuccessResponse(HttpStatus.OK, "Mandat supprimé", "MANDAT_DELETED", null);
    }

    private ContratMandat findOrThrow(Integer id) {
        return mandatRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("mandat", id));
    }

    private ContratMandatResponseDTO toDto(ContratMandat m) {
        return ContratMandatResponseDTO.builder()
                .idMandat(m.getIdMandat())
                .idCour(m.getCour().getIdCour())
                .referenceCour(m.getCour().getReferenceCour())
                .nomProprietaire(m.getCour().getProprietaire().getNom() + " " + m.getCour().getProprietaire().getPrenom())
                .idAgent(m.getAgent().getIdUser())
                .nomAgent(m.getAgent().getNom() + " " + m.getAgent().getPrenom())
                .dateDebut(m.getDateDebut())
                .dateFin(m.getDateFin())
                .typeMandat(m.getTypeMandat())
                .commission(m.getCommission())
                .modeFacturation(m.getModeFacturation())
                .statut(m.getStatut())
                .motifResiliation(m.getMotifResiliation())
                .build();
    }
}