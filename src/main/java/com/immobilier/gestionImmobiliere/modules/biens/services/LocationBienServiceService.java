package com.immobilier.gestionImmobiliere.modules.biens.services;

import com.immobilier.gestionImmobiliere.donnees.biens.model.BienService;
import com.immobilier.gestionImmobiliere.donnees.biens.model.LocationBienService;
import com.immobilier.gestionImmobiliere.donnees.biens.model.StatutLocationBienService;
import com.immobilier.gestionImmobiliere.donnees.biens.repository.BienServiceRepository;
import com.immobilier.gestionImmobiliere.donnees.biens.repository.LocationBienServiceRepository;
import com.immobilier.gestionImmobiliere.donnees.paiements.model.Paiement;
import com.immobilier.gestionImmobiliere.donnees.paiements.model.PaiementLocationBienService;
import com.immobilier.gestionImmobiliere.donnees.paiements.model.TypePaiementLocationBienService;
import com.immobilier.gestionImmobiliere.donnees.paiements.repository.PaiementLocationBienServiceRepository;
import com.immobilier.gestionImmobiliere.donnees.paiements.repository.PaiementRepository;
import com.immobilier.gestionImmobiliere.donnees.user.model.User;
import com.immobilier.gestionImmobiliere.donnees.user.repository.UserRepository;
import com.immobilier.gestionImmobiliere.exceptions.ResourceNotFoundException;
import com.immobilier.gestionImmobiliere.modules.biens.dto.requests.ConfirmerLocationDTO;
import com.immobilier.gestionImmobiliere.modules.biens.dto.requests.CreateLocationBienServiceDTO;
import com.immobilier.gestionImmobiliere.modules.biens.dto.requests.ModifierDureeLocationDTO;
import com.immobilier.gestionImmobiliere.modules.biens.dto.requests.UpdateStatutLocationBienServiceDTO;
import com.immobilier.gestionImmobiliere.modules.biens.dto.responses.LocationBienServiceResponseDTO;
import com.immobilier.gestionImmobiliere.modules.biens.dto.responses.ModifierDureeLocationResponseDTO;
import com.immobilier.gestionImmobiliere.modules.user.jwtService.UserDetailsImpl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

import static com.immobilier.gestionImmobiliere.utils.BuildSuccessResponse.buildSuccessResponse;

@Service
public class LocationBienServiceService {

    private final LocationBienServiceRepository locationBienServiceRepository;
    private final BienServiceRepository bienServiceRepository;
    private final PaiementRepository paiementRepository;
    private final UserRepository userRepository;
    private final PaiementLocationBienServiceRepository paiementLocationBienServiceRepository;

    public LocationBienServiceService(LocationBienServiceRepository locationRepository, LocationBienServiceRepository locationBienServiceRepository, BienServiceRepository bienServiceRepository,
                                      PaiementRepository paiementRepository, UserRepository userRepository, PaiementLocationBienServiceRepository paiementLocationBienServiceRepository) {
        this.locationBienServiceRepository = locationBienServiceRepository;
        this.bienServiceRepository = bienServiceRepository;
        this.paiementRepository = paiementRepository;
        this.userRepository = userRepository;
        this.paiementLocationBienServiceRepository = paiementLocationBienServiceRepository;
    }

    public ResponseEntity<?> getAll(Pageable pageable, UserDetailsImpl currentUser) {
        boolean isClient = currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CLIENT"));

        Page<LocationBienServiceResponseDTO> result = (isClient
                ? locationBienServiceRepository.findByClient_IdUser(currentUser.getIdUser(), pageable)
                : locationBienServiceRepository.findAll(pageable)
        ).map(this::toDto);

        return buildSuccessResponse(HttpStatus.OK, "Liste des locations", "LOCATION_BIEN_SERVICE_LIST", result);
    }

    public ResponseEntity<?> getById(Integer id, UserDetailsImpl currentUser) {
        LocationBienService location = findOrThrow(id);

        boolean isClient = currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CLIENT"));
        if (isClient && !location.getClient().getIdUser().equals(currentUser.getIdUser())) {
            throw new AccessDeniedException("Vous n'avez pas accès à cette location");
        }

        return buildSuccessResponse(HttpStatus.OK, "Détail location", "LOCATION_BIEN_SERVICE_DETAIL", toDto(location));
    }

    /**
     * Étape 1 — Le CLIENT dépose une demande de location.
     * Aucun paiement à ce stade. Montant estimé calculé pour information,
     * mais non contractuel tant que l'agent n'a pas confirmé.
     */
    @Transactional
    public ResponseEntity<?> create(CreateLocationBienServiceDTO dto, Integer currentUserId) {
        BienService bien = bienServiceRepository.findById(dto.getIdBienService())
                .orElseThrow(() -> new ResourceNotFoundException("bienService", dto.getIdBienService()));
        User client = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("user", currentUserId));

        int dureeEstimee = (int) Duration.between(dto.getDateDebut(), dto.getDateFin()).toDays();
        double montantEstime = dureeEstimee * (bien.getPrixJournalier() != null ? bien.getPrixJournalier() : 0);

        LocationBienService location = LocationBienService.builder()
                .client(client)
                .bienService(bien)
                .destination(dto.getDestination())
                .dateDebut(dto.getDateDebut())
                .dateFin(dto.getDateFin())
                .duree(dureeEstimee)
                .montantTotal(montantEstime)
                .statut(StatutLocationBienService.EN_ATTENTE)
                .userCreate(currentUserId)
                .build();

        locationBienServiceRepository.save(location);
        return buildSuccessResponse(HttpStatus.CREATED, "Demande de location enregistrée, en attente de confirmation", "LOCATION_BIEN_SERVICE_CREATED", toDto(location));
    }

    /**
     * Étape 2 — L'AGENT/SECRÉTAIRE réceptionne le paiement au comptoir,
     * crée l'entité Paiement, l'associe à la location, et confirme (statut ACTIF).
     * Les dates peuvent être ajustées si négociées différemment au moment du paiement.
     */
    @Transactional
    public ResponseEntity<?> confirmer(Integer id, ConfirmerLocationDTO dto, Integer currentAgentId) {
        LocationBienService location = findOrThrow(id);

        if (location.getStatut() != StatutLocationBienService.EN_ATTENTE) {
            throw new IllegalStateException("Seule une location EN_ATTENTE peut être confirmée");
        }

        // Création du paiement réceptionné par l'agent
        Paiement paiement = Paiement.builder()
                .datePaiement(LocalDateTime.now())
                .montantPaiement(dto.getMontantPaiement())
                .modePaiement(dto.getModePaiement())
                .referencePaiement(dto.getReferencePaiement())
                .userCreate(currentAgentId)
                .build();
        paiementRepository.save(paiement);

        paiementLocationBienServiceRepository.save(PaiementLocationBienService.builder()
                .idLocationBienService(location.getIdLocationBienService())
                .idPaiement(paiement.getIdPaiement())
                .typePaiement(TypePaiementLocationBienService.INITIAL)
                .build());

        // Ajustement des dates si l'agent les a modifiées au comptoir
        LocalDateTime dateDebut = dto.getDateDebut() != null ? dto.getDateDebut() : location.getDateDebut();
        LocalDateTime dateFin = dto.getDateFin() != null ? dto.getDateFin() : location.getDateFin();
        int dureeFinale = (int) Duration.between(dateDebut, dateFin).toDays();


        location.setDateDebut(dateDebut);
        location.setDateFin(dateFin);
        location.setDuree(dureeFinale);
        location.setMontantTotal(dto.getMontantPaiement());
        location.setStatut(StatutLocationBienService.ACTIF);
        location.setUserUpdate(currentAgentId);
        location.setUpdatedAt(LocalDateTime.now());

        locationBienServiceRepository.save(location);
        return buildSuccessResponse(HttpStatus.OK, "Location confirmée après réception du paiement", "LOCATION_BIEN_SERVICE_CONFIRMED", toDto(location));
    }

    @Transactional
    public ResponseEntity<?> updateStatut(Integer id, UpdateStatutLocationBienServiceDTO dto) {
        LocationBienService location = findOrThrow(id);
        location.setStatut(dto.getStatut());
        location.setUpdatedAt(LocalDateTime.now());
        locationBienServiceRepository.save(location);
        return buildSuccessResponse(HttpStatus.OK, "Statut mis à jour", "LOCATION_BIEN_SERVICE_STATUT_UPDATED", toDto(location));
    }

    /**
     * Prolonge ou raccourcit une location ACTIVE en ajustant dateFin.
     * - Prolongation : si montantComplement fourni, un paiement additionnel est enregistré
     *   (le service ne recalcule pas automatiquement un montant obligatoire à payer,
     *   c'est l'agent qui négocie et saisit le complément constaté au comptoir).
     * - Raccourcissement : montantTotal recalculé au prorata, aucun paiement créé
     *   (le remboursement éventuel se gère hors de cette méthode).
     */
    /**
     * Prolonge ou raccourcit une location ACTIVE. Chaque complément de paiement
     * lié à une prolongation devient une nouvelle ligne tracée (type PROLONGATION),
     * sans jamais modifier les paiements précédents.
     */

    @Transactional
    public ResponseEntity<?> modifierDuree(Integer id, ModifierDureeLocationDTO dto, Integer currentAgentId) {
        LocationBienService location = findOrThrow(id);

        if (location.getStatut() != StatutLocationBienService.ACTIF) {
            throw new IllegalStateException("Seule une location ACTIVE peut voir sa durée modifiée");
        }
        if (!dto.getNouvelleDateFin().isAfter(location.getDateDebut())) {
            throw new IllegalArgumentException("La nouvelle date de fin doit être postérieure à la date de début");
        }

        LocalDateTime ancienneDateFin = location.getDateFin();
        boolean estProlongation = dto.getNouvelleDateFin().isAfter(ancienneDateFin);

        int nouvelleDuree = (int) Duration.between(location.getDateDebut(), dto.getNouvelleDateFin()).toDays();
        Double prixJournalier = location.getBienService().getPrixJournalier();
        double nouveauMontant = nouvelleDuree * (prixJournalier != null ? prixJournalier : 0);

        Double montantComplementEnregistre = null;
        Double tropPercu = null;
        // Si prolongation avec paiement complémentaire déclaré, on l'enregistre
        if (estProlongation ) {
            if (dto.getMontantComplement() != null && dto.getMontantComplement() > 0) {
                Paiement complement = Paiement.builder()
                        .datePaiement(LocalDateTime.now())
                        .montantPaiement(dto.getMontantComplement())
                        .modePaiement(dto.getModePaiementComplement())
                        .referencePaiement(dto.getReferencePaiementComplement())
                        .userCreate(currentAgentId)
                        .build();
                paiementRepository.save(complement);

                paiementLocationBienServiceRepository.save(PaiementLocationBienService.builder()
                        .idLocationBienService(location.getIdLocationBienService())
                        .idPaiement(complement.getIdPaiement())
                        .typePaiement(TypePaiementLocationBienService.PROLONGATION)
                        .build());
                montantComplementEnregistre = dto.getMontantComplement();
            }
        }else {
            // Raccourcissement : on compare le nouveau montant au total déjà encaissé
            double totalEncaisse = paiementLocationBienServiceRepository.findByIdLocationBienService(location.getIdLocationBienService())
                    .stream()
                    .mapToDouble(pl -> pl.getPaiement().getMontantPaiement())
                    .sum();
            double ecart = totalEncaisse - nouveauMontant;
            if (ecart > 0) {
                tropPercu = ecart; // à rembourser ou créditer, traitement hors système
            }
        }


        location.setDateFin(dto.getNouvelleDateFin());
        location.setDuree(nouvelleDuree);
        location.setMontantTotal(nouveauMontant);
        location.setUserUpdate(currentAgentId);
        location.setUpdatedAt(LocalDateTime.now());

        locationBienServiceRepository.save(location);

        ModifierDureeLocationResponseDTO reponse = ModifierDureeLocationResponseDTO.builder()
                .location(toDto(location))
                .montantComplementEnregistre(montantComplementEnregistre)
                .tropPercu(tropPercu)
                .build();

        String message = estProlongation
                ? "Location prolongée" + (montantComplementEnregistre != null ? ", paiement complémentaire enregistré" : "")
                : "Durée raccourcie" + (tropPercu != null ? ", trop-perçu à régulariser" : "");

        return buildSuccessResponse(HttpStatus.OK, message, "LOCATION_BIEN_SERVICE_DUREE_UPDATED", reponse);
    }

    /**
     * Annulation d'une demande de location, encore EN_ATTENTE uniquement.
     * - CLIENT : peut annuler uniquement sa propre demande.
     * - AGENT/ADMIN : peut annuler n'importe quelle demande (ex: bien plus disponible).
     * Une location déjà ACTIF (payée) ne peut plus être annulée par ce biais.
     */
    @Transactional
    public ResponseEntity<?> annuler(Integer id, UserDetailsImpl currentUser) {
        LocationBienService location = findOrThrow(id);

        boolean isClient = currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CLIENT"));

        if (isClient && !location.getClient().getIdUser().equals(currentUser.getIdUser())) {
            throw new AccessDeniedException("Vous ne pouvez annuler que vos propres demandes");
        }

        if (location.getStatut() != StatutLocationBienService.EN_ATTENTE) {
            throw new IllegalStateException("Seule une demande EN_ATTENTE peut être annulée");
        }

        location.setStatut(StatutLocationBienService.ANNULE);
        location.setUserUpdate(currentUser.getIdUser());
        location.setUpdatedAt(LocalDateTime.now());
        locationBienServiceRepository.save(location);

        return buildSuccessResponse(HttpStatus.OK, "Demande annulée", "LOCATION_BIEN_SERVICE_CANCELLED", null);
    }

    private LocationBienService findOrThrow(Integer id) {
        return locationBienServiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("locationBienService", id));
    }

    private LocationBienServiceResponseDTO toDto(LocationBienService l) {
        return LocationBienServiceResponseDTO.builder()
                .idLocationBienService(l.getIdLocationBienService())
                .idClient(l.getClient().getIdUser())
                .nomClient(l.getClient().getNom() + " " + l.getClient().getPrenom())
                .idBienService(l.getBienService().getIdBienService())
                .libelleBienService(l.getBienService().getLibelle())
                .destination(l.getDestination())
                .dateDebut(l.getDateDebut())
                .dateFin(l.getDateFin())
                .duree(l.getDuree())
                .montantTotal(l.getMontantTotal())
                .statut(l.getStatut())
                .build();
    }
}