package com.immobilier.gestionImmobiliere.modules.biens.services;

import com.immobilier.gestionImmobiliere.donnees.biens.model.BienService;
import com.immobilier.gestionImmobiliere.donnees.biens.model.LocationBienService;
import com.immobilier.gestionImmobiliere.donnees.biens.model.StatutLocationBienService;
import com.immobilier.gestionImmobiliere.donnees.biens.repository.BienServiceRepository;
import com.immobilier.gestionImmobiliere.donnees.biens.repository.LocationBienServiceRepository;
import com.immobilier.gestionImmobiliere.donnees.paiements.model.*;
import com.immobilier.gestionImmobiliere.donnees.paiements.repository.PaiementLocationBienServiceRepository;
import com.immobilier.gestionImmobiliere.donnees.paiements.repository.PaiementRepository;
import com.immobilier.gestionImmobiliere.donnees.paiements.repository.RemboursementRepository;
import com.immobilier.gestionImmobiliere.donnees.user.model.User;
import com.immobilier.gestionImmobiliere.donnees.user.repository.UserRepository;
import com.immobilier.gestionImmobiliere.exceptions.ResourceNotFoundException;
import com.immobilier.gestionImmobiliere.modules.biens.dto.requests.*;
import com.immobilier.gestionImmobiliere.modules.biens.dto.responses.LocationBienServiceResponseDTO;
import com.immobilier.gestionImmobiliere.modules.biens.dto.responses.ModifierDureeLocationResponseDTO;
import com.immobilier.gestionImmobiliere.modules.biens.dto.responses.RemboursementResponseDTO;
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
import java.util.List;

import static com.immobilier.gestionImmobiliere.utils.BuildSuccessResponse.buildSuccessResponse;

@Service
public class LocationBienServiceService {

    private final LocationBienServiceRepository locationBienServiceRepository;
    private final BienServiceRepository bienServiceRepository;
    private final PaiementRepository paiementRepository;
    private final UserRepository userRepository;
    private final PaiementLocationBienServiceRepository paiementLocationBienServiceRepository;
    private final RemboursementRepository remboursementRepository;

    public LocationBienServiceService(LocationBienServiceRepository locationRepository, LocationBienServiceRepository locationBienServiceRepository, BienServiceRepository bienServiceRepository,
                                      PaiementRepository paiementRepository, UserRepository userRepository, PaiementLocationBienServiceRepository paiementLocationBienServiceRepository, RemboursementRepository remboursementRepository) {
        this.locationBienServiceRepository = locationBienServiceRepository;
        this.bienServiceRepository = bienServiceRepository;
        this.paiementRepository = paiementRepository;
        this.userRepository = userRepository;
        this.paiementLocationBienServiceRepository = paiementLocationBienServiceRepository;
        this.remboursementRepository = remboursementRepository;
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

        boolean estProlongation = dto.getNouvelleDateFin().isAfter(location.getDateFin());

        int nouvelleDuree = (int) Duration.between(location.getDateDebut(), dto.getNouvelleDateFin()).toDays();
        Double prixJournalier = location.getBienService().getPrixJournalier();
        double nouveauMontant = nouvelleDuree * (prixJournalier != null ? prixJournalier : 0);

        if (estProlongation && dto.getMontantComplement() != null && dto.getMontantComplement() > 0) {
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
        }

        location.setDateFin(dto.getNouvelleDateFin());
        location.setDuree(nouvelleDuree);
        location.setMontantTotal(nouveauMontant);
        location.setUserUpdate(currentAgentId);
        location.setUpdatedAt(LocalDateTime.now());

        locationBienServiceRepository.save(location);

        double[] chiffres = calculerEncaisseRembourseSolde(location);

        ModifierDureeLocationResponseDTO reponse = ModifierDureeLocationResponseDTO.builder()
                .location(toDto(location))
                .totalEncaisse(chiffres[0])
                .totalRembourse(chiffres[1])
                .solde(chiffres[2])
                .build();

        String message = estProlongation ? "Location prolongée" : "Durée de location raccourcie";
        return buildSuccessResponse(HttpStatus.OK, message, "LOCATION_BIEN_SERVICE_DUREE_UPDATED", reponse);
    }

    /**
     * Calcule le solde net d'une location : montantTotal - totalEncaisse + totalRembourse.
     * > 0 : il reste à payer.
     * < 0 : trop-perçu non encore remboursé.
     * = 0 : compte soldé.
     */
    private double[] calculerEncaisseRembourseSolde(LocationBienService location) {
        double totalEncaisse = paiementLocationBienServiceRepository.findByIdLocationBienService(location.getIdLocationBienService())
                .stream()
                .mapToDouble(pl -> pl.getPaiement().getMontantPaiement())
                .sum();

        double totalRembourse = remboursementRepository.findByEntiteTypeAndEntiteIdAndIsDeletedFalse(TypeEntiteRemboursement.LOCATION_BIEN_SERVICE, location.getIdLocationBienService())
                .stream()
                .mapToDouble(Remboursement::getMontant)
                .sum();

        double solde = location.getMontantTotal() - totalEncaisse + totalRembourse;
        return new double[]{totalEncaisse, totalRembourse, solde};
    }

    /**
     * Enregistre un remboursement effectif suite à un trop-perçu (raccourcissement).
     * Refuse si le montant dépasse le trop-perçu réellement constaté (protection anti-erreur).
     */
    @Transactional
    public ResponseEntity<?> rembourser(Integer id, CreerRemboursementDTO dto, Integer currentAgentId) {
        LocationBienService location = findOrThrow(id);

        if (location.getStatut() != StatutLocationBienService.ACTIF) {
            throw new IllegalStateException("Le remboursement n'est possible que sur une location ACTIVE");
        }

        double[] chiffres = calculerEncaisseRembourseSolde(location);
        double soldeActuel = chiffres[2];

        if (soldeActuel >= 0) {
            throw new IllegalStateException("Aucun trop-perçu à rembourser sur cette location");
        }
        double tropPercuDisponible = -soldeActuel;
        if (dto.getMontant() > tropPercuDisponible) {
            throw new IllegalArgumentException(
                    "Le montant du remboursement (" + dto.getMontant() +
                            ") dépasse le trop-perçu disponible (" + tropPercuDisponible + ")");
        }

        Remboursement remboursement = Remboursement.builder()
                .entiteType(TypeEntiteRemboursement.LOCATION_BIEN_SERVICE)
                .entiteId(location.getIdLocationBienService())
                .montant(dto.getMontant())
                .modeRemboursement(dto.getModeRemboursement())
                .reference(dto.getReference())
                .motif(dto.getMotif())
                .userCreate(currentAgentId)
                .build();
        remboursementRepository.save(remboursement);

        RemboursementResponseDTO reponse = RemboursementResponseDTO.builder()
                .idRemboursement(remboursement.getIdRemboursement())
                .montant(remboursement.getMontant())
                .modeRemboursement(remboursement.getModeRemboursement())
                .reference(remboursement.getReference())
                .motif(remboursement.getMotif())
                .dateRemboursement(remboursement.getCreatedAt())
                .build();

        return buildSuccessResponse(HttpStatus.CREATED, "Remboursement enregistré", "REMBOURSEMENT_CREATED", reponse);
    }

    /**
     * Historique des remboursements d'une location.
     */
    public ResponseEntity<?> getRemboursements(Integer id) {
        findOrThrow(id); // vérifie l'existence de la location
        List<RemboursementResponseDTO> result = remboursementRepository.findByEntiteTypeAndEntiteIdAndIsDeletedFalse(TypeEntiteRemboursement.LOCATION_BIEN_SERVICE,id)
                .stream()
                .map(r -> RemboursementResponseDTO.builder()
                        .idRemboursement(r.getIdRemboursement())
                        .montant(r.getMontant())
                        .modeRemboursement(r.getModeRemboursement())
                        .reference(r.getReference())
                        .motif(r.getMotif())
                        .dateRemboursement(r.getCreatedAt())
                        .build())
                .toList();
        return buildSuccessResponse(HttpStatus.OK, "Historique des remboursements", "REMBOURSEMENT_LIST", result);
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