package com.immobilier.gestionImmobiliere.modules.medias.services;

import com.immobilier.gestionImmobiliere.donnees.medias.model.Media;
import com.immobilier.gestionImmobiliere.donnees.medias.model.StatutThumbnail;
import com.immobilier.gestionImmobiliere.donnees.medias.model.TypeEntiteMedia;
import com.immobilier.gestionImmobiliere.donnees.medias.repository.MediaRepository;
import com.immobilier.gestionImmobiliere.exceptions.*;
import com.immobilier.gestionImmobiliere.modules.medias.dto.requests.ReorderMediaDTO;
import com.immobilier.gestionImmobiliere.modules.medias.dto.requests.UploadMediaDTO;
import com.immobilier.gestionImmobiliere.modules.medias.dto.requests.UploadMultipleMediaDTO;
import com.immobilier.gestionImmobiliere.modules.medias.dto.responses.MediaResponseDTO;
import com.immobilier.gestionImmobiliere.modules.medias.dto.responses.UploadEchecDTO;
import com.immobilier.gestionImmobiliere.modules.medias.dto.responses.UploadMultipleResultDTO;
import com.immobilier.gestionImmobiliere.modules.medias.events.VideoThumbnailRequestedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.immobilier.gestionImmobiliere.utils.BuildSuccessResponse.buildSuccessResponse;

@Service
public class MediaService {

    private static final Set<String> FORMATS_IMAGE = Set.of("image/jpeg", "image/png", "image/webp");
    private static final Set<String> FORMATS_VIDEO = Set.of("video/mp4", "video/quicktime", "video/webm");

    @Value("${app.medias.max-size-image-mo:5}")
    private int tailleMaxImageMo;

    @Value("${app.medias.max-size-video-mo:50}")
    private int tailleMaxVideoMo;

    private final MediaRepository mediaRepository;
    private final FileStorageService fileStorageService;
    private final MediaPersistenceService mediaPersistenceService;
    private final ApplicationEventPublisher eventPublisher;

    public MediaService(MediaRepository mediaRepository, FileStorageService fileStorageService,
                        MediaPersistenceService mediaPersistenceService, ApplicationEventPublisher eventPublisher) {
        this.mediaRepository = mediaRepository;
        this.fileStorageService = fileStorageService;
        this.mediaPersistenceService = mediaPersistenceService;
        this.eventPublisher = eventPublisher;
    }

    public ResponseEntity<?> getByEntite(TypeEntiteMedia entiteType, Integer entiteId) {
        List<MediaResponseDTO> medias = mediaRepository
                .findByEntiteTypeAndEntiteIdOrderByOrdreAsc(entiteType, entiteId)
                .stream().map(this::toDto).toList();
        return buildSuccessResponse(HttpStatus.OK, "Médias de l'entité", "MEDIA_LIST", medias);
    }

    /**
     * Aucune transaction ouverte pendant les appels réseau MinIO — seule
     * MediaPersistenceService.enregistrerAvecRetry() est transactionnelle.
     */
    public ResponseEntity<?> upload(UploadMediaDTO dto) {
        Media media = uploaderUnFichier(dto.getFichier(), dto.getEntiteType(), dto.getEntiteId(), dto.getIsPrincipal());
        String message = media.getStatutThumbnail() == StatutThumbnail.EN_COURS
                ? "Vidéo uploadée, génération de la miniature en cours"
                : "Média uploadé avec succès";
        return buildSuccessResponse(HttpStatus.CREATED, message, "MEDIA_UPLOADED", toDto(media));
    }

    /**
     * Upload plusieurs fichiers pour la même entité en un seul appel.
     * Chaque fichier est traité INDÉPENDAMMENT : un échec (format/taille invalide)
     * n'interrompt pas le traitement des suivants. Le isPrincipal demandé ne
     * s'applique qu'au premier fichier du lot.
     */
    public ResponseEntity<?> uploadMultiple(UploadMultipleMediaDTO dto) {
        List<MediaResponseDTO> reussis = new ArrayList<>();
        List<UploadEchecDTO> echecs = new ArrayList<>();

        List<MultipartFile> fichiers = dto.getFichiers();
        for (int i = 0; i < fichiers.size(); i++) {
            MultipartFile fichier = fichiers.get(i);
            boolean vouluPrincipal = i == 0 && Boolean.TRUE.equals(dto.getIsPrincipal());
            try {
                Media media = uploaderUnFichier(fichier, dto.getEntiteType(), dto.getEntiteId(), vouluPrincipal);
                reussis.add(toDto(media));
            } catch (Exception e) {
                echecs.add(UploadEchecDTO.builder()
                        .nomFichier(fichier.getOriginalFilename())
                        .motif(e.getMessage())
                        .build());
            }
        }

        UploadMultipleResultDTO result = UploadMultipleResultDTO.builder()
                .reussis(reussis)
                .echecs(echecs)
                .build();

        HttpStatus status = reussis.isEmpty() ? HttpStatus.BAD_REQUEST : HttpStatus.CREATED;
        String message = echecs.isEmpty()
                ? reussis.size() + " média(s) uploadé(s) avec succès"
                : reussis.size() + " réussi(s), " + echecs.size() + " échoué(s)";

        return buildSuccessResponse(status, message, "MEDIA_MULTIPLE_UPLOADED", result);
    }

    /**
     * Logique d'upload unitaire, extraite de l'ancien upload() pour être
     * réutilisable par upload() (1 fichier) et uploadMultiple() (n fichiers).
     */
    private Media uploaderUnFichier(MultipartFile fichier, TypeEntiteMedia entiteType, Integer entiteId, Boolean isPrincipal) {
        String typeReel = fileStorageService.detecterTypeReel(fichier);
        boolean estVideo = validerFichier(fichier, typeReel);

        String bucket = fileStorageService.bucketPour(entiteType);
        String typeDossier = estVideo ? "videos" : "images";
        String sousDossier = entiteType.name().toLowerCase() + "/" + typeDossier;

        String chemin = fileStorageService.store(fichier, bucket, sousDossier);

        String cheminThumbnail = null;
        StatutThumbnail statutThumbnail = StatutThumbnail.PRET;
        String extension = null;

        if (estVideo) {
            statutThumbnail = StatutThumbnail.EN_COURS;
            extension = fileStorageService.extraireExtension(fichier.getOriginalFilename());
        } else {
            cheminThumbnail = fileStorageService.storeThumbnail(fichier, bucket, sousDossier, 400);
        }

        boolean vouluPrincipal = Boolean.TRUE.equals(isPrincipal);
        long ordreSuivant = mediaRepository.countByEntiteTypeAndEntiteId(entiteType, entiteId);
        boolean estPremiere = ordreSuivant == 0;

        Media media = Media.builder()
                .entiteType(entiteType)
                .entiteId(entiteId)
                .typeMedia(typeReel)
                .mediaPath(chemin)
                .mediaPathThumbnail(cheminThumbnail)
                .statutThumbnail(statutThumbnail)
                .isPrincipal(vouluPrincipal || estPremiere)
                .ordre((short) ordreSuivant)
                .dateUpload(LocalDateTime.now())
                .build();

        mediaPersistenceService.enregistrerAvecRetry(media, vouluPrincipal);

        if (estVideo) {
            eventPublisher.publishEvent(new VideoThumbnailRequestedEvent(
                    media.getIdMedia(), bucket, chemin, extension, sousDossier));
        }

        return media;
    }

    /**
     * Soft delete réellement récupérable (Option A). Ne touche PAS MinIO :
     * le fichier reste accessible tant que la purge ne l'a pas traité
     * (délai de rétention configurable, 30 jours par défaut).
     */
    @Transactional
    public ResponseEntity<?> delete(Integer id) {
        Media media = findOrThrow(id);
        mediaRepository.delete(media);

        if (Boolean.TRUE.equals(media.getIsPrincipal())) {
            mediaRepository.findByEntiteTypeAndEntiteIdOrderByOrdreAsc(media.getEntiteType(), media.getEntiteId())
                    .stream().findFirst()
                    .ifPresent(suivant -> {
                        suivant.setIsPrincipal(true);
                        mediaRepository.save(suivant);
                    });
        }

        return buildSuccessResponse(HttpStatus.OK, "Média supprimé (récupérable 30 jours)", "MEDIA_DELETED", null);
    }

    @Transactional
    public ResponseEntity<?> setPrincipal(Integer id) {
        Media media = findOrThrow(id);
        mediaRepository.findByEntiteTypeAndEntiteIdAndIsPrincipalTrue(media.getEntiteType(), media.getEntiteId())
                .ifPresent(ancien -> {
                    ancien.setIsPrincipal(false);
                    mediaRepository.save(ancien);
                });
        media.setIsPrincipal(true);
        mediaRepository.save(media);
        return buildSuccessResponse(HttpStatus.OK, "Image principale mise à jour", "MEDIA_PRINCIPAL_UPDATED", toDto(media));
    }

    @Transactional
    public ResponseEntity<?> reorder(ReorderMediaDTO dto) {
        List<Integer> ids = dto.getIdsMediaOrdonnes();
        List<Media> medias = mediaRepository.findAllById(ids);
        if (medias.size() != ids.size()) {
            throw new ResourceNotFoundException("média", ids);
        }

        TypeEntiteMedia typeReference = medias.get(0).getEntiteType();
        Integer entiteIdReference = medias.get(0).getEntiteId();
        boolean melange = medias.stream().anyMatch(m ->
                m.getEntiteType() != typeReference || !m.getEntiteId().equals(entiteIdReference));
        if (melange) {
            throw new IllegalArgumentException("Tous les médias à réordonner doivent appartenir à la même entité");
        }

        Map<Integer, Media> mediaParId = medias.stream().collect(Collectors.toMap(Media::getIdMedia, m -> m));
        for (short i = 0; i < ids.size(); i++) {
            Media media = mediaParId.get(ids.get(i));
            media.setOrdre(i);
            mediaRepository.save(media);
        }

        return buildSuccessResponse(HttpStatus.OK, "Ordre des médias mis à jour", "MEDIA_REORDERED", null);
    }

    private boolean validerFichier(MultipartFile fichier, String typeReel) {
        if (fichier == null || fichier.isEmpty()) {
            throw new IllegalArgumentException("Aucun fichier fourni");
        }

        long tailleOctets = fichier.getSize();

        if (FORMATS_IMAGE.contains(typeReel)) {
            if (tailleOctets > tailleMaxImageMo * 1024L * 1024L) {
                throw new TailleMediaExcessiveException(tailleOctets);
            }
            return false;
        }

        if (FORMATS_VIDEO.contains(typeReel)) {
            if (tailleOctets > tailleMaxVideoMo * 1024L * 1024L) {
                throw new TailleMediaExcessiveException(tailleOctets);
            }
            return true;
        }

        throw new FormatMediaInvalideException(typeReel);
    }

    private Media findOrThrow(Integer id) {
        return mediaRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("média", id));
    }

    private MediaResponseDTO toDto(Media m) {
        String bucket = fileStorageService.bucketPour(m.getEntiteType());
        boolean estPublic = fileStorageService.estPublic(m.getEntiteType());

        String url = estPublic
                ? fileStorageService.genererUrlPublique(bucket, m.getMediaPath())
                : fileStorageService.genererUrlPresignee(bucket, m.getMediaPath());
        String urlThumbnail = estPublic
                ? fileStorageService.genererUrlPublique(bucket, m.getMediaPathThumbnail())
                : fileStorageService.genererUrlPresignee(bucket, m.getMediaPathThumbnail());

        return MediaResponseDTO.builder()
                .idMedia(m.getIdMedia())
                .entiteType(m.getEntiteType())
                .entiteId(m.getEntiteId())
                .typeMedia(m.getTypeMedia())
                .url(url)
                .urlThumbnail(urlThumbnail)
                .statutThumbnail(m.getStatutThumbnail())
                .isPrincipal(m.getIsPrincipal())
                .ordre(m.getOrdre())
                .dateUpload(m.getDateUpload())
                .build();
    }
}