package com.immobilier.gestionImmobiliere.modules.contrats.services;

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


    public ContratMandatService(ContratMandatRepository mandatRepository, CourRepository courRepository, UserRepository userRepository, EcheanceService echeanceService, ContratLocationRepository contratLocationRepository) {
        this.mandatRepository = mandatRepository;
        this.courRepository = courRepository;
        this.userRepository = userRepository;
        this.echeanceService = echeanceService;
        this.contratLocationRepository = contratLocationRepository;
    }

    public ResponseEntity<?> getAllForCurrentUser(Integer idCour, StatutMandat statut, String recherche,
                                                  Integer currentUserId, boolean isAdminOrAgent,
                                                  Pageable pageable) {
        // BAILLEUR : uniquement les mandats sur ses propres cours
        Page<ContratMandat> page = mandatRepository.findAll(
                filtrer(idCour, statut, recherche, isAdminOrAgent ? null : currentUserId), pageable);

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

        //echeanceService.supprimerEcheancesFutures(mandat.getIdMandat(), LocalDate.now());

        return buildSuccessResponse(HttpStatus.OK, "Mandat résilié", "MANDAT_RESILIE", toDto(mandat));
    }

    /**
     * Modifie dateFin/commission/modeFacturation/typeMandat sans changer le statut.
     * Réservé aux mandats EN_ATTENTE/ACTIF : un mandat RESILIE/EXPIRE est figé.
     */
    @Transactional
    public ResponseEntity<?> modifier(Integer id, UpdateContratMandatDTO dto, Integer currentUserId) {
        ContratMandat mandat = findOrThrow(id);

        if (mandat.getStatut() != StatutMandat.EN_ATTENTE && mandat.getStatut() != StatutMandat.ACTIF) {
            throw new InvalidStatutTransitionException(mandat.getStatut().name(), "MODIFICATION_MANDAT");
        }

        if (dto.getDateFin() != null) {
            if (!dto.getDateFin().isAfter(mandat.getDateDebut())) {
                throw new IllegalArgumentException("La date de fin doit être postérieure à la date de début");
            }
            mandat.setDateFin(dto.getDateFin());
        }
        TypeMandat type = dto.getTypeMandat() != null ? dto.getTypeMandat() : mandat.getTypeMandat();
        ModeFacturation mode = dto.getModeFacturation() != null ? dto.getModeFacturation() : mandat.getModeFacturation();
        if (dto.getTypeMandat() != null || dto.getModeFacturation() != null) {
            validerModeFacturation(type, mode);
        }

        if (dto.getCommission() != null) mandat.setCommission(dto.getCommission());
        mandat.setModeFacturation(mode);
        mandat.setTypeMandat(type);

        mandatRepository.save(mandat);
        return buildSuccessResponse(HttpStatus.OK, "Mandat modifié", "MANDAT_UPDATED", toDto(mandat));
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