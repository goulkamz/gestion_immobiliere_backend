package com.immobilier.gestionImmobiliere.modules.biens.services;

import com.immobilier.gestionImmobiliere.donnees.biens.model.CategorieBienService;
import com.immobilier.gestionImmobiliere.donnees.biens.repository.BienServiceRepository;
import com.immobilier.gestionImmobiliere.donnees.biens.repository.CategorieBienServiceRepository;
import com.immobilier.gestionImmobiliere.exceptions.CategorieUtiliseeException;
import com.immobilier.gestionImmobiliere.exceptions.ResourceNotFoundException;
import com.immobilier.gestionImmobiliere.modules.biens.dto.requests.CreateCategorieDTO;
import com.immobilier.gestionImmobiliere.modules.biens.dto.requests.UpdateCategorieDTO;
import com.immobilier.gestionImmobiliere.modules.biens.dto.responses.CategorieResponseDTO;
import com.immobilier.gestionImmobiliere.utils.RechercheSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.immobilier.gestionImmobiliere.utils.BuildSuccessResponse.buildSuccessResponse;

@Service
public class CategorieBienServiceService {

    private final CategorieBienServiceRepository categorieRepository;
    private final BienServiceRepository bienServiceRepository;

    public CategorieBienServiceService(CategorieBienServiceRepository categorieRepository,
                                       BienServiceRepository bienServiceRepository) {
        this.categorieRepository = categorieRepository;
        this.bienServiceRepository = bienServiceRepository;
    }

    public ResponseEntity<?> getAll(String recherche, Pageable pageable) {
        // Recherche sur le libelle appliquee cote base avant la pagination ; optionnelle
        Page<CategorieBienService> page = categorieRepository
                .findAll(RechercheSpecification.<CategorieBienService>statutEtTexte(null, recherche, "libelle"), pageable);
        // Une seule requete groupee pour toute la page, pas un count par ligne
        Map<Integer, Long> nb = compterBiensServices(page.getContent());
        Page<CategorieResponseDTO> result = page.map(c -> toDto(c, nb.getOrDefault(c.getIdCategorie(), 0L)));
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
        long nb = bienServiceRepository.countByCategorie_IdCategorieAndIsDeletedFalse(id);
        if (nb > 0) throw new CategorieUtiliseeException(categorie.getLibelle(), nb);
        categorie.setIsDeleted(true);
        categorieRepository.save(categorie);
        return buildSuccessResponse(HttpStatus.OK, "Catégorie supprimée", "CATEGORIE_DELETED", null);
    }

    private CategorieBienService findOrThrow(Integer id) {
        return categorieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("categorie", id));
    }

    private Map<Integer, Long> compterBiensServices(List<CategorieBienService> categories) {
        Map<Integer, Long> nb = new HashMap<>();
        if (categories.isEmpty()) return nb;
        List<Integer> ids = categories.stream().map(CategorieBienService::getIdCategorie).toList();
        for (Object[] ligne : bienServiceRepository.countParCategorie(ids)) {
            nb.put((Integer) ligne[0], (Long) ligne[1]);
        }
        return nb;
    }

    // Variante pour une seule categorie (detail, creation, mise a jour)
    private CategorieResponseDTO toDto(CategorieBienService c) {
        return toDto(c, c.getIdCategorie() == null ? 0L
                : bienServiceRepository.countByCategorie_IdCategorieAndIsDeletedFalse(c.getIdCategorie()));
    }

    private CategorieResponseDTO toDto(CategorieBienService c, long nbBiensServices) {
        return CategorieResponseDTO.builder()
                .nbBiensServices(nbBiensServices)
                .idCategorie(c.getIdCategorie())
                .libelle(c.getLibelle())
                .description(c.getDescription())
                .build();
    }
}