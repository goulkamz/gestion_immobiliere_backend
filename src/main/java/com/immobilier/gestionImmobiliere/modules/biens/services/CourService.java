package com.immobilier.gestionImmobiliere.modules.biens.services;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import java.util.ArrayList;
import java.util.List;

import com.immobilier.gestionImmobiliere.donnees.biens.model.Cour;
import com.immobilier.gestionImmobiliere.donnees.biens.model.Maison;
import com.immobilier.gestionImmobiliere.donnees.contrats.model.ContratMandat;
import com.immobilier.gestionImmobiliere.donnees.contrats.model.StatutMandat;
import jakarta.persistence.criteria.Subquery;
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
import com.immobilier.gestionImmobiliere.modules.user.jwtService.UserDetailsImpl;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
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


    // Recherche insensible à la casse sur la référence, le lot, le secteur ou le nom du bailleur
    private Specification<Cour> filtrer(Integer idSecteur, String recherche, Integer idProprietaire, boolean sansMandatActif) {
        return (root, query, cb) -> {
            List<Predicate> predicats = new ArrayList<>();
            if (idSecteur != null) {
                predicats.add(cb.equal(root.get("secteur").get("idSecteur"), idSecteur));
            }
            if (idProprietaire != null) {
                predicats.add(cb.equal(root.get("proprietaire").get("idUser"), idProprietaire));
            }
            if (sansMandatActif) {
                // Cours n'ayant aucun mandat ACTIF (une cour ne peut en avoir qu'un à la fois)
                Subquery<Integer> actifs = query.subquery(Integer.class);
                var mandat = actifs.from(ContratMandat.class);
                actifs.select(mandat.get("cour").get("idCour"))
                        .where(cb.equal(mandat.get("statut"), StatutMandat.ACTIF));
                predicats.add(cb.not(root.get("idCour").in(actifs)));
            }
            if (recherche != null && !recherche.isBlank()) {
                String motif = "%" + recherche.trim().toLowerCase() + "%";
                Join<Object, Object> secteur = root.join("secteur");
                Join<Object, Object> proprietaire = root.join("proprietaire");
                predicats.add(cb.or(
                        cb.like(cb.lower(root.<String>get("referenceCour")), motif),
                        cb.like(cb.lower(root.<String>get("lotCour")), motif),
                        cb.like(cb.lower(secteur.<String>get("nomSecteur")), motif),
                        cb.like(cb.lower(cb.concat(cb.concat(proprietaire.<String>get("nom"), " "), proprietaire.<String>get("prenom"))), motif)));
            }
            return cb.and(predicats.toArray(new Predicate[0]));
        };
    }

    public ResponseEntity<?> getAll(Integer idSecteur, String recherche, Boolean sansMandatActif, Pageable pageable, UserDetailsImpl currentUser) {

        boolean isBailleur = currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_BAILLEUR"));

        // Un bailleur ne voit QUE ses propres cours ; agent / admin : vue globale.
        // Secteur et recherche sont appliqués côté base avant la pagination.
        Page<CourResponseDTO> result = courRepository
                .findAll(filtrer(idSecteur, recherche, isBailleur ? currentUser.getIdUser() : null, Boolean.TRUE.equals(sansMandatActif)), pageable)
                .map(this::toDto);

        return buildSuccessResponse(HttpStatus.OK, "Liste des cours", "COUR_LIST", result);
    }


    public ResponseEntity<?> getById(Integer id, UserDetailsImpl currentUser) {
        Cour cour = courRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Cour introuvable"));

        boolean isBailleur = currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_BAILLEUR"));

        if (isBailleur && !cour.getProprietaire().getIdUser().equals(currentUser.getIdUser())) {
            throw new AccessDeniedException("Vous n'avez pas accès à ce bien");
        }

        return buildSuccessResponse(HttpStatus.OK, "Détail du cour", "COUR_DETAIL", toDto(cour));
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
                .referenceCour(dto.getReferenceCour())
                .lotCour(dto.getLotCour())
                .numeroPorte(dto.getNumeroPorte())
                .userCreate(currentUserId)
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
        if (dto.getReferenceCour() != null) cour.setReferenceCour(dto.getReferenceCour());
        if (dto.getLotCour() != null) cour.setLotCour(dto.getLotCour());
        if (dto.getNumeroPorte() != null) cour.setNumeroPorte(dto.getNumeroPorte());
        cour.setUserUpdate(currentUserId);
        cour.setUpdatedAt(LocalDateTime.now());

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
                .referenceCour(c.getReferenceCour())
                .lotCour(c.getLotCour())
                .numeroPorte(c.getNumeroPorte())
                .idSecteur(c.getSecteur().getIdSecteur())
                .nomSecteur(c.getSecteur().getNomSecteur())
                .idProprietaire(c.getProprietaire().getIdUser())
                .nomProprietaire(c.getProprietaire().getNom() + " " + c.getProprietaire().getPrenom())
                .build();
    }
}