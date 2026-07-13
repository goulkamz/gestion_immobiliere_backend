package com.immobilier.gestionImmobiliere.modules.reservations.services;

import com.immobilier.gestionImmobiliere.donnees.biens.model.Maison;
import com.immobilier.gestionImmobiliere.donnees.biens.model.StatutMaison;
import com.immobilier.gestionImmobiliere.donnees.biens.repository.MaisonRepository;
import com.immobilier.gestionImmobiliere.donnees.reservations.model.ReservationMaison;
import com.immobilier.gestionImmobiliere.donnees.reservations.model.StatutReservation;
import com.immobilier.gestionImmobiliere.donnees.reservations.repository.ReservationMaisonRepository;
import com.immobilier.gestionImmobiliere.donnees.user.model.User;
import com.immobilier.gestionImmobiliere.donnees.user.repository.UserRepository;
import com.immobilier.gestionImmobiliere.exceptions.*;
import com.immobilier.gestionImmobiliere.modules.contrats.dto.requests.CreateContratLocationDTO;
import com.immobilier.gestionImmobiliere.modules.contrats.services.ContratLocationService;
import com.immobilier.gestionImmobiliere.modules.reservations.dto.requests.CreateReservationDTO;
import com.immobilier.gestionImmobiliere.modules.reservations.dto.responses.ReservationResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.immobilier.gestionImmobiliere.utils.BuildSuccessResponse.buildSuccessResponse;

@Service
public class ReservationService {

    private final ReservationMaisonRepository reservationRepository;
    private final MaisonRepository maisonRepository;
    private final UserRepository userRepository;
    private final ContratLocationService contratLocationService;

    public ReservationService(ReservationMaisonRepository reservationRepository, MaisonRepository maisonRepository,
                              UserRepository userRepository, ContratLocationService contratLocationService) {
        this.reservationRepository = reservationRepository;
        this.maisonRepository = maisonRepository;
        this.userRepository = userRepository;
        this.contratLocationService = contratLocationService;
    }

    public ResponseEntity<?> getAllForCurrentUser(Integer idMaison, Integer currentUserId, boolean isAdminOrAgent, Pageable pageable) {
        Page<ReservationMaison> page = isAdminOrAgent
                ? (idMaison != null ? reservationRepository.findByMaison_IdMaison(idMaison, pageable) : reservationRepository.findAll(pageable))
                : reservationRepository.findByUser_IdUser(currentUserId, pageable);
        return buildSuccessResponse(HttpStatus.OK, "Liste des réservations", "RESERVATION_LIST", page.map(this::toDto));
    }

    // Remplace l'ancien getById(Integer id) qui retournait un ResponseEntity
    @PostAuthorize(
            "hasAnyRole('ADMIN','AGENT') " +
                    "or returnObject.idUser == authentication.principal.id"
    )
    public ReservationResponseDTO getReservationById(Integer id) {
        return toDto(findOrThrow(id));
    }

    @Transactional
    public ResponseEntity<?> create(CreateReservationDTO dto, Integer currentUserId) {
        if (!dto.getDateFin().isAfter(dto.getDateDebut())) {
            throw new IllegalArgumentException("La date de fin doit être postérieure à la date de début");
        }

        Maison maison = maisonRepository.findById(dto.getIdMaison())
                .orElseThrow(() -> new ResourceNotFoundException("maison", dto.getIdMaison()));
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("utilisateur", currentUserId));

        // RG — maison doit être disponible
        if (maison.getStatut() != StatutMaison.DISPONIBLE) {
            throw new MaisonIndisponibleException(maison.getIdMaison());
        }

        // RG2 — vérification des conflits de dates
        if (!reservationRepository.findConflits(dto.getIdMaison(), dto.getDateDebut(), dto.getDateFin()).isEmpty()) {
            throw new ConflitReservationException(dto.getIdMaison());
        }

        ReservationMaison reservation = ReservationMaison.builder()
                .user(user)
                .maison(maison)
                .dateDebut(dto.getDateDebut())
                .dateFin(dto.getDateFin())
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

        // Libère la maison uniquement si aucune autre réservation active ne la couvre
        Maison maison = reservation.getMaison();
        if (reservationRepository.findConflits(maison.getIdMaison(), reservation.getDateDebut(), reservation.getDateFin()).isEmpty()) {
            maison.setStatut(StatutMaison.DISPONIBLE);
            maisonRepository.save(maison);
        }

        return buildSuccessResponse(HttpStatus.OK, "Réservation annulée", "RESERVATION_ANNULEE", toDto(reservation));
    }

    /**
     * F21 — conversion en contrat de location. Délègue au ContratLocationService
     * pour éviter de dupliquer la logique de création de bail (génération d'échéances incluse).
     */
    @Transactional
    public ResponseEntity<?> convertirEnLocation(Integer id, Double montantLoyer, String typeContrat, Integer currentUserId) {
        ReservationMaison reservation = findOrThrow(id);

        if (reservation.getStatut() != StatutReservation.CONFIRMEE) {
            throw new InvalidStatutTransitionException(reservation.getStatut().name(), StatutReservation.CONVERTIE.name());
        }

        CreateContratLocationDTO contratDto = new CreateContratLocationDTO();
        contratDto.setIdLocataire(reservation.getUser().getIdUser());
        contratDto.setIdMaison(reservation.getMaison().getIdMaison());
        contratDto.setDateEntree(reservation.getDateDebut());
        contratDto.setDateSortie(reservation.getDateFin());
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
                .idMaison(r.getMaison().getIdMaison())
                .nomCommunMaison(r.getMaison().getNomCommunMaison())
                .dateDebut(r.getDateDebut())
                .dateFin(r.getDateFin())
                .statut(r.getStatut())
                .build();
    }
}