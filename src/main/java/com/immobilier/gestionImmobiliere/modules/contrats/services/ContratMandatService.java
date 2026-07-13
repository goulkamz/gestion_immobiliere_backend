package com.immobilier.gestionImmobiliere.modules.contrats.services;

import com.immobilier.gestionImmobiliere.donnees.biens.model.Cour;
import com.immobilier.gestionImmobiliere.donnees.biens.repository.CourRepository;
import com.immobilier.gestionImmobiliere.donnees.contrats.model.ContratMandat;
import com.immobilier.gestionImmobiliere.donnees.contrats.model.StatutMandat;
import com.immobilier.gestionImmobiliere.donnees.contrats.repository.ContratMandatRepository;
import com.immobilier.gestionImmobiliere.donnees.user.model.User;
import com.immobilier.gestionImmobiliere.donnees.user.repository.UserRepository;
import com.immobilier.gestionImmobiliere.exceptions.InvalidStatutTransitionException;
import com.immobilier.gestionImmobiliere.exceptions.MandatActifExistantException;
import com.immobilier.gestionImmobiliere.exceptions.ResourceNotFoundException;
import com.immobilier.gestionImmobiliere.modules.contrats.dto.requests.CreateContratMandatDTO;
import com.immobilier.gestionImmobiliere.modules.contrats.dto.requests.ResilierMandatDTO;
import com.immobilier.gestionImmobiliere.modules.contrats.dto.responses.ContratMandatResponseDTO;
import com.immobilier.gestionImmobiliere.modules.journal.services.JournalService;
import com.immobilier.gestionImmobiliere.modules.paiements.services.EcheanceGenerationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static com.immobilier.gestionImmobiliere.utils.BuildSuccessResponse.buildSuccessResponse;

@Service
public class ContratMandatService {

    private final ContratMandatRepository mandatRepository;
    private final CourRepository courRepository;
    private final UserRepository userRepository;
    private final EcheanceGenerationService echeanceGenerationService;
    private final JournalService journalService;

    public ContratMandatService(ContratMandatRepository mandatRepository, CourRepository courRepository, UserRepository userRepository, EcheanceGenerationService echeanceGenerationService, JournalService journalService) {
        this.mandatRepository = mandatRepository;
        this.courRepository = courRepository;
        this.userRepository = userRepository;
        this.echeanceGenerationService = echeanceGenerationService;
        this.journalService = journalService;
    }

    public ResponseEntity<?> getAll(Integer idCour, StatutMandat statut, Pageable pageable) {
        Page<ContratMandat> page = idCour != null
                ? mandatRepository.findByCour_IdCour(idCour, pageable)
                : (statut != null ? mandatRepository.findByStatut(statut, pageable) : mandatRepository.findAll(pageable));
        return buildSuccessResponse(HttpStatus.OK, "Liste des mandats", "MANDAT_LIST", page.map(this::toDto));
    }

    public ResponseEntity<?> getById(Integer id) {
        return buildSuccessResponse(HttpStatus.OK, "Mandat trouvé", "MANDAT_FOUND", toDto(findOrThrow(id)));
    }

    @Transactional
    public ResponseEntity<?> create(CreateContratMandatDTO dto,Integer currentUserId) {
        Cour cour = courRepository.findById(dto.getIdCour())
                .orElseThrow(() -> new ResourceNotFoundException("cour", dto.getIdCour()));
        User agent = userRepository.findById(dto.getIdAgent())
                .orElseThrow(() -> new ResourceNotFoundException("agent", dto.getIdAgent()));

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

        journalService.enregistrer(currentUserId, "CREATION", "contrat_mandat", mandat.getIdMandat(),
                "Création du mandat pour la cour id " + cour.getIdCour(), null, "statut=EN_ATTENTE");

        return buildSuccessResponse(HttpStatus.CREATED, "Mandat créé (en attente d'activation)", "MANDAT_CREATED", toDto(mandat));
    }

    @Transactional
    public ResponseEntity<?> activer(Integer id, Integer currentUserId) {
        ContratMandat mandat = findOrThrow(id);
        String ancienStatut = mandat.getStatut().name();

        if (mandat.getStatut() != StatutMandat.EN_ATTENTE) {
            throw new InvalidStatutTransitionException(mandat.getStatut().name(), StatutMandat.ACTIF.name());
        }
        // RG F13 — un seul mandat actif par cour
        if (mandatRepository.existsByCour_IdCourAndStatut(mandat.getCour().getIdCour(), StatutMandat.ACTIF)) {
            throw new MandatActifExistantException(mandat.getCour().getIdCour());
        }

        mandat.setStatut(StatutMandat.ACTIF);
        mandatRepository.save(mandat);
        echeanceGenerationService.genererEcheancesMandat(mandat);

        journalService.enregistrer(currentUserId, "ACTIVATION", "contrat_mandat", mandat.getIdMandat(),
                "Activation du mandat", "statut=" + ancienStatut, "statut=ACTIF");

        return buildSuccessResponse(HttpStatus.OK, "Mandat activé", "MANDAT_ACTIVATED", toDto(mandat));
    }

    @Transactional
    public ResponseEntity<?> resilier(Integer id, ResilierMandatDTO dto,Integer currentUserId) {
        ContratMandat mandat = findOrThrow(id);
        String ancienStatut = mandat.getStatut().name();

        if (mandat.getStatut() != StatutMandat.ACTIF) {
            throw new InvalidStatutTransitionException(mandat.getStatut().name(), StatutMandat.RESILIE.name());
        }

        mandat.setStatut(StatutMandat.RESILIE);
        mandat.setMotifResiliation(dto.getMotifResiliation());
        mandat.setDateResiliation(LocalDateTime.now());
        mandatRepository.save(mandat);
        journalService.enregistrer(currentUserId, "RESILIATION", "contrat_mandat", mandat.getIdMandat(),
                "Résiliation du mandat, motif : " + dto.getMotifResiliation(),
                "statut=" + ancienStatut, "statut=RESILIE");
        return buildSuccessResponse(HttpStatus.OK, "Mandat résilié", "MANDAT_RESILIE", toDto(mandat));
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
                .referenceCours(m.getCour().getReferenceCours())
                .idAgent(m.getAgent().getIdUser())
                .nomAgent(m.getAgent().getNom() + " " + m.getAgent().getPrenom())
                .dateDebut(m.getDateDebut())
                .dateFin(m.getDateFin())
                .typeMandat(m.getTypeMandat())
                .commission(m.getCommission())
                .statut(m.getStatut())
                .motifResiliation(m.getMotifResiliation())
                .build();
    }
}