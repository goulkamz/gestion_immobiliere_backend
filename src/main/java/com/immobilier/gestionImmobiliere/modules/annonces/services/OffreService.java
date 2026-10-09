package com.immobilier.gestionImmobiliere.modules.annonces.services;

import com.immobilier.gestionImmobiliere.donnees.annonces.model.Offre;
import com.immobilier.gestionImmobiliere.donnees.annonces.model.StatutOffre;
import com.immobilier.gestionImmobiliere.donnees.annonces.repository.OffreRepository;
import com.immobilier.gestionImmobiliere.exceptions.ResourceNotFoundException;
import com.immobilier.gestionImmobiliere.exceptions.TooManyRequestsException;
import com.immobilier.gestionImmobiliere.modules.annonces.dto.requests.CreateOffreDTO;
import com.immobilier.gestionImmobiliere.modules.annonces.dto.requests.UpdateStatutOffreDTO;
import com.immobilier.gestionImmobiliere.modules.annonces.dto.responses.OffreResponseDTO;
import com.immobilier.gestionImmobiliere.modules.medias.dto.requests.UploadPhotosOffreDTO;
import com.immobilier.gestionImmobiliere.modules.medias.services.MediaService;
import com.immobilier.gestionImmobiliere.modules.user.jwt.RateLimitService;
import com.immobilier.gestionImmobiliere.utils.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import com.immobilier.gestionImmobiliere.donnees.medias.model.TypeEntiteMedia;
import com.immobilier.gestionImmobiliere.utils.RechercheSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

import static com.immobilier.gestionImmobiliere.utils.BuildSuccessResponse.buildSuccessResponse;

@Service
public class OffreService {

    private final OffreRepository offreRepository;
    private final RateLimitService rateLimitService;
    private final ClientIpResolver clientIpResolver;
    private final JetonDepotOffreService jetonDepotOffreService;
    private final MediaService mediaService;

    // Depot public de photos : plafond par offre et par IP
    private static final int MAX_PHOTOS_PAR_OFFRE = 10;
    private static final int MAX_ENVOIS_PHOTOS_PAR_HEURE = 20;

    public OffreService(OffreRepository offreRepository, RateLimitService rateLimitService, ClientIpResolver clientIpResolver,
                        JetonDepotOffreService jetonDepotOffreService, MediaService mediaService) {
        this.offreRepository = offreRepository;
        this.rateLimitService = rateLimitService;
        this.clientIpResolver = clientIpResolver;
        this.jetonDepotOffreService = jetonDepotOffreService;
        this.mediaService = mediaService;
    }

    public ResponseEntity<?> getAll(StatutOffre statut, String recherche, Pageable pageable) {
        Page<Offre> offres = offreRepository.findAll(
                RechercheSpecification.<Offre>statutEtTexte(statut, recherche, "nomComplet", "email", "titre", "adresse", "typeOffre"), pageable);
        Page<OffreResponseDTO> page = offres.map(this::toDto);
        return buildSuccessResponse(HttpStatus.OK, "Liste des offres", "OFFRE_LIST", page);
    }

    public ResponseEntity<?> getById(Integer id) {
        return buildSuccessResponse(HttpStatus.OK, "Offre trouvée", "OFFRE_FOUND", toDto(findOrThrow(id)));
    }

    @Transactional
    public ResponseEntity<?> create(CreateOffreDTO dto, HttpServletRequest request) {
        if (rateLimitService.estLimiteDepassee("offre:" + clientIpResolver.resolve(request))) {
            throw new TooManyRequestsException("Trop d'offres envoyées. Veuillez réessayer plus tard.");
        }

        Offre offre = Offre.builder()
                .nomComplet(dto.getNomComplet())
                .email(dto.getEmail())
                .telephone(dto.getTelephone())
                .typeOffre(dto.getTypeOffre())
                .titre(dto.getTitre())
                .description(dto.getDescription())
                .adresse(dto.getAdresse())
                .dateOffre(LocalDateTime.now())
                .statut(StatutOffre.EN_ATTENTE)
                .build();
        offreRepository.save(offre);
        OffreResponseDTO reponse = toDto(offre);
        reponse.setJetonDepotPhotos(jetonDepotOffreService.generer(offre.getIdOffre()));
        return buildSuccessResponse(HttpStatus.CREATED, "Offre déposée avec succès", "OFFRE_CREATED", reponse);
    }

    /**
     * Depot public de photos sur une offre. Autorise par le jeton remis a la creation de
     * l'offre (valable 1 h) : sans lui, n'importe qui pourrait remplir l'offre d'un tiers.
     */
    public ResponseEntity<?> deposerPhotos(Integer id, String jeton, UploadPhotosOffreDTO dto, HttpServletRequest request) {
        if (rateLimitService.estLimiteDepassee("offre-photos:" + clientIpResolver.resolve(request),
                MAX_ENVOIS_PHOTOS_PAR_HEURE, Duration.ofHours(1))) {
            throw new TooManyRequestsException("Trop d'envois de photos. Veuillez réessayer plus tard.");
        }

        Offre offre = findOrThrow(id);
        if (!jetonDepotOffreService.estValide(id, jeton)) {
            throw new AccessDeniedException("Jeton de dépôt invalide ou expiré");
        }
        if (offre.getStatut() != StatutOffre.EN_ATTENTE) {
            throw new IllegalArgumentException("Cette offre n'accepte plus de photos");
        }

        long restant = MAX_PHOTOS_PAR_OFFRE - mediaService.compterMedias(TypeEntiteMedia.OFFRE, id);
        if (dto.getFichiers().size() > restant) {
            throw new IllegalArgumentException("Maximum " + MAX_PHOTOS_PAR_OFFRE + " photos par offre");
        }
        return mediaService.uploadPhotosOffre(id, dto.getFichiers());
    }

    @Transactional
    public ResponseEntity<?> updateStatut(Integer id, UpdateStatutOffreDTO dto) {
        Offre offre = findOrThrow(id);
        offre.setStatut(dto.getStatut());
        offreRepository.save(offre);
        return buildSuccessResponse(HttpStatus.OK, "Statut mis à jour", "OFFRE_STATUT_UPDATED", toDto(offre));
    }

    private Offre findOrThrow(Integer id) {
        return offreRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("offre", id));
    }

    private OffreResponseDTO toDto(Offre o) {
        return OffreResponseDTO.builder()
                .idOffre(o.getIdOffre())
                .nomComplet(o.getNomComplet())
                .email(o.getEmail())
                .telephone(o.getTelephone())
                .typeOffre(o.getTypeOffre())
                .titre(o.getTitre())
                .description(o.getDescription())
                .adresse(o.getAdresse())
                .dateOffre(o.getDateOffre())
                .statut(o.getStatut())
                .build();
    }
}