package com.immobilier.gestionImmobiliere.modules.contrats.services;

import com.immobilier.gestionImmobiliere.donnees.biens.model.Maison;
import com.immobilier.gestionImmobiliere.donnees.biens.model.StatutMaison;
import com.immobilier.gestionImmobiliere.donnees.biens.repository.MaisonRepository;
import com.immobilier.gestionImmobiliere.donnees.contrats.model.ContratLocation;
import com.immobilier.gestionImmobiliere.donnees.contrats.model.StatutLocation;
import com.immobilier.gestionImmobiliere.donnees.contrats.repository.ContratLocationRepository;
import com.immobilier.gestionImmobiliere.donnees.user.model.User;
import com.immobilier.gestionImmobiliere.donnees.user.repository.UserRepository;
import com.immobilier.gestionImmobiliere.exceptions.InvalidStatutTransitionException;
import com.immobilier.gestionImmobiliere.exceptions.MaisonIndisponibleException;
import com.immobilier.gestionImmobiliere.exceptions.ResourceNotFoundException;
import com.immobilier.gestionImmobiliere.modules.contrats.dto.requests.CreateContratLocationDTO;
import com.immobilier.gestionImmobiliere.modules.contrats.dto.requests.TerminerLocationDTO;
import com.immobilier.gestionImmobiliere.modules.contrats.dto.responses.ContratLocationResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static com.immobilier.gestionImmobiliere.utils.BuildSuccessResponse.buildSuccessResponse;

@Service
public class ContratLocationService {

    private final ContratLocationRepository locationRepository;
    private final MaisonRepository maisonRepository;
    private final UserRepository userRepository;

    public ContratLocationService(ContratLocationRepository locationRepository, MaisonRepository maisonRepository, UserRepository userRepository) {
        this.locationRepository = locationRepository;
        this.maisonRepository = maisonRepository;
        this.userRepository = userRepository;
    }

    /**
     * Version "brute" protégée par @PostAuthorize — DOIT être appelée
     * depuis un autre bean (le controller) pour que le proxy Spring intercepte.
     */
    @PostAuthorize(
            "hasAnyRole('ADMIN','AGENT') " +
                    "or returnObject.idLocataire == authentication.principal.id " +
                    "or @contratLocationSecurity.isProprietaire(returnObject.idMaison, authentication.principal.id)"
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

    public ResponseEntity<?> getAll(Integer idMaison, Integer idLocataire, Pageable pageable) {
        Page<ContratLocation> page = idMaison != null
                ? locationRepository.findByMaison_IdMaison(idMaison, pageable)
                : (idLocataire != null ? locationRepository.findByLocataire_IdUser(idLocataire, pageable) : locationRepository.findAll(pageable));
        return buildSuccessResponse(HttpStatus.OK, "Liste des contrats de location", "LOCATION_LIST", page.map(this::toDto));
    }

    public ResponseEntity<?> getById(Integer id) {
        return buildSuccessResponse(HttpStatus.OK, "Contrat de location trouvé", "LOCATION_FOUND", toDto(findOrThrow(id)));
    }

    @Transactional
    public ResponseEntity<?> create(CreateContratLocationDTO dto, Integer currentUserId) {
        Maison maison = maisonRepository.findById(dto.getIdMaison())
                .orElseThrow(() -> new ResourceNotFoundException("maison", dto.getIdMaison()));
        User locataire = userRepository.findById(dto.getIdLocataire())
                .orElseThrow(() -> new ResourceNotFoundException("locataire", dto.getIdLocataire()));

        // RG F14 — vérification de la disponibilité de la maison
        if (maison.getStatut() != StatutMaison.DISPONIBLE) {
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
                .dateCreate(LocalDate.now())
                .build();
        locationRepository.save(location);

        // Bascule la maison en LOUEE (transition directe, contrat signé sans passage par réservation)
        maison.setStatut(StatutMaison.LOUEE);
        maisonRepository.save(maison);

        // TODO (module Paiements) : génération automatique des échéances de loyer
        return buildSuccessResponse(HttpStatus.CREATED, "Contrat de location créé, maison marquée louée", "LOCATION_CREATED", toDto(location));
    }

    @Transactional
    public ResponseEntity<?> terminer(Integer id, TerminerLocationDTO dto) {
        ContratLocation location = findOrThrow(id);

        if (location.getStatut() != StatutLocation.ACTIF) {
            throw new InvalidStatutTransitionException(location.getStatut().name(), StatutLocation.TERMINE.name());
        }

        location.setStatut(StatutLocation.TERMINE);
        location.setEtatDesLieuxSortie(dto.getEtatDesLieuxSortie());
        location.setDateSortie(dto.getDateSortie() != null ? dto.getDateSortie() : LocalDate.now());
        locationRepository.save(location);

        Maison maison = location.getMaison();
        maison.setStatut(StatutMaison.DISPONIBLE);
        maisonRepository.save(maison);

        return buildSuccessResponse(HttpStatus.OK, "Contrat terminé, maison redevenue disponible", "LOCATION_TERMINEE", toDto(location));
    }

    @Transactional
    public ResponseEntity<?> resilier(Integer id) {
        ContratLocation location = findOrThrow(id);

        if (location.getStatut() != StatutLocation.ACTIF) {
            throw new InvalidStatutTransitionException(location.getStatut().name(), StatutLocation.RESILIE.name());
        }

        location.setStatut(StatutLocation.RESILIE);
        locationRepository.save(location);

        Maison maison = location.getMaison();
        maison.setStatut(StatutMaison.DISPONIBLE);
        maisonRepository.save(maison);

        return buildSuccessResponse(HttpStatus.OK, "Contrat résilié, maison redevenue disponible", "LOCATION_RESILIEE", toDto(location));
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
}