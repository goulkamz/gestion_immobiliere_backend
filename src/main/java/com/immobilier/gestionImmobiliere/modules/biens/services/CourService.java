package com.immobilier.gestionImmobiliere.modules.biens.services;

import com.immobilier.gestionImmobiliere.donnees.biens.model.Cour;
import com.immobilier.gestionImmobiliere.donnees.biens.model.Maison;
import com.immobilier.gestionImmobiliere.donnees.biens.model.StatutMaison;
import com.immobilier.gestionImmobiliere.donnees.biens.repository.CourRepository;
import com.immobilier.gestionImmobiliere.donnees.biens.repository.MaisonRepository;
import com.immobilier.gestionImmobiliere.donnees.localisation.model.Secteur;
import com.immobilier.gestionImmobiliere.donnees.localisation.repository.SecteurRepository;
import com.immobilier.gestionImmobiliere.donnees.user.model.User;
import com.immobilier.gestionImmobiliere.donnees.user.repository.UserRepository;

import com.immobilier.gestionImmobiliere.exceptions.ResourceNotFoundException;
import com.immobilier.gestionImmobiliere.exceptions.SecteurNotFoundException;
import com.immobilier.gestionImmobiliere.modules.biens.dto.requests.CreateCourDTO;
import com.immobilier.gestionImmobiliere.modules.biens.dto.requests.UpdateCourDTO;
import com.immobilier.gestionImmobiliere.modules.biens.dto.responses.CourResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static com.immobilier.gestionImmobiliere.utils.BuildSuccessResponse.buildSuccessResponse;

@Service
public class CourService {

    private final CourRepository courRepository;
    private final SecteurRepository secteurRepository;
    private final UserRepository userRepository;

    public CourService(CourRepository courRepository, SecteurRepository secteurRepository, UserRepository userRepository) {
        this.courRepository = courRepository;
        this.secteurRepository = secteurRepository;
        this.userRepository = userRepository;
    }

    public ResponseEntity<?> getAll(Integer idSecteur, Pageable pageable) {
        Page<CourResponseDTO> result = (idSecteur != null
                ? courRepository.findBySecteur_IdSecteur(idSecteur, pageable)
                : courRepository.findAll(pageable)).map(this::toDto);
        return buildSuccessResponse(HttpStatus.OK, "Liste des cours", "COUR_LIST", result);
    }

    public ResponseEntity<?> getById(Integer id) {
        return buildSuccessResponse(HttpStatus.OK, "Cour trouvée", "COUR_FOUND", toDto(findOrThrow(id)));
    }

    @Transactional
    public ResponseEntity<?> create(CreateCourDTO dto, Integer currentUserId) {
        Secteur secteur = secteurRepository.findById(dto.getIdSecteur())
                .orElseThrow(() -> new SecteurNotFoundException(dto.getIdSecteur()));
        User proprietaire = userRepository.findById(dto.getIdProprietaire())
                .orElseThrow(() -> new ResourceNotFoundException("user",dto.getIdProprietaire()));

        Cour cour = Cour.builder()
                .secteur(secteur)
                .proprietaire(proprietaire)
                .referenceCours(dto.getReferenceCours())
                .lotCours(dto.getLotCours())
                .numeroPorte(dto.getNumeroPorte())
                .userCreate(currentUserId)
                .dateCreate(LocalDateTime.now())
                .build();
        courRepository.save(cour);
        return buildSuccessResponse(HttpStatus.CREATED, "Cour créée avec succès", "COUR_CREATED", toDto(cour));
    }

    @Transactional
    public ResponseEntity<?> update(Integer id, UpdateCourDTO dto, Integer currentUserId) {
        Cour cour = findOrThrow(id);

        if (dto.getIdSecteur() != null) {
            Secteur secteur = secteurRepository.findById(dto.getIdSecteur())
                    .orElseThrow(() -> new ResourceNotFoundException("secteur",dto.getIdSecteur()));
            cour.setSecteur(secteur);
        }
        if (dto.getReferenceCours() != null) cour.setReferenceCours(dto.getReferenceCours());
        if (dto.getLotCours() != null) cour.setLotCours(dto.getLotCours());
        if (dto.getNumeroPorte() != null) cour.setNumeroPorte(dto.getNumeroPorte());
        cour.setUserUpdate(currentUserId);
        cour.setDateUpdate(LocalDateTime.now());

        courRepository.save(cour);
        return buildSuccessResponse(HttpStatus.OK, "Cour mise à jour", "COUR_UPDATED", toDto(cour));
    }

    @Transactional
    public ResponseEntity<?> create(CreateCourDTO dto) {
        Secteur secteur = secteurRepository.findById(dto.getIdSecteur())
                .orElseThrow(() -> new SecteurNotFoundException(dto.getIdSecteur()));
        User proprietaire = userRepository.findById(dto.getIdProprietaire())
                .orElseThrow(() -> new ResourceNotFoundException("user",dto.getIdProprietaire()));

        Cour cour = Cour.builder()
                .secteur(secteur)
                .proprietaire(proprietaire)
                .referenceCours(dto.getReferenceCours())
                .lotCours(dto.getLotCours())
                .numeroPorte(dto.getNumeroPorte())
                .dateCreate(LocalDateTime.now())
                .build();
        courRepository.save(cour);
        return buildSuccessResponse(HttpStatus.CREATED, "Cour créée avec succès", "COUR_CREATED", toDto(cour));
    }

    @Transactional
    public ResponseEntity<?> update(Integer id, UpdateCourDTO dto) {
        Cour cour = findOrThrow(id);

        if (dto.getIdSecteur() != null) {
            Secteur secteur = secteurRepository.findById(dto.getIdSecteur())
                    .orElseThrow(() -> new ResourceNotFoundException("secteur",dto.getIdSecteur()));
            cour.setSecteur(secteur);
        }
        if (dto.getReferenceCours() != null) cour.setReferenceCours(dto.getReferenceCours());
        if (dto.getLotCours() != null) cour.setLotCours(dto.getLotCours());
        if (dto.getNumeroPorte() != null) cour.setNumeroPorte(dto.getNumeroPorte());
        cour.setDateUpdate(LocalDateTime.now());

        courRepository.save(cour);
        return buildSuccessResponse(HttpStatus.OK, "Cour mise à jour", "COUR_UPDATED", toDto(cour));
    }

    @Transactional
    public ResponseEntity<?> delete(Integer id) {
        courRepository.delete(findOrThrow(id));
        return buildSuccessResponse(HttpStatus.OK, "Cour supprimée", "COUR_DELETED", null);
    }

    private Cour findOrThrow(Integer id) {
        return courRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("cour",id));
    }

    private CourResponseDTO toDto(Cour c) {
        return CourResponseDTO.builder()
                .idCour(c.getIdCour())
                .referenceCours(c.getReferenceCours())
                .lotCours(c.getLotCours())
                .numeroPorte(c.getNumeroPorte())
                .idSecteur(c.getSecteur().getIdSecteur())
                .nomSecteur(c.getSecteur().getNomSecteur())
                .idProprietaire(c.getProprietaire().getIdUser())
                .nomProprietaire(c.getProprietaire().getNom() + " " + c.getProprietaire().getPrenom())
                .build();
    }
}