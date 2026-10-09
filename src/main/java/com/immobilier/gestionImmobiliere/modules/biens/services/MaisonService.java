package com.immobilier.gestionImmobiliere.modules.biens.services;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import java.util.ArrayList;
import java.util.List;

import com.immobilier.gestionImmobiliere.donnees.biens.model.Cour;
import com.immobilier.gestionImmobiliere.donnees.biens.model.Maison;
import com.immobilier.gestionImmobiliere.donnees.biens.model.StatutMaison;
import com.immobilier.gestionImmobiliere.donnees.biens.repository.CourRepository;
import com.immobilier.gestionImmobiliere.donnees.biens.repository.MaisonRepository;
import com.immobilier.gestionImmobiliere.donnees.contrats.model.StatutMandat;
import com.immobilier.gestionImmobiliere.donnees.contrats.model.TypeMandat;
import com.immobilier.gestionImmobiliere.donnees.contrats.repository.ContratMandatRepository;
import com.immobilier.gestionImmobiliere.exceptions.InvalidStatutTransitionException;
import com.immobilier.gestionImmobiliere.exceptions.ResourceNotFoundException;
import com.immobilier.gestionImmobiliere.modules.biens.dto.requests.CreateMaisonDTO;
import com.immobilier.gestionImmobiliere.modules.biens.dto.requests.UpdateMaisonDTO;
import com.immobilier.gestionImmobiliere.modules.biens.dto.requests.UpdateStatutMaisonDTO;
import com.immobilier.gestionImmobiliere.modules.biens.dto.responses.MaisonResponseDTO;
import com.immobilier.gestionImmobiliere.modules.journal.services.JournalService;
import com.immobilier.gestionImmobiliere.modules.user.jwtService.UserDetailsImpl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
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
    private final ContratMandatRepository mandatRepository;

    public MaisonService(MaisonRepository maisonRepository, CourRepository courRepository,
                         ContratMandatRepository mandatRepository) {
        this.maisonRepository = maisonRepository;
        this.courRepository = courRepository;
        this.mandatRepository = mandatRepository;
    }

    // Catalogue public : uniquement les maisons DISPONIBLES dont la cour a un mandat ACTIF GESTION ou LOCATION
    public ResponseEntity<?> getAllPublic(Integer idCour, String recherche, Pageable pageable) {
        String motif = recherche == null || recherche.isBlank() ? "%" : "%" + recherche.trim().toLowerCase() + "%";
        Page<Maison> page = maisonRepository.findCataloguePublic(idCour, motif, pageable);
        return buildSuccessResponse(HttpStatus.OK, "Liste des maisons", "MAISON_LIST", page.map(this::toDto));
    }

    // Une maison DISPONIBLE sans mandat valide n'existe pas pour le public ; les autres statuts
    // (réservée, louée...) restent consultables par ceux qui ont une réservation ou un contrat dessus.
    public ResponseEntity<?> getByIdPublic(Integer id) {
        Maison maison = findOrThrow(id);
        if (maison.getStatut() == StatutMaison.DISPONIBLE && !aMandatLocatif(maison)) {
            throw new ResourceNotFoundException("maison", id);
        }
        return buildSuccessResponse(HttpStatus.OK, "Maison trouvée", "MAISON_FOUND", toDto(maison));
    }

    // Vue authentifiée : agent/admin voient toutes les maisons, un bailleur seulement celles de ses cours
    public ResponseEntity<?> getAllPourUtilisateur(Integer idCour, StatutMaison statut, String recherche, Pageable pageable,
                                                   UserDetailsImpl currentUser) {
        Integer idProprietaire = estBailleur(currentUser) ? currentUser.getIdUser() : null;
        Page<Maison> page = maisonRepository.findAll(filtrer(idCour, statut, idProprietaire, recherche), pageable);
        return buildSuccessResponse(HttpStatus.OK, "Liste des maisons", "MAISON_LIST", page.map(this::toDto));
    }

    // Recherche insensible à la casse sur le nom, le type de la maison ou la référence de sa cour
    private Specification<Maison> filtrer(Integer idCour, StatutMaison statut, Integer idProprietaire, String recherche) {
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
                predicats.add(cb.or(
                        cb.like(cb.lower(root.<String>get("nomCommunMaison")), motif),
                        cb.like(cb.lower(root.<String>get("typeMaison")), motif),
                        cb.like(cb.lower(cour.<String>get("referenceCour")), motif)));
            }
            return cb.and(predicats.toArray(new Predicate[0]));
        };
    }

    public ResponseEntity<?> getByIdPourUtilisateur(Integer id, UserDetailsImpl currentUser) {
        Maison maison = findOrThrow(id);
        if (estBailleur(currentUser) && !maison.getCour().getProprietaire().getIdUser().equals(currentUser.getIdUser())) {
            throw new AccessDeniedException("Vous n'avez pas accès à ce bien");
        }
        return buildSuccessResponse(HttpStatus.OK, "Maison trouvée", "MAISON_FOUND", toDto(maison));
    }

    private boolean aMandatLocatif(Maison maison) {
        return mandatRepository.existsByCour_IdCourAndStatutAndTypeMandatIn(
                maison.getCour().getIdCour(), StatutMandat.ACTIF, List.of(TypeMandat.GESTION, TypeMandat.LOCATION));
    }

    private boolean estBailleur(UserDetailsImpl currentUser) {
        return currentUser.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_BAILLEUR"));
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
                .avance(dto.getAvance())
                .statut(StatutMaison.DISPONIBLE)
                .userCreate(currentUserId)
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
        if (dto.getAvance() != null) maison.setAvance(dto.getAvance());
        if (dto.getNombreMoisCaution() != null) maison.setNombreMoisCaution(dto.getNombreMoisCaution());
        maison.setUserUpdate(currentUserId);
        maison.setUpdatedAt(LocalDateTime.now());

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
        maison.setUpdatedAt(LocalDateTime.now());
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
        maison.setUpdatedAt(LocalDateTime.now());
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
        maison.setUpdatedAt(LocalDateTime.now());
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
                .avance(m.getAvance())
                .nombreMoisCaution(m.getNombreMoisCaution())
                .statut(m.getStatut())
                .idCour(m.getCour().getIdCour())
                .referenceCour(m.getCour().getReferenceCour())
                .build();
    }
}