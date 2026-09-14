package com.immobilier.gestionImmobiliere.modules.contrats.services;

import com.immobilier.gestionImmobiliere.donnees.biens.model.Maison;
import com.immobilier.gestionImmobiliere.donnees.biens.model.StatutMaison;
import com.immobilier.gestionImmobiliere.donnees.biens.repository.MaisonRepository;
import com.immobilier.gestionImmobiliere.donnees.contrats.model.ContratLocation;
import com.immobilier.gestionImmobiliere.donnees.contrats.model.DecompteSortie;
import com.immobilier.gestionImmobiliere.donnees.contrats.model.StatutDecompteSortie;
import com.immobilier.gestionImmobiliere.donnees.contrats.model.StatutLocation;
import com.immobilier.gestionImmobiliere.donnees.contrats.repository.ContratLocationRepository;
import com.immobilier.gestionImmobiliere.donnees.contrats.repository.DecompteSortieRepository;
import com.immobilier.gestionImmobiliere.donnees.paiements.model.Paiement;
import com.immobilier.gestionImmobiliere.donnees.paiements.model.SensPaiement;
import com.immobilier.gestionImmobiliere.donnees.paiements.repository.EcheanceLoyerRepository;
import com.immobilier.gestionImmobiliere.donnees.paiements.repository.PaiementRepository;
import com.immobilier.gestionImmobiliere.donnees.user.model.User;
import com.immobilier.gestionImmobiliere.donnees.user.repository.UserRepository;
import com.immobilier.gestionImmobiliere.exceptions.InvalidStatutTransitionException;
import com.immobilier.gestionImmobiliere.exceptions.MaisonIndisponibleException;
import com.immobilier.gestionImmobiliere.exceptions.ResourceNotFoundException;
import com.immobilier.gestionImmobiliere.modules.contrats.dto.requests.CreateContratLocationDTO;
import com.immobilier.gestionImmobiliere.modules.contrats.dto.requests.ResilierLocationDTO;
import com.immobilier.gestionImmobiliere.modules.contrats.dto.requests.TerminerLocationDTO;
import com.immobilier.gestionImmobiliere.modules.contrats.dto.responses.ContratLocationResponseDTO;
import com.immobilier.gestionImmobiliere.modules.contrats.dto.responses.DecompteSortieResponseDTO;
import com.immobilier.gestionImmobiliere.modules.paiements.dto.requests.ConfirmerReglementSortieDTO;
import com.immobilier.gestionImmobiliere.modules.paiements.services.EcheanceGenerationService;
import com.immobilier.gestionImmobiliere.modules.paiements.services.EcheanceService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static com.immobilier.gestionImmobiliere.utils.BuildSuccessResponse.buildSuccessResponse;

@Service
public class ContratLocationService {

    private final ContratLocationRepository locationRepository;
    private final MaisonRepository maisonRepository;
    private final UserRepository userRepository;
    private final EcheanceGenerationService echeanceGenerationService;
    private final EcheanceService echeanceService;
    private final EcheanceLoyerRepository echeanceLoyerRepository;
    private final DecompteSortieRepository decompteSortieRepository;
    private final PaiementRepository paiementRepository;


    public ContratLocationService(ContratLocationRepository locationRepository, MaisonRepository maisonRepository, UserRepository userRepository, EcheanceGenerationService echeanceGenerationService, EcheanceService echeanceService, EcheanceLoyerRepository echeanceLoyerRepository, DecompteSortieRepository decompteSortieRepository, PaiementRepository paiementRepository) {
        this.locationRepository = locationRepository;
        this.maisonRepository = maisonRepository;
        this.userRepository = userRepository;
        this.echeanceGenerationService = echeanceGenerationService;
        this.echeanceService = echeanceService;
        this.echeanceLoyerRepository = echeanceLoyerRepository;
        this.decompteSortieRepository = decompteSortieRepository;
        this.paiementRepository = paiementRepository;
    }

    /**
     * Version "brute" protégée par @PostAuthorize — DOIT être appelée
     * depuis un autre bean (le controller) pour que le proxy Spring intercepte.
     */
    @PostAuthorize(
            "hasAnyRole('ADMIN','AGENT') " +
                    "or returnObject.idLocataire == authentication.principal.idUser " +
                    "or @contratLocationSecurity.isProprietaire(returnObject.idMaison, authentication.principal.idUser)"
    )
    public ContratLocationResponseDTO getContratById(Integer id) {
        return toDto(findOrThrow(id));
    }

    /**
     * Liste filtrée selon le rôle courant :
     * - ADMIN/AGENT : accès total
     * - CLIENT : uniquement ses propres contrats (idLocataire forcé)
     * - BAILLEUR : uniquement les contrats sur ses cours
     */
    public ResponseEntity<?> getAllForCurrentUser(Integer idMaison, Integer idLocataire,
                                                  Integer currentUserId, boolean isAdminOrAgent,
                                                  boolean isBailleur, Pageable pageable) {
        Page<ContratLocation> page;

        if (isAdminOrAgent) {
            page = idMaison != null
                    ? locationRepository.findByMaison_IdMaison(idMaison, pageable)
                    : (idLocataire != null
                    ? locationRepository.findByLocataire_IdUser(idLocataire, pageable)
                    : locationRepository.findAll(pageable));
        } else if (isBailleur) {
            page = locationRepository.findByMaison_Cour_Proprietaire_IdUser(currentUserId, pageable);
        } else {
            // CLIENT : filtre imposé, ignore toute tentative de consulter un autre id
            page = locationRepository.findByLocataire_IdUser(currentUserId, pageable);
        }

        return buildSuccessResponse(HttpStatus.OK, "Liste des contrats de location", "LOCATION_LIST", page.map(this::toDto));
    }

    /**
     * Point d'entrée standard — exige que la maison soit DISPONIBLE (F14).
     */
    @Transactional
    public ContratLocationResponseDTO createFromReservation(CreateContratLocationDTO dto, Integer currentUserId) {
        ContratLocation location = creerContrat(dto, currentUserId, false);
        return toDto(location);
    }


    /**
     * Point d'entrée réservé aux conversions de réservation confirmée (F21).
     * Saute la vérification StatutMaison.DISPONIBLE : la maison est légitimement
     * en RESERVEE à ce stade, et c'est précisément l'état attendu ici — pas un
     * contournement, mais une précondition différente et explicite.
     */

    private ContratLocation creerContrat(CreateContratLocationDTO dto, Integer currentUserId, boolean exigerDisponible) {
        Maison maison = maisonRepository.findById(dto.getIdMaison())
                .orElseThrow(() -> new ResourceNotFoundException("maison", dto.getIdMaison()));
        User locataire = userRepository.findById(dto.getIdLocataire())
                .orElseThrow(() -> new ResourceNotFoundException("locataire", dto.getIdLocataire()));

        if (exigerDisponible && maison.getStatut() != StatutMaison.DISPONIBLE) {
            throw new MaisonIndisponibleException(maison.getIdMaison());
        }
        if (!exigerDisponible && maison.getStatut() != StatutMaison.RESERVEE) {
            // Garde-fou : une conversion ne doit partir que d'une maison réservée
            throw new MaisonIndisponibleException(maison.getIdMaison());
        }

        ContratLocation location = ContratLocation.builder()
                .locataire(locataire)
                .maison(maison)
                .dateEntree(dto.getDateEntree())
                .dateSortie(dto.getDateSortie())
                .montantLoyer(dto.getMontantLoyer())
                .typeContrat(dto.getTypeContrat())
                .etatDesLieuxEntree(dto.getEtatDesLieuxEntree())
                .statut(StatutLocation.ACTIF)
                .userCreate(currentUserId)
                .build();
        locationRepository.save(location);

        maison.setStatut(StatutMaison.LOUEE);
        maisonRepository.save(maison);

        echeanceGenerationService.genererEcheancesLocation(location);

        return location;
    }

    @Transactional
    public ResponseEntity<?> terminer(Integer id, TerminerLocationDTO dto, Integer currentUserId) {
        return cloturerContrat(id, StatutLocation.TERMINE, dto.getEtatDesLieuxSortie(),
                dto.getDateSortie(), dto.getFraisReparation(), currentUserId, "LOCATION_TERMINEE", "Contrat terminé");
    }

    @Transactional
    public ResponseEntity<?> resilierContratLocation(Integer id, ResilierLocationDTO dto, Integer currentUserId) {
        return cloturerContrat(id, StatutLocation.RESILIE, dto.getEtatDesLieuxSortie(),
                dto.getDateSortie(), dto.getCoutReparation(), currentUserId, "LOCATION_RESILIEE", "Contrat résilié");
    }

    /**
     * Logique commune à terminer() et resilierContratLocation() :
     *  Change le statut du contrat, libère la maison.
     *  Supprime les échéances LOCATION futures non payées.
     *  Calcule le règlement de sortie : arriérés déduits d'abord sur l'avance,
     *    puis sur la caution ; coût de réparation déduit sur le reste de la caution ;
     *    manquant à charge du locataire si la caution ne suffit pas, sinon
     *    remboursement du solde.
     */
    private ResponseEntity<?> cloturerContrat(Integer id, StatutLocation nouveauStatut, String etatDesLieuxSortie, LocalDateTime dateSortie, BigDecimal coutReparation, Integer currentUserId, String code, String messageBase) {
        ContratLocation location = findOrThrow(id);

        if (location.getStatut() != StatutLocation.ACTIF) {
            throw new InvalidStatutTransitionException(location.getStatut().name(), nouveauStatut.name());
        }

        LocalDateTime dateSortieEffective = dateSortie != null ? dateSortie : LocalDateTime.now();

        location.setStatut(nouveauStatut);
        location.setEtatDesLieuxSortie(etatDesLieuxSortie);
        location.setDateSortie(dateSortieEffective);
        location.setUserUpdate(currentUserId);
        locationRepository.save(location);

        Maison maison = location.getMaison();
        maison.setStatut(StatutMaison.DISPONIBLE);
        maisonRepository.save(maison);

        echeanceService.supprimerEcheancesFutures(location.getIdContratLocation(), LocalDate.now());

        DecompteSortie decompte = calculerDecompteSortie(location, maison, dateSortieEffective, coutReparation, currentUserId);

        String message = messageBase + ", maison redevenue disponible. " +
                (decompte.getMontantARembourser().compareTo(BigDecimal.ZERO) > 0
                        ? "Montant à rembourser au locataire : " + decompte.getMontantARembourser()
                        : decompte.getMontantManquant().compareTo(BigDecimal.ZERO) > 0
                        ? "Montant manquant dû par le locataire : " + decompte.getMontantManquant()
                        : "Compte soldé, aucun mouvement nécessaire.");

        return buildSuccessResponse(HttpStatus.OK, message, code, toDtoDecompte(decompte));
    }

    /**
     * Calcule et persiste le règlement de sortie, selon la règle :
     * arriérés -> déduits sur avance, puis sur caution -> coût réparation
     * remboursement (agence rend le solde).
     */
    private DecompteSortie calculerDecompteSortie(ContratLocation location, Maison maison,
                                                  LocalDateTime dateSortie, BigDecimal coutReparation,
                                                  Integer currentUserId) {
        BigDecimal avanceReference = maison.getAvance() != null ? maison.getAvance() : BigDecimal.ZERO;
        BigDecimal cautionReference = maison.getCaution() != null ? maison.getCaution() : BigDecimal.ZERO;

        BigDecimal arrieres = echeanceLoyerRepository.sumArrieresParContrat(location.getIdContratLocation());
        if (arrieres == null) arrieres = BigDecimal.ZERO;

        // Pot combiné : arriérés + réparations, consommés sur avance PUIS caution
        BigDecimal totalADeduire = arrieres.add(coutReparation);

        BigDecimal deduitAvance = totalADeduire.min(avanceReference);
        BigDecimal resteApresAvance = totalADeduire.subtract(deduitAvance);

        BigDecimal deduitCaution = resteApresAvance.min(cautionReference);
        BigDecimal manquant = resteApresAvance.subtract(deduitCaution);

        BigDecimal avanceRestante = avanceReference.subtract(deduitAvance);
        BigDecimal cautionRestante = cautionReference.subtract(deduitCaution);
        BigDecimal montantARembourser = avanceRestante.add(cautionRestante);

        DecompteSortie decompte = DecompteSortie.builder()
                .contratLocation(location)
                .dateSortie(dateSortie)
                .montantAvanceReference(avanceReference)
                .montantCautionReference(cautionReference)
                .montantArrieres(arrieres)
                .coutReparation(coutReparation)
                .montantDeduitAvance(deduitAvance)
                .montantDeduitCaution(deduitCaution)
                .montantManquant(manquant)
                .montantARembourser(montantARembourser)
                .statut(StatutDecompteSortie.EN_ATTENTE)
                .userCreate(currentUserId)
                .build();

        return decompteSortieRepository.save(decompte);
    }

    /**
     * Étape 2 — l'agent déclenche le règlement réel : remboursement au locataire
     * (sens SORTIE) si un solde positif existe, ou encaissement du manquant
     * (sens ENTREE) si la caution ne couvrait pas tout. Si les deux sont à zéro,
     * le compte est simplement soldé sans mouvement financier.
     */
    @Transactional
    public ResponseEntity<?> reglerDecompteSortie(Integer idDecompte, ConfirmerReglementSortieDTO dto, Integer currentAgentId) {
        DecompteSortie decompte = decompteSortieRepository.findById(idDecompte)
                .orElseThrow(() -> new ResourceNotFoundException("décompte de sortie", idDecompte));

        if (decompte.getStatut() != StatutDecompteSortie.EN_ATTENTE) {
            throw new IllegalStateException("Ce décompte de sortie a déjà été réglé");
        }

        BigDecimal montant = decompte.getMontantARembourser().compareTo(BigDecimal.ZERO) > 0
                ? decompte.getMontantARembourser()
                : decompte.getMontantManquant();

        if (montant.compareTo(BigDecimal.ZERO) > 0) {
            SensPaiement sens = decompte.getMontantARembourser().compareTo(BigDecimal.ZERO) > 0
                    ? SensPaiement.SORTIE
                    : SensPaiement.ENTREE;

            Paiement paiement = Paiement.builder()
                    .datePaiement(LocalDateTime.now())
                    .montantPaiement(montant)
                    .modePaiement(dto.getModePaiement())
                    .referencePaiement(dto.getReference())
                    .sens(sens)
                    .userCreate(currentAgentId)
                    .build();
            paiementRepository.save(paiement);
            decompte.setPaiement(paiement);
        }

        decompte.setStatut(StatutDecompteSortie.REGLE);
        decompte.setUserUpdate(currentAgentId);
        decompteSortieRepository.save(decompte);

        return buildSuccessResponse(HttpStatus.OK, "Règlement de sortie finalisé", "DECOMPTE_SORTIE_REGLE", toDtoDecompte(decompte));
    }


    public ResponseEntity<?> getDecompteSortie(Integer idContratLocation) {
        DecompteSortie decompte = decompteSortieRepository.findByContratLocation_IdContratLocation(idContratLocation)
                .orElseThrow(() -> new ResourceNotFoundException("décompte de sortie pour le contrat", idContratLocation));
        return buildSuccessResponse(HttpStatus.OK, "Décompte de sortie", "DECOMPTE_SORTIE_DETAIL", toDtoDecompte(decompte));
    }



    private ContratLocation findOrThrow(Integer id) {
        return locationRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("location", id));
    }

    private ContratLocationResponseDTO toDto(ContratLocation l) {
        return ContratLocationResponseDTO.builder()
                .idContratLocation(l.getIdContratLocation())
                .idLocataire(l.getLocataire().getIdUser())
                .nomLocataire(l.getLocataire().getNom() + " " + l.getLocataire().getPrenom())
                .idMaison(l.getMaison().getIdMaison())
                .nomCommunMaison(l.getMaison().getNomCommunMaison())
                .dateEntree(l.getDateEntree())
                .dateSortie(l.getDateSortie())
                .montantLoyer(l.getMontantLoyer())
                .statut(l.getStatut())
                .build();
    }

    private DecompteSortieResponseDTO toDtoDecompte(DecompteSortie d) {
        return DecompteSortieResponseDTO.builder()
                .idDecompte(d.getIdDecompte())
                .idContratLocation(d.getContratLocation().getIdContratLocation())
                .dateSortie(d.getDateSortie())
                .montantAvanceReference(d.getMontantAvanceReference())
                .montantCautionReference(d.getMontantCautionReference())
                .montantArrieres(d.getMontantArrieres())
                .coutReparation(d.getCoutReparation())
                .montantDeduitAvance(d.getMontantDeduitAvance())
                .montantDeduitCaution(d.getMontantDeduitCaution())
                .montantManquant(d.getMontantManquant())
                .montantARembourser(d.getMontantARembourser())
                .statut(d.getStatut())
                .build();
    }
}