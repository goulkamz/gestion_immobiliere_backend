package com.immobilier.gestionImmobiliere.modules.biens.services;

import com.immobilier.gestionImmobiliere.donnees.biens.model.Cour;
import com.immobilier.gestionImmobiliere.donnees.biens.model.Maison;
import com.immobilier.gestionImmobiliere.donnees.biens.model.StatutMaison;
import com.immobilier.gestionImmobiliere.donnees.biens.repository.CourRepository;
import com.immobilier.gestionImmobiliere.donnees.biens.repository.MaisonRepository;
import com.immobilier.gestionImmobiliere.exceptions.InvalidStatutTransitionException;
import com.immobilier.gestionImmobiliere.exceptions.ResourceNotFoundException;
import com.immobilier.gestionImmobiliere.modules.biens.dto.requests.CreateMaisonDTO;
import com.immobilier.gestionImmobiliere.modules.biens.dto.requests.UpdateMaisonDTO;
import com.immobilier.gestionImmobiliere.modules.biens.dto.requests.UpdateStatutMaisonDTO;
import com.immobilier.gestionImmobiliere.modules.biens.dto.responses.MaisonResponseDTO;
import com.immobilier.gestionImmobiliere.modules.journal.services.JournalService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;

import static com.immobilier.gestionImmobiliere.utils.BuildSuccessResponse.buildSuccessResponse;

@Service
public class MaisonService {

    // RG3 — transitions autorisées
    private static final Map<StatutMaison, EnumSet<StatutMaison>> TRANSITIONS = new EnumMap<>(StatutMaison.class);
    static {
        TRANSITIONS.put(StatutMaison.DISPONIBLE, EnumSet.of(StatutMaison.RESERVEE, StatutMaison.EN_MAINTENANCE));
        TRANSITIONS.put(StatutMaison.RESERVEE, EnumSet.of(StatutMaison.LOUEE, StatutMaison.DISPONIBLE));
        TRANSITIONS.put(StatutMaison.LOUEE, EnumSet.of(StatutMaison.DISPONIBLE));
        TRANSITIONS.put(StatutMaison.EN_MAINTENANCE, EnumSet.of(StatutMaison.DISPONIBLE));
    }

    private final MaisonRepository maisonRepository;
    private final CourRepository courRepository;

    public MaisonService(MaisonRepository maisonRepository, CourRepository courRepository) {
        this.maisonRepository = maisonRepository;
        this.courRepository = courRepository;
    }

    public ResponseEntity<?> getAll(Integer idCour, StatutMaison statut, Pageable pageable) {
        Page<Maison> page = idCour != null
                ? maisonRepository.findByCour_IdCour(idCour, pageable)
                : (statut != null ? maisonRepository.findByStatut(statut, pageable) : maisonRepository.findAll(pageable));
        return buildSuccessResponse(HttpStatus.OK, "Liste des maisons", "MAISON_LIST", page.map(this::toDto));
    }

    public ResponseEntity<?> getById(Integer id) {
        return buildSuccessResponse(HttpStatus.OK, "Maison trouvée", "MAISON_FOUND", toDto(findOrThrow(id)));
    }

    @Transactional
    public ResponseEntity<?> create(CreateMaisonDTO dto, Integer currentUserId) {
        Cour cour = courRepository.findById(dto.getIdCour())
                .orElseThrow(() -> new ResourceNotFoundException("cour",dto.getIdCour()));

        Maison maison = Maison.builder()
                .cour(cour)
                .typeMaison(dto.getTypeMaison())
                .nomCommunMaison(dto.getNomCommunMaison())
                .nombrePiece(dto.getNombrePiece())
                .loyer(dto.getLoyer())
                .caution(dto.getCaution())
                .nombreMoisCaution(dto.getNombreMoisCaution())
                .statut(StatutMaison.DISPONIBLE)
                .userCreate(currentUserId)
                .dateCreate(LocalDateTime.now())
                .build();
        maisonRepository.save(maison);
        return buildSuccessResponse(HttpStatus.CREATED, "Maison créée avec succès", "MAISON_CREATED", toDto(maison));
    }

    @Transactional
    public ResponseEntity<?> update(Integer id, UpdateMaisonDTO dto, Integer currentUserId) {
        Maison maison = findOrThrow(id);

        if (dto.getTypeMaison() != null) maison.setTypeMaison(dto.getTypeMaison());
        if (dto.getNomCommunMaison() != null) maison.setNomCommunMaison(dto.getNomCommunMaison());
        if (dto.getNombrePiece() != null) maison.setNombrePiece(dto.getNombrePiece());
        if (dto.getLoyer() != null) maison.setLoyer(dto.getLoyer());
        if (dto.getCaution() != null) maison.setCaution(dto.getCaution());
        if (dto.getNombreMoisCaution() != null) maison.setNombreMoisCaution(dto.getNombreMoisCaution());
        maison.setUserUpdate(currentUserId);
        maison.setDateUpdate(LocalDateTime.now());

        maisonRepository.save(maison);
        return buildSuccessResponse(HttpStatus.OK, "Maison mise à jour", "MAISON_UPDATED", toDto(maison));
    }


    @Transactional
    public ResponseEntity<?> create(CreateMaisonDTO dto) {
        Cour cour = courRepository.findById(dto.getIdCour())
                .orElseThrow(() -> new ResourceNotFoundException("cour",dto.getIdCour()));

        Maison maison = Maison.builder()
                .cour(cour)
                .typeMaison(dto.getTypeMaison())
                .nomCommunMaison(dto.getNomCommunMaison())
                .nombrePiece(dto.getNombrePiece())
                .loyer(dto.getLoyer())
                .caution(dto.getCaution())
                .nombreMoisCaution(dto.getNombreMoisCaution())
                .statut(StatutMaison.DISPONIBLE)
                .dateCreate(LocalDateTime.now())
                .build();
        maisonRepository.save(maison);
        return buildSuccessResponse(HttpStatus.CREATED, "Maison créée avec succès", "MAISON_CREATED", toDto(maison));
    }

    @Transactional
    public ResponseEntity<?> update(Integer id, UpdateMaisonDTO dto) {
        Maison maison = findOrThrow(id);

        if (dto.getTypeMaison() != null) maison.setTypeMaison(dto.getTypeMaison());
        if (dto.getNomCommunMaison() != null) maison.setNomCommunMaison(dto.getNomCommunMaison());
        if (dto.getNombrePiece() != null) maison.setNombrePiece(dto.getNombrePiece());
        if (dto.getLoyer() != null) maison.setLoyer(dto.getLoyer());
        if (dto.getCaution() != null) maison.setCaution(dto.getCaution());
        if (dto.getNombreMoisCaution() != null) maison.setNombreMoisCaution(dto.getNombreMoisCaution());
        maison.setDateUpdate(LocalDateTime.now());
        maisonRepository.save(maison);
        return buildSuccessResponse(HttpStatus.OK, "Maison mise à jour", "MAISON_UPDATED", toDto(maison));
    }

    @Transactional
    public ResponseEntity<?> updateStatut(Integer id, UpdateStatutMaisonDTO dto, Integer currentUserId) {
        Maison maison = findOrThrow(id);
        StatutMaison current = maison.getStatut();
        StatutMaison target = dto.getStatut();

        if (current != target && !TRANSITIONS.getOrDefault(current, EnumSet.noneOf(StatutMaison.class)).contains(target)) {
            throw new InvalidStatutTransitionException(current.name(), target.name());
        }
        maison.setStatut(target);
        maison.setUserUpdate(currentUserId);
        maison.setDateUpdate(LocalDateTime.now());
        maisonRepository.save(maison);
        return buildSuccessResponse(HttpStatus.OK, "Statut mis à jour", "MAISON_STATUT_UPDATED", toDto(maison));
    }

    @Transactional
    public ResponseEntity<?> updateStatut(Integer id, UpdateStatutMaisonDTO dto) {
        Maison maison = findOrThrow(id);
        StatutMaison current = maison.getStatut();
        StatutMaison target = dto.getStatut();

        if (current != target && !TRANSITIONS.getOrDefault(current, EnumSet.noneOf(StatutMaison.class)).contains(target)) {
            throw new InvalidStatutTransitionException(current.name(), target.name());
        }

        maison.setStatut(target);
        maison.setDateUpdate(LocalDateTime.now());
        maisonRepository.save(maison);
        return buildSuccessResponse(HttpStatus.OK, "Statut mis à jour", "MAISON_STATUT_UPDATED", toDto(maison));
    }

    @Transactional
    public ResponseEntity<?> delete(Integer id) {
        maisonRepository.delete(findOrThrow(id));
        return buildSuccessResponse(HttpStatus.OK, "Maison supprimée", "MAISON_DELETED", null);
    }

    private Maison findOrThrow(Integer id) {
        return maisonRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("maison",id));
    }

    private MaisonResponseDTO toDto(Maison m) {
        return MaisonResponseDTO.builder()
                .idMaison(m.getIdMaison())
                .typeMaison(m.getTypeMaison())
                .nomCommunMaison(m.getNomCommunMaison())
                .nombrePiece(m.getNombrePiece())
                .loyer(m.getLoyer())
                .caution(m.getCaution())
                .nombreMoisCaution(m.getNombreMoisCaution())
                .statut(m.getStatut())
                .idCour(m.getCour().getIdCour())
                .referenceCours(m.getCour().getReferenceCours())
                .build();
    }
}