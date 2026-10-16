package com.immobilier.gestionImmobiliere.modules.biens.services;

import com.immobilier.gestionImmobiliere.utils.VilleSpecification;
import com.immobilier.gestionImmobiliere.donnees.biens.model.BienService;
import com.immobilier.gestionImmobiliere.donnees.biens.model.LocationBienService;
import com.immobilier.gestionImmobiliere.donnees.biens.model.StatutBienService;
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
import com.immobilier.gestionImmobiliere.modules.biens.dto.responses.PaiementLocationBienServiceResponseDTO;
import com.immobilier.gestionImmobiliere.modules.biens.dto.responses.RemboursementResponseDTO;
import com.immobilier.gestionImmobiliere.modules.notifications.services.AlerteAgentService;
import com.immobilier.gestionImmobiliere.modules.notifications.services.AlerteUtilisateurService;
import com.immobilier.gestionImmobiliere.modules.user.jwtService.UserDetailsImpl;
import jakarta.persistence.criteria.Join;
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
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
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
    private final AlerteAgentService alerteAgentService;
    private final AlerteUtilisateurService alerteUtilisateurService;


    public LocationBienServiceService(LocationBienServiceRepository locationRepository, LocationBienServiceRepository locationBienServiceRepository, BienServiceRepository bienServiceRepository,
                                      PaiementRepository paiementRepository, UserRepository userRepository, PaiementLocationBienServiceRepository paiementLocationBienServiceRepository, RemboursementRepository remboursementRepository,
                                      AlerteAgentService alerteAgentService, AlerteUtilisateurService alerteUtilisateurService) {
        this.alerteUtilisateurService = alerteUtilisateurService;
        this.alerteAgentService = alerteAgentService;
        this.locationBienServiceRepository = locationBienServiceRepository;
        this.bienServiceRepository = bienServiceRepository;
        this.paiementRepository = paiementRepository;
        this.userRepository = userRepository;
        this.paiementLocationBienServiceRepository = paiementLocationBienServiceRepository;
        this.remboursementRepository = remboursementRepository;
    }

    // Locations ACTIF dont la fin tombe dans les JOURS_PREAVIS_ECHEANCE prochains jours (ou est déjà dépassée)
    public static final int JOURS_PREAVIS_ECHEANCE = 2;

    public ResponseEntity<?> getAll(StatutLocationBienService statut, String recherche, Boolean aEcheance, Integer idVille, Boolean sansVille, Pageable pageable, UserDetailsImpl currentUser) {
        boolean isClient = currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CLIENT"));

        // Filtres (statut, recherche) appliques cote base avant la pagination ; optionnels pour ne pas casser les autres espaces
        Page<LocationBienServiceResponseDTO> result = locationBienServiceRepository
                .findAll(filtrer(isClient ? currentUser.getIdUser() : null, statut, recherche, Boolean.TRUE.equals(aEcheance)).and(VilleSpecification.parVille(idVille, sansVille, "bienService", "secteur", "ville")), pageable)
                .map(this::toDto);

        return buildSuccessResponse(HttpStatus.OK, "Liste des locations", "LOCATION_BIEN_SERVICE_LIST", result);
    }

    // Recherche insensible a la casse sur le libelle du bien, le nom/prenom du client ou la destination
    private Specification<LocationBienService> filtrer(Integer idClient, StatutLocationBienService statut, String recherche, boolean aEcheance) {
        return (root, query, cb) -> {
            List<Predicate> predicats = new ArrayList<>();
            if (idClient != null) {
                predicats.add(cb.equal(root.get("client").get("idUser"), idClient));
            }
            if (statut != null) {
                predicats.add(cb.equal(root.get("statut"), statut));
            }
            if (aEcheance) {
                predicats.add(cb.equal(root.get("statut"), StatutLocationBienService.ACTIF));
                predicats.add(cb.lessThan(root.<LocalDateTime>get("dateFin"),
                        LocalDate.now().plusDays(JOURS_PREAVIS_ECHEANCE + 1L).atStartOfDay()));
            }
            if (recherche != null && !recherche.isBlank()) {
                String motif = "%" + recherche.trim().toLowerCase() + "%";
                Join<Object, Object> bien = root.join("bienService");
                Join<Object, Object> client = root.join("client");
                predicats.add(cb.or(
                        cb.like(cb.lower(bien.<String>get("libelle")), motif),
                        cb.like(cb.lower(cb.concat(cb.concat(client.<String>get("nom"), " "), client.<String>get("prenom"))), motif),
                        cb.like(cb.lower(cb.coalesce(root.<String>get("destination"), "")), motif)));
            }
            return cb.and(predicats.toArray(new Predicate[0]));
        };
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
    public ResponseEntity<?> create(CreateLocationBienServiceDTO dto, Integer idClient, Integer currentUserId) {
        BienService bien = bienServiceRepository.findById(dto.getIdBienService())
                .orElseThrow(() -> new ResourceNotFoundException("bienService", dto.getIdBienService()));
        User client = userRepository.findById(idClient)
                .orElseThrow(() -> new ResourceNotFoundException("user", idClient));

        if (dto.getDateFin().isBefore(dto.getDateDebut())) {
            throw new IllegalArgumentException("La date de fin doit être après la date de début");
        }

        long dureeEstimee = ChronoUnit.DAYS.between(dto.getDateDebut().toLocalDate(), dto.getDateFin().toLocalDate());

        if (dureeEstimee <= 0) {
            throw new IllegalArgumentException("La durée doit être d'au moins 1 jour");
        }

        BigDecimal prix = bien.getPrixJournalier() != null
                ? bien.getPrixJournalier()
                : BigDecimal.ZERO;

        BigDecimal montantEstime = prix
                .multiply(BigDecimal.valueOf(dureeEstimee))
                .setScale(2, RoundingMode.HALF_UP);

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
        synchroniserDisponibilite(bien);
        alerteAgentService.nouvelleDemandeLocation(location);
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
        BigDecimal coutJournalier = location.getBienService().getPrixJournalier();
        // Dates optionnelles : a defaut d'ajustement, on garde celles de la demande
        LocalDateTime dateDebut = dto.getDateDebut() != null ? dto.getDateDebut() : location.getDateDebut();
        LocalDateTime dateFin = dto.getDateFin() != null ? dto.getDateFin() : location.getDateFin();
        long njrs = ChronoUnit.DAYS.between(dateDebut.toLocalDate(), dateFin.toLocalDate());
        if (njrs <= 0) {
            throw new IllegalArgumentException("La date de fin doit être postérieure à la date de début");
        }
        BigDecimal coutMax = coutJournalier.multiply(BigDecimal.valueOf(njrs));

        if (dto.getMontantPaiement() == null) {
            throw new IllegalArgumentException("Le montant de paiement est obligatoire");
        }

        if (dto.getMontantPaiement().compareTo(coutMax) > 0) {
            dto.setMontantPaiement(coutMax);
        }

        if (location.getStatut() != StatutLocationBienService.EN_ATTENTE) {
            throw new IllegalStateException("Seule une location EN_ATTENTE peut être confirmée");
        }
        verifierBienLibre(location, dateDebut, dateFin);
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
                .paiement(paiement)
                .typePaiement(TypePaiementLocationBienService.NORMAL)
                .build());

        // Ajustement des dates si l'agent les a modifiées au comptoir
        long dureeFinale =  ChronoUnit.DAYS.between(dateDebut.toLocalDate(), dateFin.toLocalDate());


        location.setDateDebut(dateDebut);
        location.setDateFin(dateFin);
        location.setDuree(dureeFinale);
        location.setMontantTotal(dto.getMontantPaiement());
        location.setStatut(StatutLocationBienService.ACTIF);
        location.setUserUpdate(currentAgentId);
        location.setUpdatedAt(LocalDateTime.now());

        locationBienServiceRepository.save(location);
        synchroniserDisponibilite(location.getBienService());
        alerteUtilisateurService.locationConfirmee(location);
        alerteUtilisateurService.paiementLocationRecu(location, paiement, false);
        return buildSuccessResponse(HttpStatus.OK, "Location confirmée après réception du paiement", "LOCATION_BIEN_SERVICE_CONFIRMED", toDto(location));
    }

    @Transactional
    public ResponseEntity<?> updateStatut(Integer id, UpdateStatutLocationBienServiceDTO dto) {
        LocationBienService location = findOrThrow(id);
        // Les autres transitions ont leur propre endpoint (confirmer, annuler) ou n'ont pas de sens (retour en arriere)
        boolean cloture = location.getStatut() == StatutLocationBienService.ACTIF && dto.getStatut() == StatutLocationBienService.TERMINE;
        boolean annulation = location.getStatut() == StatutLocationBienService.EN_ATTENTE && dto.getStatut() == StatutLocationBienService.ANNULE;
        if (!cloture && !annulation) {
            throw new IllegalStateException("Transition de statut non autorisée : " + location.getStatut() + " vers " + dto.getStatut());
        }
        if (cloture) {
            // Clôture anticipée : uniquement si l'agent l'a demandé, la durée est ramenée à aujourd'hui
            // et le montant recalculé au prorata (trop-perçu à rembourser, comme un raccourcissement).
            if (Boolean.TRUE.equals(dto.getRaccourcir())) {
                raccourcirJusquAujourdhui(location);
            } else if (LocalDate.now().isBefore(location.getDateFin().toLocalDate())) {
                // Sans raccourcissement, une location ne se termine pas avant sa date de fin : l'agent
                // peut ajuster la durée lui-même (bouton Durée) puis rembourser avant de clôturer.
                throw new IllegalStateException("La date de fin prévue n'est pas atteinte : ajustez d'abord la durée de la location (bouton Durée), ou clôturez en ramenant la fin à aujourd'hui");
            }
            // Pas de clôture tant qu'un trop-perçu n'a pas été remboursé au client : la location reste
            // ACTIVE (durée déjà ramenée à aujourd'hui) en attendant le remboursement.
            BigDecimal solde = calculerEncaisseRembourseSolde(location)[2];
            if (solde.compareTo(BigDecimal.ZERO) < 0) {
                location.setUpdatedAt(LocalDateTime.now());
                locationBienServiceRepository.save(location);
                return buildSuccessResponse(HttpStatus.OK,
                        "Durée ramenée à aujourd'hui : un trop-perçu de " + solde.negate().stripTrailingZeros().toPlainString()
                                + " doit être remboursé au client avant de terminer la location",
                        "LOCATION_BIEN_SERVICE_REMBOURSEMENT_REQUIS", toDto(location));
            }
        }
        location.setStatut(dto.getStatut());
        location.setUpdatedAt(LocalDateTime.now());
        locationBienServiceRepository.save(location);
        synchroniserDisponibilite(location.getBienService());
        if (cloture) alerteUtilisateurService.locationTerminee(location);
        else alerteUtilisateurService.locationAnnulee(location);
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

        if (dto.getNouvelleDateFin() == null) {
            throw new IllegalArgumentException("La nouvelle date de fin est obligatoire");
        }

        if (!dto.getNouvelleDateFin().isAfter(location.getDateDebut())) {
            throw new IllegalArgumentException("La nouvelle date de fin doit être postérieure à la date de début");
        }

        boolean estProlongation = dto.getNouvelleDateFin().isAfter(location.getDateFin());
        if (estProlongation) {
            verifierBienLibre(location, location.getDateDebut(), dto.getNouvelleDateFin());
        }

        long nouvelleDuree =  ChronoUnit.DAYS.between(location.getDateDebut().toLocalDate(), dto.getNouvelleDateFin().toLocalDate());

        if (nouvelleDuree <= 0) {
            throw new IllegalArgumentException("La durée doit être d'au moins 1 jour");
        }

        BigDecimal prixJournalier = location.getBienService().getPrixJournalier();

        if (prixJournalier == null || prixJournalier.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Prix journalier manquant ou invalide");
        }

        BigDecimal nouveauMontant = prixJournalier.multiply(BigDecimal.valueOf(nouvelleDuree)).setScale(2, RoundingMode.HALF_UP);

        if (estProlongation && dto.getMontantComplement() != null && dto.getMontantComplement().compareTo(BigDecimal.ZERO) > 0) {
            // Montant réellement dû = nouveau total - (déjà encaissé - déjà remboursé) : un trop-perçu
            // issu d'un raccourcissement antérieur est réutilisé avant de demander un nouveau paiement.
            BigDecimal dejaVerse = paiementLocationBienServiceRepository.sumMontantByLocation(location.getIdLocationBienService())
                    .subtract(remboursementRepository.sumMontantByEntite(TypeEntiteRemboursement.LOCATION_BIEN_SERVICE, location.getIdLocationBienService()));
            BigDecimal resteDu = nouveauMontant.subtract(dejaVerse);
            if (resteDu.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalStateException("Aucun complément à encaisser : les sommes déjà versées couvrent la nouvelle durée");
            }
            if (dto.getMontantComplement().compareTo(resteDu) > 0) {
                throw new IllegalArgumentException("Le complément (" + dto.getMontantComplement().stripTrailingZeros().toPlainString()
                        + ") dépasse le reste dû (" + resteDu.stripTrailingZeros().toPlainString() + ")");
            }
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
                    .paiement(complement)
                    .typePaiement(TypePaiementLocationBienService.PROLONGATION)
                    .build());
            alerteUtilisateurService.paiementLocationRecu(location, complement, true);
        }

        location.setDateFin(dto.getNouvelleDateFin());
        location.setDuree(nouvelleDuree);
        location.setMontantTotal(nouveauMontant);
        location.setUserUpdate(currentAgentId);
        location.setUpdatedAt(LocalDateTime.now());

        locationBienServiceRepository.save(location);

        BigDecimal[] chiffres = calculerEncaisseRembourseSolde(location);

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
    private BigDecimal[] calculerEncaisseRembourseSolde(LocationBienService location) {
        BigDecimal totalEncaisse = paiementLocationBienServiceRepository.sumMontantByLocation(location.getIdLocationBienService());

        BigDecimal totalRembourse = remboursementRepository.sumMontantByEntite(TypeEntiteRemboursement.LOCATION_BIEN_SERVICE, location.getIdLocationBienService());

        BigDecimal solde = location.getMontantTotal().subtract(totalEncaisse).add(totalRembourse);
        return new BigDecimal []{totalEncaisse, totalRembourse, solde};
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

        BigDecimal [] chiffres = calculerEncaisseRembourseSolde(location);
        BigDecimal soldeActuel = chiffres[2];

        if (soldeActuel.compareTo(BigDecimal.ZERO) >= 0) {
            throw new IllegalStateException("Aucun trop-perçu à rembourser sur cette location");
        }
        BigDecimal tropPercuDisponible = soldeActuel.negate();

        BigDecimal montant = dto.getMontant();
        if (montant == null) {
            throw new IllegalArgumentException("Le montant du remboursement est obligatoire");
        }
        if (dto.getMontant().compareTo(tropPercuDisponible) > 0) {
            throw new IllegalArgumentException(
                    "Le montant du remboursement (" + dto.getMontant() +
                            ") dépasse le trop-perçu disponible (" + tropPercuDisponible + ")");
        }

        Paiement paiement = Paiement.builder()
                .datePaiement(LocalDateTime.now())
                .montantPaiement(dto.getMontant())
                .modePaiement(dto.getModeRemboursement())
                .referencePaiement(dto.getReference())
                .sens(SensPaiement.SORTIE)
                .userCreate(currentAgentId)
                .build();
        paiementRepository.save(paiement);

        Remboursement remboursement = Remboursement.builder()
                .entiteType(TypeEntiteRemboursement.LOCATION_BIEN_SERVICE)
                .entiteId(location.getIdLocationBienService())
                .paiement(paiement)
                .motif(dto.getMotif())
                .userCreate(currentAgentId)
                .build();
        remboursementRepository.save(remboursement);

        RemboursementResponseDTO reponse = RemboursementResponseDTO.builder()
                .idRemboursement(remboursement.getIdRemboursement())
                .montant(paiement.getMontantPaiement())
                .modeRemboursement(paiement.getModePaiement())
                .reference(paiement.getReferencePaiement())
                .motif(remboursement.getMotif())
                .dateRemboursement(paiement.getDatePaiement())
                .build();

        alerteUtilisateurService.remboursementEffectue(location, remboursement.getIdRemboursement(), paiement.getMontantPaiement());
        return buildSuccessResponse(HttpStatus.CREATED, "Remboursement enregistré", "REMBOURSEMENT_CREATED", reponse);
    }

    /**
     * Historique des remboursements d'une location.
     */
    public ResponseEntity<?> getRemboursements(Integer id, UserDetailsImpl currentUser) {
        LocationBienService location = findOrThrow(id); // vérifie l'existence de la location
        boolean isClient = currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CLIENT"));
        if (isClient && !location.getClient().getIdUser().equals(currentUser.getIdUser())) {
            throw new AccessDeniedException("Vous n'avez pas accès à cette location");
        }
        List<RemboursementResponseDTO> result = remboursementRepository.findByEntiteTypeAndEntiteIdAndIsDeletedFalse(TypeEntiteRemboursement.LOCATION_BIEN_SERVICE,id)
                .stream()
                .map(r -> RemboursementResponseDTO.builder()
                        .idRemboursement(r.getIdRemboursement())
                        .montant(r.getPaiement().getMontantPaiement())
                        .modeRemboursement(r.getPaiement().getModePaiement())
                        .reference(r.getPaiement().getReferencePaiement())
                        .motif(r.getMotif())
                        .dateRemboursement(r.getPaiement().getDatePaiement())
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
        synchroniserDisponibilite(location.getBienService());
        // Le client qui annule lui-meme sa demande n'a pas besoin d'etre alerte
        if (!isClient) alerteUtilisateurService.locationAnnulee(location);

        return buildSuccessResponse(HttpStatus.OK, "Demande annulée", "LOCATION_BIEN_SERVICE_CANCELLED", null);
    }

    // Si la date de fin prévue n'est pas encore atteinte, la ramène à aujourd'hui (au moins 1 jour de location)
    private void raccourcirJusquAujourdhui(LocationBienService location) {
        LocalDate debut = location.getDateDebut().toLocalDate();
        LocalDate aujourdhui = LocalDate.now();
        if (!aujourdhui.isBefore(location.getDateFin().toLocalDate())) return;
        LocalDate nouvelleFin = aujourdhui.isAfter(debut) ? aujourdhui : debut.plusDays(1);
        long duree = ChronoUnit.DAYS.between(debut, nouvelleFin);
        BigDecimal prix = location.getBienService().getPrixJournalier();
        if (prix == null) return;
        location.setDateFin(nouvelleFin.atStartOfDay());
        location.setDuree(duree);
        location.setMontantTotal(prix.multiply(BigDecimal.valueOf(duree)).setScale(2, RoundingMode.HALF_UP));
    }

    /**
     * La disponibilité du bien suit le processus de location (jamais saisie à la main pour RESERVEE / LOUEE) :
     * une location ACTIF le rend LOUEE, une demande EN_ATTENTE le rend RESERVEE, sinon DISPONIBLE.
     * Un bien EN_MAINTENANCE (décision manuelle) n'est pas touché.
     */
    private void synchroniserDisponibilite(BienService bien) {
        if (bien.getDisponibilite() == StatutBienService.EN_MAINTENANCE) return;
        Integer idBien = bien.getIdBienService();
        StatutBienService cible;
        if (locationBienServiceRepository.existsByBienService_IdBienServiceAndStatut(idBien, StatutLocationBienService.ACTIF)) {
            cible = StatutBienService.LOUEE;
        } else if (locationBienServiceRepository.existsByBienService_IdBienServiceAndStatut(idBien, StatutLocationBienService.EN_ATTENTE)) {
            cible = StatutBienService.RESERVEE;
        } else {
            cible = StatutBienService.DISPONIBLE;
        }
        if (bien.getDisponibilite() != cible) {
            bien.setDisponibilite(cible);
            bienServiceRepository.save(bien);
        }
    }

    // Refuse si une autre location ACTIF du meme bien chevauche la periode demandee
    private void verifierBienLibre(LocationBienService location, LocalDateTime debut, LocalDateTime fin) {
        long conflits = locationBienServiceRepository.countChevauchements(
                location.getBienService().getIdBienService(), StatutLocationBienService.ACTIF,
                location.getIdLocationBienService(), debut, fin);
        if (conflits > 0) {
            throw new IllegalStateException("Ce bien est déjà loué sur tout ou partie de cette période");
        }
    }

    private LocationBienService findOrThrow(Integer id) {
        return locationBienServiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("locationBienService", id));
    }

    private LocationBienServiceResponseDTO toDto(LocationBienService l) {

        List<PaiementLocationBienServiceResponseDTO> historique = paiementLocationBienServiceRepository
                .findByIdLocationBienService(l.getIdLocationBienService())
                .stream()
                .map(pl -> PaiementLocationBienServiceResponseDTO.builder()
                        .idPaiement(pl.getIdPaiement())
                        .montant(pl.getPaiement().getMontantPaiement())
                        .modePaiement(pl.getPaiement().getModePaiement())
                        .typePaiement(pl.getTypePaiement())
                        .datePaiement(pl.getPaiement().getDatePaiement())
                        .build())
                .toList();

        BigDecimal[] chiffres = calculerEncaisseRembourseSolde(l);

        return LocationBienServiceResponseDTO.builder()
                .idLocationBienService(l.getIdLocationBienService())
                .idClient(l.getClient().getIdUser())
                .nomClient(l.getClient().getNom() + " " + l.getClient().getPrenom())
                .emailClient(l.getClient().getEmail())
                .telephoneClient(l.getClient().getTelephone())
                .idBienService(l.getBienService().getIdBienService())
                .libelleBienService(l.getBienService().getLibelle())
                .destination(l.getDestination())
                .dateDebut(l.getDateDebut())
                .dateFin(l.getDateFin())
                .duree(l.getDuree())
                .prixJournalier(l.getBienService().getPrixJournalier())
                .montantTotal(l.getMontantTotal())
                .statut(l.getStatut())
                .historiquePaiements(historique)
                .totalEncaisse(chiffres[0])
                .totalRembourse(chiffres[1])
                .solde(chiffres[2])
                .build();
    }
}