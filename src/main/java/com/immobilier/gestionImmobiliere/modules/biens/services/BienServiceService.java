package com.immobilier.gestionImmobiliere.modules.biens.services;

import com.immobilier.gestionImmobiliere.donnees.biens.model.BienService;
import com.immobilier.gestionImmobiliere.donnees.biens.model.CategorieBienService;
import com.immobilier.gestionImmobiliere.donnees.biens.repository.BienServiceRepository;
import com.immobilier.gestionImmobiliere.donnees.biens.repository.CategorieBienServiceRepository;
import com.immobilier.gestionImmobiliere.donnees.localisation.model.Secteur;
import com.immobilier.gestionImmobiliere.donnees.localisation.repository.SecteurRepository;
import com.immobilier.gestionImmobiliere.donnees.user.model.User;
import com.immobilier.gestionImmobiliere.donnees.user.repository.UserRepository;
import com.immobilier.gestionImmobiliere.exceptions.ResourceNotFoundException;
import com.immobilier.gestionImmobiliere.exceptions.SecteurNotFoundException;
import com.immobilier.gestionImmobiliere.modules.biens.dto.requests.CreateBienServiceDTO;
import com.immobilier.gestionImmobiliere.modules.biens.dto.requests.UpdateBienServiceDTO;
import com.immobilier.gestionImmobiliere.modules.biens.dto.requests.UpdateDisponibiliteBienServiceDTO;
import com.immobilier.gestionImmobiliere.modules.biens.dto.responses.BienServiceResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static com.immobilier.gestionImmobiliere.utils.BuildSuccessResponse.buildSuccessResponse;

@Service
public class BienServiceService {

    private final BienServiceRepository bienServiceRepository;
    private final SecteurRepository secteurRepository;
    private final CategorieBienServiceRepository categorieRepository;
    private final UserRepository userRepository;

    public BienServiceService(BienServiceRepository bienServiceRepository, SecteurRepository secteurRepository,
                              CategorieBienServiceRepository categorieRepository, UserRepository userRepository) {
        this.bienServiceRepository = bienServiceRepository;
        this.secteurRepository = secteurRepository;
        this.categorieRepository = categorieRepository;
        this.userRepository = userRepository;
    }

    public ResponseEntity<?> getAll(Integer idSecteur, Integer idCategorie, Pageable pageable) {
        Page<BienServiceResponseDTO> result;
        if (idSecteur != null && idCategorie != null) {
            result = bienServiceRepository.findBySecteur_IdSecteurAndCategorie_IdCategorie(idSecteur, idCategorie, pageable).map(this::toDto);
        } else if (idSecteur != null) {
            result = bienServiceRepository.findBySecteur_IdSecteur(idSecteur, pageable).map(this::toDto);
        } else if (idCategorie != null) {
            result = bienServiceRepository.findByCategorie_IdCategorie(idCategorie, pageable).map(this::toDto);
        } else {
            result = bienServiceRepository.findAll(pageable).map(this::toDto);
        }
        return buildSuccessResponse(HttpStatus.OK, "Liste des biens/services", "BIEN_SERVICE_LIST", result);
    }

    public ResponseEntity<?> getById(Integer id) {
        return buildSuccessResponse(HttpStatus.OK, "Détail bien/service", "BIEN_SERVICE_DETAIL", toDto(findOrThrow(id)));
    }

    @Transactional
    public ResponseEntity<?> create(CreateBienServiceDTO dto, Integer currentUserId) {
        Secteur secteur = secteurRepository.findById(dto.getIdSecteur())
                .orElseThrow(() -> new SecteurNotFoundException(dto.getIdSecteur()));
        CategorieBienService categorie = categorieRepository.findById(dto.getIdCategorie())
                .orElseThrow(() -> new ResourceNotFoundException("categorie", dto.getIdCategorie()));
        User gestionnaire = userRepository.findById(dto.getIdGestionnaire())
                .orElseThrow(() -> new ResourceNotFoundException("user", dto.getIdGestionnaire()));

        BienService bien = BienService.builder()
                .secteur(secteur)
                .categorie(categorie)
                .gestionnaire(gestionnaire)
                .libelle(dto.getLibelle())
                .description(dto.getDescription())
                .prixJournalier(dto.getPrixJournalier())
                .prixMensuel(dto.getPrixMensuel())
                .userCreate(currentUserId)
                .build();
        bienServiceRepository.save(bien);
        return buildSuccessResponse(HttpStatus.CREATED, "Bien/service créé", "BIEN_SERVICE_CREATED", toDto(bien));
    }

    @Transactional
    public ResponseEntity<?> update(Integer id, UpdateBienServiceDTO dto, Integer currentUserId) {
        BienService bien = findOrThrow(id);

        if (dto.getIdSecteur() != null) {
            Secteur secteur = secteurRepository.findById(dto.getIdSecteur())
                    .orElseThrow(() -> new SecteurNotFoundException(dto.getIdSecteur()));
            bien.setSecteur(secteur);
        }
        if (dto.getIdCategorie() != null) {
            CategorieBienService categorie = categorieRepository.findById(dto.getIdCategorie())
                    .orElseThrow(() -> new ResourceNotFoundException("categorie", dto.getIdCategorie()));
            bien.setCategorie(categorie);
        }
        if (dto.getLibelle() != null) bien.setLibelle(dto.getLibelle());
        if (dto.getDescription() != null) bien.setDescription(dto.getDescription());
        if (dto.getPrixJournalier() != null) bien.setPrixJournalier(dto.getPrixJournalier());
        if (dto.getPrixMensuel() != null) bien.setPrixMensuel(dto.getPrixMensuel());
        bien.setUserUpdate(currentUserId);
        bien.setUpdatedAt(LocalDateTime.now());

        bienServiceRepository.save(bien);
        return buildSuccessResponse(HttpStatus.OK, "Bien/service mis à jour", "BIEN_SERVICE_UPDATED", toDto(bien));
    }

    @Transactional
    public ResponseEntity<?> updateDisponibilite(Integer id, UpdateDisponibiliteBienServiceDTO dto) {
        BienService bien = findOrThrow(id);
        bien.setDisponibilite(dto.getDisponibilite());
        bien.setUpdatedAt(LocalDateTime.now());
        bienServiceRepository.save(bien);
        return buildSuccessResponse(HttpStatus.OK, "Disponibilité mise à jour", "BIEN_SERVICE_DISPO_UPDATED", toDto(bien));
    }

    @Transactional
    public ResponseEntity<?> delete(Integer id) {
        BienService bien = findOrThrow(id);
        bien.setIsDeleted(true);
        bienServiceRepository.save(bien);
        return buildSuccessResponse(HttpStatus.OK, "Bien/service supprimé", "BIEN_SERVICE_DELETED", null);
    }

    private BienService findOrThrow(Integer id) {
        return bienServiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("bienService", id));
    }

    private BienServiceResponseDTO toDto(BienService b) {
        return BienServiceResponseDTO.builder()
                .idBienService(b.getIdBienService())
                .libelle(b.getLibelle())
                .description(b.getDescription())
                .prixJournalier(b.getPrixJournalier())
                .prixMensuel(b.getPrixMensuel())
                .disponibilite(b.getDisponibilite())
                .idSecteur(b.getSecteur().getIdSecteur())
                .nomSecteur(b.getSecteur().getNomSecteur())
                .idCategorie(b.getCategorie().getIdCategorie())
                .libelleCategorie(b.getCategorie().getLibelle())
                .idGestionnaire(b.getGestionnaire().getIdUser())
                .nomGestionnaire(b.getGestionnaire().getNom() + " " + b.getGestionnaire().getPrenom())
                .build();
    }
}