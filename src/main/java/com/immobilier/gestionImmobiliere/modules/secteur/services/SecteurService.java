package com.immobilier.gestionImmobiliere.modules.secteur.services;

import com.immobilier.gestionImmobiliere.donnees.secteur.model.Secteur;
import com.immobilier.gestionImmobiliere.donnees.ville.model.Ville;
import com.immobilier.gestionImmobiliere.donnees.secteur.repository.SecteurRepository;
import com.immobilier.gestionImmobiliere.donnees.ville.repository.VilleRepository;
import com.immobilier.gestionImmobiliere.exceptions.CodeAlreadyExistsException;
import com.immobilier.gestionImmobiliere.exceptions.SecteurNotFoundException;
import com.immobilier.gestionImmobiliere.exceptions.VilleNotFoundException;
import com.immobilier.gestionImmobiliere.modules.secteur.dto.requests.CreateSecteurDTO;
import com.immobilier.gestionImmobiliere.modules.secteur.dto.requests.UpdateSecteurDTO;
import com.immobilier.gestionImmobiliere.modules.secteur.dto.responses.SecteurResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.immobilier.gestionImmobiliere.utils.BuildSuccessResponse.buildSuccessResponse;

@Service
public class SecteurService {

    private final SecteurRepository secteurRepository;
    private final VilleRepository villeRepository;

    public SecteurService(SecteurRepository secteurRepository, VilleRepository villeRepository) {
        this.secteurRepository = secteurRepository;
        this.villeRepository = villeRepository;
    }

    public ResponseEntity<?> getAll(Long idVille, Pageable pageable) {
        Page<SecteurResponseDTO> result = (idVille != null
                ? secteurRepository.findByVille_IdVille(idVille, pageable)
                : secteurRepository.findAll(pageable))
                .map(this::toDto);
        return buildSuccessResponse(HttpStatus.OK, "Liste des secteurs", "SECTEUR_LIST", result);
    }

    public ResponseEntity<?> getById(Long id) {
        return buildSuccessResponse(HttpStatus.OK, "Secteur trouvé", "SECTEUR_FOUND", toDto(findOrThrow(id)));
    }

    @Transactional
    public ResponseEntity<?> create(CreateSecteurDTO dto) {
        Ville ville = villeRepository.findById(dto.getIdVille())
                .orElseThrow(() -> new VilleNotFoundException(dto.getIdVille()));

        if (secteurRepository.existsByCodeSecteurIgnoreCaseAndVille_IdVille(dto.getCodeSecteur(), dto.getIdVille())) {
            throw new CodeAlreadyExistsException("secteur", dto.getCodeSecteur());
        }

        Secteur secteur = Secteur.builder()
                .ville(ville)
                .codeSecteur(dto.getCodeSecteur())
                .nomSecteur(dto.getNomSecteur())
                .build();
        secteurRepository.save(secteur);
        return buildSuccessResponse(HttpStatus.CREATED, "Secteur créé avec succès", "SECTEUR_CREATED", toDto(secteur));
    }

    @Transactional
    public ResponseEntity<?> update(Long id, UpdateSecteurDTO dto) {
        Secteur secteur = findOrThrow(id);

        if (dto.getIdVille() != null) {
            Ville ville = villeRepository.findById(dto.getIdVille())
                    .orElseThrow(() -> new VilleNotFoundException(dto.getIdVille()));
            secteur.setVille(ville);
        }
        if (dto.getCodeSecteur() != null) secteur.setCodeSecteur(dto.getCodeSecteur());
        if (dto.getNomSecteur() != null) secteur.setNomSecteur(dto.getNomSecteur());

        secteurRepository.save(secteur);
        return buildSuccessResponse(HttpStatus.OK, "Secteur mis à jour", "SECTEUR_UPDATED", toDto(secteur));
    }

    @Transactional
    public ResponseEntity<?> delete(Long id) {
        secteurRepository.delete(findOrThrow(id));
        return buildSuccessResponse(HttpStatus.OK, "Secteur supprimé", "SECTEUR_DELETED", null);
    }

    private Secteur findOrThrow(Long id) {
        return secteurRepository.findById(id).orElseThrow(() -> new SecteurNotFoundException(id));
    }

    private SecteurResponseDTO toDto(Secteur s) {
        return SecteurResponseDTO.builder()
                .idSecteur(s.getIdSecteur())
                .codeSecteur(s.getCodeSecteur())
                .nomSecteur(s.getNomSecteur())
                .idVille(s.getVille().getIdVille())
                .nomVille(s.getVille().getNomVille())
                .build();
    }
}