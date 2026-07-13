package com.immobilier.gestionImmobiliere.modules.localisation.services;

import com.immobilier.gestionImmobiliere.donnees.localisation.model.Secteur;
import com.immobilier.gestionImmobiliere.donnees.localisation.model.Ville;
import com.immobilier.gestionImmobiliere.donnees.localisation.repository.SecteurRepository;
import com.immobilier.gestionImmobiliere.donnees.localisation.repository.VilleRepository;
import com.immobilier.gestionImmobiliere.exceptions.CodeAlreadyExistsException;
import com.immobilier.gestionImmobiliere.exceptions.ResourceNotFoundException;
import com.immobilier.gestionImmobiliere.exceptions.SecteurNotFoundException;
import com.immobilier.gestionImmobiliere.exceptions.VilleNotFoundException;
import com.immobilier.gestionImmobiliere.modules.localisation.dto.requests.CreateSecteurDTO;
import com.immobilier.gestionImmobiliere.modules.localisation.dto.requests.UpdateSecteurDTO;
import com.immobilier.gestionImmobiliere.modules.localisation.dto.responses.SecteurResponseDTO;
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

    public ResponseEntity<?> getAll(Integer idVille, Pageable pageable) {
        Page<SecteurResponseDTO> result = (idVille != null
                ? secteurRepository.findByVille_IdVille(idVille, pageable)
                : secteurRepository.findAll(pageable))
                .map(this::toDto);
        return buildSuccessResponse(HttpStatus.OK, "Liste des secteurs", "SECTEUR_LIST", result);
    }

    public ResponseEntity<?> getById(Integer id) {
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
    public ResponseEntity<?> update(Integer id, UpdateSecteurDTO dto) {
        Secteur secteur = findOrThrow(id);

        if (dto.getIdVille() != null) {
            Ville ville = villeRepository.findById(dto.getIdVille())
                    .orElseThrow(() -> new ResourceNotFoundException("secteur",dto.getIdVille()));
            secteur.setVille(ville);
        }
        if (dto.getCodeSecteur() != null) secteur.setCodeSecteur(dto.getCodeSecteur());
        if (dto.getNomSecteur() != null) secteur.setNomSecteur(dto.getNomSecteur());

        secteurRepository.save(secteur);
        return buildSuccessResponse(HttpStatus.OK, "Secteur mis à jour", "SECTEUR_UPDATED", toDto(secteur));
    }

    @Transactional
    public ResponseEntity<?> delete(Integer id) {
        secteurRepository.delete(findOrThrow(id));
        return buildSuccessResponse(HttpStatus.OK, "Secteur supprimé", "SECTEUR_DELETED", null);
    }

    private Secteur findOrThrow(Integer id) {
        return secteurRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("secteur",id));
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