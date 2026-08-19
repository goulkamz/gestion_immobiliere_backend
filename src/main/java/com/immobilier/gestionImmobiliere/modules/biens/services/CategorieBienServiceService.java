package com.immobilier.gestionImmobiliere.modules.biens.services;

import com.immobilier.gestionImmobiliere.donnees.biens.model.CategorieBienService;
import com.immobilier.gestionImmobiliere.donnees.biens.repository.CategorieBienServiceRepository;
import com.immobilier.gestionImmobiliere.exceptions.ResourceNotFoundException;
import com.immobilier.gestionImmobiliere.modules.biens.dto.requests.CreateCategorieDTO;
import com.immobilier.gestionImmobiliere.modules.biens.dto.requests.UpdateCategorieDTO;
import com.immobilier.gestionImmobiliere.modules.biens.dto.responses.CategorieResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.immobilier.gestionImmobiliere.utils.BuildSuccessResponse.buildSuccessResponse;

@Service
public class CategorieBienServiceService {

    private final CategorieBienServiceRepository categorieRepository;

    public CategorieBienServiceService(CategorieBienServiceRepository categorieRepository) {
        this.categorieRepository = categorieRepository;
    }

    public ResponseEntity<?> getAll(Pageable pageable) {
        Page<CategorieResponseDTO> result = categorieRepository.findAll(pageable).map(this::toDto);
        return buildSuccessResponse(HttpStatus.OK, "Liste des catégories", "CATEGORIE_LIST", result);
    }

    public ResponseEntity<?> getById(Integer id) {
        return buildSuccessResponse(HttpStatus.OK, "Détail catégorie", "CATEGORIE_DETAIL", toDto(findOrThrow(id)));
    }

    @Transactional
    public ResponseEntity<?> create(CreateCategorieDTO dto) {
        CategorieBienService categorie = CategorieBienService.builder()
                .libelle(dto.getLibelle())
                .description(dto.getDescription())
                .build();
        categorieRepository.save(categorie);
        return buildSuccessResponse(HttpStatus.CREATED, "Catégorie créée", "CATEGORIE_CREATED", toDto(categorie));
    }

    @Transactional
    public ResponseEntity<?> update(Integer id, UpdateCategorieDTO dto) {
        CategorieBienService categorie = findOrThrow(id);
        if (dto.getLibelle() != null) categorie.setLibelle(dto.getLibelle());
        if (dto.getDescription() != null) categorie.setDescription(dto.getDescription());
        categorieRepository.save(categorie);
        return buildSuccessResponse(HttpStatus.OK, "Catégorie mise à jour", "CATEGORIE_UPDATED", toDto(categorie));
    }

    @Transactional
    public ResponseEntity<?> delete(Integer id) {
        CategorieBienService categorie = findOrThrow(id);
        categorie.setIsDeleted(true);
        categorieRepository.save(categorie);
        return buildSuccessResponse(HttpStatus.OK, "Catégorie supprimée", "CATEGORIE_DELETED", null);
    }

    private CategorieBienService findOrThrow(Integer id) {
        return categorieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("categorie", id));
    }

    private CategorieResponseDTO toDto(CategorieBienService c) {
        return CategorieResponseDTO.builder()
                .idCategorie(c.getIdCategorie())
                .libelle(c.getLibelle())
                .description(c.getDescription())
                .build();
    }
}