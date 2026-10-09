package com.immobilier.gestionImmobiliere.modules.reservations.services;

import com.immobilier.gestionImmobiliere.donnees.biens.model.Maison;
import com.immobilier.gestionImmobiliere.donnees.biens.model.StatutMaison;
import com.immobilier.gestionImmobiliere.donnees.contrats.model.StatutMandat;
import com.immobilier.gestionImmobiliere.donnees.contrats.model.TypeMandat;
import com.immobilier.gestionImmobiliere.donnees.contrats.repository.ContratMandatRepository;
import com.immobilier.gestionImmobiliere.donnees.biens.repository.MaisonRepository;
import com.immobilier.gestionImmobiliere.donnees.reservations.model.ReservationMaison;
import com.immobilier.gestionImmobiliere.donnees.reservations.model.StatutReservation;
import com.immobilier.gestionImmobiliere.donnees.reservations.repository.ReservationMaisonRepository;
import com.immobilier.gestionImmobiliere.donnees.user.model.User;
import com.immobilier.gestionImmobiliere.donnees.user.repository.UserRepository;
import com.immobilier.gestionImmobiliere.exceptions.*;
import com.immobilier.gestionImmobiliere.modules.contrats.dto.requests.CreateContratLocationDTO;
import com.immobilier.gestionImmobiliere.modules.contrats.services.ContratLocationService;
import com.immobilier.gestionImmobiliere.donnees.parametres.model.CleParametre;
import com.immobilier.gestionImmobiliere.modules.parametres.services.ParametreService;
import com.immobilier.gestionImmobiliere.modules.reservations.dto.requests.CreateReservationDTO;
import com.immobilier.gestionImmobiliere.modules.reservations.dto.responses.ReservationResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.immobilier.gestionImmobiliere.utils.BuildSuccessResponse.buildSuccessResponse;

@Service
public class ReservationService {

    private final ReservationMaisonRepository reservationRepository;
    private final MaisonRepository maisonRepository;
    private final UserRepository userRepository;
    private final ContratLocationService contratLocationService;
    private final ParametreService parametreService;
    private final ContratMandatRepository mandatRepository;

    public ReservationService(ReservationMaisonRepository reservationRepository, MaisonRepository maisonRepository,
                              UserRepository userRepository, ContratLocationService contratLocationService,
                              ParametreService parametreService, ContratMandatRepository mandatRepository) {
        this.reservationRepository = reservationRepository;
        this.maisonRepository = maisonRepository;
        this.userRepository = userRepository;
        this.contratLocationService = contratLocationService;
        this.parametreService = parametreService;
        this.mandatRepository = mandatRepository;
    }

    public ResponseEntity<?> getAllForCurrentUser(Integer idMaison, StatutReservation statut, String recherche,
                                                  Integer currentUserId, boolean isAdminOrAgent, boolean isBailleur,
                                                  Pageable pageable) {
        Page<ReservationMaison> page;
        if (isAdminOrAgent) {
            // Filtres appliqués côté serveur pour que la pagination reste cohérente
            page = reservationRepository.findAll(filtrer(idMaison, statut, recherche), pageable);
        } else if (isBailleur) {
            page = reservationRepository.findByMaison_Cour_Proprietaire_IdUser(currentUserId, pageable);
        } else {
            // CLIENT : ses propres réservations uniquement
            page = reservationRepository.findByUser_IdUser(currentUserId, pageable);
        }
        return buildSuccessResponse(HttpStatus.OK, "Liste réservations", "RESERVATION_LIST", page.map(this::toDto));
    }

    // Recherche insensible à la casse sur le nom de la maison ou le nom/prénom du client
    private Specification<ReservationMaison> filtrer(Integer idMaison, StatutReservation statut, String recherche) {
        return (root, query, cb) -> {
            List<Predicate> predicats = new ArrayList<>();
            if (idMaison != null) {
                predicats.add(cb.equal(root.get("maison").get("idMaison"), idMaison));
            }
            if (statut != null) {
                predicats.add(cb.equal(root.get("statut"), statut));
            }
            if (recherche != null && !recherche.isBlank()) {
                String motif = "%" + recherche.trim().toLowerCase() + "%";
                Join<Object, Object> maison = root.join("maison");
                Join<Object, Object> user = root.join("user");
                predicats.add(cb.or(
                        cb.like(cb.lower(maison.get("nomCommunMaison")), motif),
                        cb.like(cb.lower(cb.concat(cb.concat(user.get("nom"), " "), user.get("prenom"))), motif)));
            }
            return cb.and(predicats.toArray(new Predicate[0]));
        };
    }

    public ResponseEntity<?> getByIdForCurrentUser(Integer id, Integer currentUserId, boolean isAdminOrAgent) {
        ReservationMaison reservation = findOrThrow(id);
        // @PreAuthorize a déjà tranché l'accès ; ici on charge juste et on renvoie
        return buildSuccessResponse(HttpStatus.OK, "Réservation trouvée", "RESERVATION_FOUND", toDto(reservation));
    }
    @Transactional
    public ResponseEntity<?> create(CreateReservationDTO dto, Integer currentUserId) {
        Maison maison = maisonRepository.findById(dto.getIdMaison())
                .orElseThrow(() -> new ResourceNotFoundException("maison", dto.getIdMaison()));
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("utilisateur", currentUserId));

        // RG — maison doit être disponible
        if (maison.getStatut() != StatutMaison.DISPONIBLE) {
            throw new MaisonIndisponibleException(maison.getIdMaison());
        }

        // RG — la maison n'est proposée au public que si sa cour a un mandat ACTIF GESTION ou LOCATION
        Integer idCourMaison = maison.getCour().getIdCour();
        if (!mandatRepository.existsByCour_IdCourAndStatutAndTypeMandatIn(
                idCourMaison, StatutMandat.ACTIF, List.of(TypeMandat.GESTION, TypeMandat.LOCATION))) {
            throw new MandatInsuffisantException(idCourMaison, "Cette maison n'est plus proposée à la réservation.");
        }

        // dateFin n'est qu'un marqueur technique d'expiration du blocage (pas la durée
        // réelle du séjour, inconnue à ce stade — voir convertirEnLocation)
        int delaiHeures = parametreService.getEntier(CleParametre.DELAI_EXPIRATION_RESERVATION_HEURES, 6);
        LocalDateTime dateFin = dto.getDateDebut().plusHours(delaiHeures);

        // RG2 — vérification des conflits sur la fenêtre de blocage
        if (!reservationRepository.findConflits(dto.getIdMaison(), dto.getDateDebut(), dateFin).isEmpty()) {
            throw new ConflitReservationException(dto.getIdMaison());
        }

        ReservationMaison reservation = ReservationMaison.builder()
                .user(user)
                .maison(maison)
                .dateDebut(dto.getDateDebut())
                .dateFin(dateFin)
                .statut(StatutReservation.EN_ATTENTE)
                .build();
        reservationRepository.save(reservation);

        maison.setStatut(StatutMaison.RESERVEE);
        maisonRepository.save(maison);

        return buildSuccessResponse(HttpStatus.CREATED, "Réservation créée, maison marquée réservée", "RESERVATION_CREATED", toDto(reservation));
    }

    @Transactional
    public ResponseEntity<?> confirmer(Integer id) {
        ReservationMaison reservation = findOrThrow(id);
        if (reservation.getStatut() != StatutReservation.EN_ATTENTE) {
            throw new InvalidStatutTransitionException(reservation.getStatut().name(), StatutReservation.CONFIRMEE.name());
        }

        reservation.setStatut(StatutReservation.CONFIRMEE);
        reservationRepository.save(reservation);
        return buildSuccessResponse(HttpStatus.OK, "Réservation confirmée", "RESERVATION_CONFIRMEE", toDto(reservation));
    }

    @Transactional
    public ResponseEntity<?> annuler(Integer id) {
        ReservationMaison reservation = findOrThrow(id);
        if (reservation.getStatut() == StatutReservation.CONVERTIE || reservation.getStatut() == StatutReservation.ANNULEE) {
            throw new InvalidStatutTransitionException(reservation.getStatut().name(), StatutReservation.ANNULEE.name());
        }

        reservation.setStatut(StatutReservation.ANNULEE);
        reservationRepository.save(reservation);
        libererMaisonSiLibre(reservation);

        return buildSuccessResponse(HttpStatus.OK, "Réservation annulée", "RESERVATION_ANNULEE", toDto(reservation));
    }

    /**
     * Job planifié — annule les réservations EN_ATTENTE non confirmées dont dateFin
     * (= dateDebut + DELAI_EXPIRATION_RESERVATION_HEURES, calculée à la création)
     * est dépassée, et repasse la maison en DISPONIBLE si aucune autre réservation
     * active ne la couvre.
     */
    @Transactional
    public void expirerReservationsEnAttente() {
        List<ReservationMaison> expirees = reservationRepository.findByStatutAndDateFinBefore(StatutReservation.EN_ATTENTE, LocalDateTime.now());
        for (ReservationMaison reservation : expirees) {
            reservation.setStatut(StatutReservation.ANNULEE);
            reservationRepository.save(reservation);
            libererMaisonSiLibre(reservation);
        }
    }

    // Libère la maison uniquement si aucune autre réservation active ne la couvre
    private void libererMaisonSiLibre(ReservationMaison reservation) {
        Maison maison = reservation.getMaison();
        if (reservationRepository.findConflits(maison.getIdMaison(), reservation.getDateDebut(), reservation.getDateFin()).isEmpty()) {
            maison.setStatut(StatutMaison.DISPONIBLE);
            maisonRepository.save(maison);
        }
    }

    /**
     * F21 — conversion en contrat de location. Délègue au ContratLocationService
     * pour éviter de dupliquer la logique de création de bail (génération d'échéances incluse).
     * dateSortie est optionnelle (bail à durée indéterminée si absente) : dateFin de
     * la réservation n'est qu'un marqueur technique d'expiration du blocage, pas la
     * durée réelle du séjour, donc on ne la réutilise pas ici.
     */
    @Transactional
    public ResponseEntity<?> convertirEnLocation(Integer id, BigDecimal montantLoyer, String typeContrat,
                                                 LocalDateTime dateSortie, Integer currentUserId) {
        ReservationMaison reservation = findOrThrow(id);

        if (reservation.getStatut() != StatutReservation.CONFIRMEE) {
            throw new InvalidStatutTransitionException(reservation.getStatut().name(), StatutReservation.CONVERTIE.name());
        }

        // Un mandat ACTIF de type GESTION ou LOCATION doit couvrir la cour de la maison (pas VENTE)
        Integer idCour = reservation.getMaison().getCour().getIdCour();
        if (!mandatRepository.existsByCour_IdCourAndStatutAndTypeMandatIn(
                idCour, StatutMandat.ACTIF, List.of(TypeMandat.GESTION, TypeMandat.LOCATION))) {
            throw new MandatInsuffisantException(idCour);
        }

        CreateContratLocationDTO contratDto = new CreateContratLocationDTO();
        contratDto.setIdLocataire(reservation.getUser().getIdUser());
        contratDto.setIdMaison(reservation.getMaison().getIdMaison());
        contratDto.setDateEntree(reservation.getDateDebut());
        contratDto.setDateSortie(dateSortie);
        contratDto.setMontantLoyer(montantLoyer);
        contratDto.setTypeContrat(typeContrat);

        // Plus de bascule de statut manuelle ici : createFromReservation() accepte
        // directement une maison RESERVEE (la maison reste RESERVEE jusqu'à l'appel).
        var contratDto2 = contratLocationService.createFromReservation(contratDto, currentUserId);

        reservation.setStatut(StatutReservation.CONVERTIE);
        reservationRepository.save(reservation);

        return buildSuccessResponse(HttpStatus.CREATED, "Réservation convertie en contrat de location", "RESERVATION_CONVERTIE", contratDto2);
    }

    private ReservationMaison findOrThrow(Integer id) {
        return reservationRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("réservation", id));
    }

    private ReservationResponseDTO toDto(ReservationMaison r) {
        return ReservationResponseDTO.builder()
                .idReservation(r.getIdReservation())
                .idUser(r.getUser().getIdUser())
                .nomUser(r.getUser().getNom() + " " + r.getUser().getPrenom())
                .emailUser(r.getUser().getEmail())
                .telephoneUser(r.getUser().getTelephone())
                .idMaison(r.getMaison().getIdMaison())
                .nomCommunMaison(r.getMaison().getNomCommunMaison())
                .dateDebut(r.getDateDebut())
                .dateFin(r.getDateFin())
                .statut(r.getStatut())
                .build();
    }
}