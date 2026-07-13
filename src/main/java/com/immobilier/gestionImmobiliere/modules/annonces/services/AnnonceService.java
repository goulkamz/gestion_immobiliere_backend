package com.immobilier.gestionImmobiliere.modules.annonces.services;

import com.immobilier.gestionImmobiliere.donnees.annonces.model.Annonce;
import com.immobilier.gestionImmobiliere.donnees.annonces.model.StatutAnnonce;
import com.immobilier.gestionImmobiliere.donnees.annonces.repository.AnnonceRepository;
import com.immobilier.gestionImmobiliere.exceptions.DateExpirationInvalideException;
import com.immobilier.gestionImmobiliere.exceptions.ResourceNotFoundException;
import com.immobilier.gestionImmobiliere.modules.annonces.dto.requests.CreateAnnonceDTO;
import com.immobilier.gestionImmobiliere.modules.annonces.dto.requests.UpdateAnnonceDTO;
import com.immobilier.gestionImmobiliere.modules.annonces.dto.requests.UpdateStatutAnnonceDTO;
import com.immobilier.gestionImmobiliere.modules.annonces.dto.responses.AnnonceResponseDTO;
import com.immobilier.gestionImmobiliere.modules.journal.services.JournalService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static com.immobilier.gestionImmobiliere.utils.BuildSuccessResponse.buildSuccessResponse;

@Service
public class AnnonceService {

    private final AnnonceRepository annonceRepository;
    private final JournalService journalService;

    public AnnonceService(AnnonceRepository annonceRepository, JournalService journalService) {
        this.annonceRepository = annonceRepository;
        this.journalService = journalService;
    }

    public ResponseEntity<?> getAll(StatutAnnonce statut, Pageable pageable) {
        Page<Annonce> page = statut != null ? annonceRepository.findByStatut(statut, pageable) : annonceRepository.findAll(pageable);
        return buildSuccessResponse(HttpStatus.OK, "Liste des annonces", "ANNONCE_LIST", page.map(this::toDto));
    }

    public ResponseEntity<?> getById(Integer id) {
        return buildSuccessResponse(HttpStatus.OK, "Annonce trouvée", "ANNONCE_FOUND", toDto(findOrThrow(id)));
    }

    @Transactional
    public ResponseEntity<?> create(CreateAnnonceDTO dto) {
        LocalDateTime publication = LocalDateTime.now();
        if (!dto.getDateExpiration().isAfter(publication)) {
            throw new DateExpirationInvalideException();
        }

        Annonce annonce = Annonce.builder()
                .titre(dto.getTitre())
                .description(dto.getDescription())
                .typeAnnonce(dto.getTypeAnnonce())
                .datePublication(publication)
                .dateExpiration(dto.getDateExpiration())
                .prix(dto.getPrix())
                .localisation(dto.getLocalisation())
                .statut(StatutAnnonce.ACTIVE)
                .build();
        annonceRepository.save(annonce);
        return buildSuccessResponse(HttpStatus.CREATED, "Annonce créée avec succès", "ANNONCE_CREATED", toDto(annonce));
    }

    @Transactional
    public ResponseEntity<?> update(Integer id, UpdateAnnonceDTO dto) {
        Annonce annonce = findOrThrow(id);

        if (dto.getDateExpiration() != null && !dto.getDateExpiration().isAfter(annonce.getDatePublication())) {
            throw new DateExpirationInvalideException();
        }

        if (dto.getTitre() != null) annonce.setTitre(dto.getTitre());
        if (dto.getDescription() != null) annonce.setDescription(dto.getDescription());
        if (dto.getTypeAnnonce() != null) annonce.setTypeAnnonce(dto.getTypeAnnonce());
        if (dto.getDateExpiration() != null) annonce.setDateExpiration(dto.getDateExpiration());
        if (dto.getPrix() != null) annonce.setPrix(dto.getPrix());
        if (dto.getLocalisation() != null) annonce.setLocalisation(dto.getLocalisation());

        annonceRepository.save(annonce);
        return buildSuccessResponse(HttpStatus.OK, "Annonce mise à jour", "ANNONCE_UPDATED", toDto(annonce));
    }

    @Transactional
    public ResponseEntity<?> updateStatut(Integer id, UpdateStatutAnnonceDTO dto,Integer currentUserId) {
        Annonce annonce = findOrThrow(id);
        String ancienStatut = annonce.getStatut().name();
        annonce.setStatut(dto.getStatut());

        annonceRepository.save(annonce);
        journalService.enregistrer(currentUserId, "CHANGEMENT_STATUT", "annonce", annonce.getIdAnnonce(),
                "Transition de statut de l'annonce", "statut=" + ancienStatut, "statut=" + dto.getStatut().name());

        return buildSuccessResponse(HttpStatus.OK, "Statut mis à jour", "ANNONCE_STATUT_UPDATED", toDto(annonce));
    }

    @Transactional
    public ResponseEntity<?> delete(Integer id) {
        annonceRepository.delete(findOrThrow(id));
        return buildSuccessResponse(HttpStatus.OK, "Annonce supprimée", "ANNONCE_DELETED", null);
    }

    /**
     * Job quotidien — bascule ACTIVE -> EXPIREE une fois la date d'expiration dépassée.
     */
    @Scheduled(cron = "0 0 1 * * *")
    @Transactional
    public void expirerAnnoncesAutomatiquement() {
        List<Annonce> expirees = annonceRepository.findByStatutAndDateExpirationBefore(StatutAnnonce.ACTIVE, LocalDateTime.now());
        expirees.forEach(a -> a.setStatut(StatutAnnonce.EXPIREE));
        annonceRepository.saveAll(expirees);
    }

    private Annonce findOrThrow(Integer id) {
        return annonceRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("annonce", id));
    }

    private AnnonceResponseDTO toDto(Annonce a) {
        return AnnonceResponseDTO.builder()
                .idAnnonce(a.getIdAnnonce())
                .titre(a.getTitre())
                .description(a.getDescription())
                .typeAnnonce(a.getTypeAnnonce())
                .datePublication(a.getDatePublication())
                .dateExpiration(a.getDateExpiration())
                .statut(a.getStatut())
                .prix(a.getPrix())
                .localisation(a.getLocalisation())
                .build();
    }
}