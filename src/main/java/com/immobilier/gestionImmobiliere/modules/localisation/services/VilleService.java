package com.immobilier.gestionImmobiliere.modules.localisation.services;

import com.immobilier.gestionImmobiliere.donnees.annonces.repository.AnnonceRepository;
import com.immobilier.gestionImmobiliere.donnees.annonces.repository.DemandeRepository;
import com.immobilier.gestionImmobiliere.donnees.annonces.repository.OffreRepository;
import com.immobilier.gestionImmobiliere.donnees.localisation.model.Pays;
import com.immobilier.gestionImmobiliere.donnees.localisation.model.Ville;
import com.immobilier.gestionImmobiliere.donnees.localisation.repository.PaysRepository;
import com.immobilier.gestionImmobiliere.donnees.localisation.repository.SecteurRepository;
import com.immobilier.gestionImmobiliere.donnees.localisation.repository.VilleRepository;
import com.immobilier.gestionImmobiliere.exceptions.CodeAlreadyExistsException;
import com.immobilier.gestionImmobiliere.exceptions.LocalisationUtiliseeException;
import com.immobilier.gestionImmobiliere.exceptions.PaysNotFoundException;
import com.immobilier.gestionImmobiliere.exceptions.ResourceNotFoundException;
import com.immobilier.gestionImmobiliere.exceptions.VilleNotFoundException;
import com.immobilier.gestionImmobiliere.modules.localisation.dto.requests.CreateVilleDTO;
import com.immobilier.gestionImmobiliere.modules.localisation.dto.requests.UpdateVilleDTO;
import com.immobilier.gestionImmobiliere.modules.localisation.dto.responses.VilleResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.immobilier.gestionImmobiliere.utils.BuildSuccessResponse.buildSuccessResponse;

@Service
public class VilleService {

    private final VilleRepository villeRepository;
    private final PaysRepository paysRepository;

    private final SecteurRepository secteurRepository;
    private final AnnonceRepository annonceRepository;
    private final DemandeRepository demandeRepository;
    private final OffreRepository offreRepository;

    public VilleService(VilleRepository villeRepository, PaysRepository paysRepository,
                        SecteurRepository secteurRepository, AnnonceRepository annonceRepository,
                        DemandeRepository demandeRepository, OffreRepository offreRepository) {
        this.villeRepository = villeRepository;
        this.paysRepository = paysRepository;
        this.secteurRepository = secteurRepository;
        this.annonceRepository = annonceRepository;
        this.demandeRepository = demandeRepository;
        this.offreRepository = offreRepository;
    }

    public ResponseEntity<?> getAll(Integer idPays, Pageable pageable) {
        Page<VilleResponseDTO> result = (idPays != null
                ? villeRepository.findByPays_IdPays(idPays, pageable)
                : villeRepository.findAll(pageable))
                .map(this::toDto);
        return buildSuccessResponse(HttpStatus.OK, "Liste des villes", "VILLE_LIST", result);
    }

    public ResponseEntity<?> getById(Integer id) {
        return buildSuccessResponse(HttpStatus.OK, "Ville trouvée", "VILLE_FOUND", toDto(findOrThrow(id)));
    }

    @Transactional
    public ResponseEntity<?> create(CreateVilleDTO dto) {
        Pays pays = paysRepository.findById(dto.getIdPays())
                .orElseThrow(() -> new PaysNotFoundException(dto.getIdPays()));

        if (villeRepository.existsByCodeVilleIgnoreCaseAndPays_IdPays(dto.getCodeVille(), dto.getIdPays())) {
            throw new CodeAlreadyExistsException("ville", dto.getCodeVille());
        }

        Ville ville = Ville.builder()
                .pays(pays)
                .codeVille(dto.getCodeVille())
                .nomVille(dto.getNomVille())
                .build();
        villeRepository.save(ville);
        return buildSuccessResponse(HttpStatus.CREATED, "Ville créée avec succès", "VILLE_CREATED", toDto(ville));
    }

    @Transactional
    public ResponseEntity<?> update(Integer id, UpdateVilleDTO dto) {
        Ville ville = findOrThrow(id);

        if (dto.getIdPays() != null) {
            Pays pays = paysRepository.findById(dto.getIdPays())
                    .orElseThrow(() -> new ResourceNotFoundException("pays",dto.getIdPays()));
            ville.setPays(pays);
        }
        if (dto.getCodeVille() != null) ville.setCodeVille(dto.getCodeVille());
        if (dto.getNomVille() != null) ville.setNomVille(dto.getNomVille());

        villeRepository.save(ville);
        return buildSuccessResponse(HttpStatus.OK, "Ville mise à jour", "VILLE_UPDATED", toDto(ville));
    }

    @Transactional
    public ResponseEntity<?> delete(Integer id) {
        Ville ville = findOrThrow(id);
        long nbSecteurs = secteurRepository.countByVille_IdVille(id);
        long nbAnnonces = annonceRepository.countByVille_IdVille(id);
        long nbDemandes = demandeRepository.countByVille_IdVille(id);
        long nbOffres = offreRepository.countByVille_IdVille(id);
        if (nbSecteurs + nbAnnonces + nbDemandes + nbOffres > 0) {
            throw new LocalisationUtiliseeException("la ville", ville.getNomVille(),
                    nbSecteurs + " secteur(s), " + nbAnnonces + " annonce(s), "
                            + nbDemandes + " demande(s), " + nbOffres + " offre(s)");
        }
        villeRepository.delete(ville);
        return buildSuccessResponse(HttpStatus.OK, "Ville supprimée", "VILLE_DELETED", null);
    }

    private Ville findOrThrow(Integer id) {
        return villeRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("ville",id));
    }

    private VilleResponseDTO toDto(Ville v) {
        return VilleResponseDTO.builder()
                .idVille(v.getIdVille())
                .codeVille(v.getCodeVille())
                .nomVille(v.getNomVille())
                .idPays(v.getPays().getIdPays())
                .nomPays(v.getPays().getNomPays())
                .build();
    }
}