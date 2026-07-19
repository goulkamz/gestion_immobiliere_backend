package com.immobilier.gestionImmobiliere.modules.medias.services;

import com.immobilier.gestionImmobiliere.donnees.medias.model.Media;
import com.immobilier.gestionImmobiliere.donnees.medias.model.StatutThumbnail;
import com.immobilier.gestionImmobiliere.donnees.medias.model.TypeEntiteMedia;
import com.immobilier.gestionImmobiliere.donnees.medias.repository.MediaRepository;
import com.immobilier.gestionImmobiliere.exceptions.FormatMediaInvalideException;
import com.immobilier.gestionImmobiliere.exceptions.MediaStorageException;
import com.immobilier.gestionImmobiliere.exceptions.ResourceNotFoundException;
import com.immobilier.gestionImmobiliere.exceptions.TailleMediaExcessiveException;
import com.immobilier.gestionImmobiliere.modules.medias.dto.requests.ReorderMediaDTO;
import com.immobilier.gestionImmobiliere.modules.medias.dto.requests.UploadMediaDTO;
import com.immobilier.gestionImmobiliere.modules.medias.dto.responses.MediaResponseDTO;
import com.immobilier.gestionImmobiliere.modules.medias.events.VideoThumbnailRequestedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
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
    private final ApplicationEventPublisher eventPublisher;

    public MediaService(MediaRepository mediaRepository, FileStorageService fileStorageService,
                        ApplicationEventPublisher eventPublisher) {
        this.mediaRepository = mediaRepository;
        this.fileStorageService = fileStorageService;
        this.eventPublisher = eventPublisher;
    }

    public ResponseEntity<?> getByEntite(TypeEntiteMedia entiteType, Integer entiteId) {
        List<MediaResponseDTO> medias = mediaRepository
                .findByEntiteTypeAndEntiteIdOrderByOrdreAsc(entiteType, entiteId)
                .stream().map(this::toDto).toList();
        return buildSuccessResponse(HttpStatus.OK, "Médias de l'entité", "MEDIA_LIST", medias);
    }

    @Transactional
    public ResponseEntity<?> upload(UploadMediaDTO dto) {
        MultipartFile fichier = dto.getFichier();
        boolean estVideo = validerFichier(fichier);

        String typeDossier = estVideo ? "videos" : "images";
        String sousDossier = dto.getEntiteType().name().toLowerCase() + "/" + typeDossier;
        String bucket = fileStorageService.getBucketMedias();

        String chemin = fileStorageService.store(fichier, bucket, sousDossier);

        String cheminThumbnail = null;
        StatutThumbnail statutThumbnail = StatutThumbnail.PRET;
        byte[] videoBytes = null;
        String extension = null;

        if (estVideo) {
            statutThumbnail = StatutThumbnail.EN_COURS;
            try {
                videoBytes = fichier.getBytes();
            } catch (IOException e) {
                throw new MediaStorageException("Impossible de lire le fichier vidéo", e);
            }
            extension = fileStorageService.extraireExtension(fichier.getOriginalFilename());
        } else {
            cheminThumbnail = fileStorageService.storeThumbnail(fichier, bucket, sousDossier, 400);
        }

        boolean vouluPrincipal = Boolean.TRUE.equals(dto.getIsPrincipal());
        if (vouluPrincipal) {
            mediaRepository.findByEntiteTypeAndEntiteIdAndIsPrincipalTrue(dto.getEntiteType(), dto.getEntiteId())
                    .ifPresent(ancien -> {
                        ancien.setIsPrincipal(false);
                        mediaRepository.save(ancien);
                    });
        }

        long ordreSuivant = mediaRepository.countByEntiteTypeAndEntiteId(dto.getEntiteType(), dto.getEntiteId());
        boolean estPremiere = ordreSuivant == 0;

        Media media = Media.builder()
                .entiteType(dto.getEntiteType())
                .entiteId(dto.getEntiteId())
                .typeMedia(fichier.getContentType())
                .mediaPath(chemin)
                .mediaPathThumbnail(cheminThumbnail)
                .statutThumbnail(statutThumbnail)
                .isPrincipal(vouluPrincipal || estPremiere)
                .ordre((short) ordreSuivant)
                .dateUpload(LocalDateTime.now())
                .build();
        mediaRepository.save(media);

        if (estVideo) {
            eventPublisher.publishEvent(new VideoThumbnailRequestedEvent(
                    media.getIdMedia(), videoBytes, extension, bucket, sousDossier));
        }

        String message = estVideo
                ? "Vidéo uploadée, génération de la miniature en cours"
                : "Média uploadé avec succès";

        return buildSuccessResponse(HttpStatus.CREATED, message, "MEDIA_UPLOADED", toDto(media));
    }

    @Transactional
    public ResponseEntity<?> delete(Integer id) {
        Media media = findOrThrow(id);
        String bucket = fileStorageService.getBucketMedias();
        fileStorageService.delete(bucket, media.getMediaPath());
        fileStorageService.delete(bucket, media.getMediaPathThumbnail());
        mediaRepository.delete(media);

        if (Boolean.TRUE.equals(media.getIsPrincipal())) {
            mediaRepository.findByEntiteTypeAndEntiteIdOrderByOrdreAsc(media.getEntiteType(), media.getEntiteId())
                    .stream().findFirst()
                    .ifPresent(suivant -> {
                        suivant.setIsPrincipal(true);
                        mediaRepository.save(suivant);
                    });
        }

        return buildSuccessResponse(HttpStatus.OK, "Média supprimé", "MEDIA_DELETED", null);
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

    private boolean validerFichier(MultipartFile fichier) {
        if (fichier == null || fichier.isEmpty()) {
            throw new IllegalArgumentException("Aucun fichier fourni");
        }

        String contentType = fichier.getContentType();
        long tailleOctets = fichier.getSize();

        if (FORMATS_IMAGE.contains(contentType)) {
            if (tailleOctets > tailleMaxImageMo * 1024L * 1024L) {
                throw new TailleMediaExcessiveException(tailleOctets);
            }
            return false;
        }

        if (FORMATS_VIDEO.contains(contentType)) {
            if (tailleOctets > tailleMaxVideoMo * 1024L * 1024L) {
                throw new TailleMediaExcessiveException(tailleOctets);
            }
            return true;
        }

        throw new FormatMediaInvalideException(contentType);
    }

    private Media findOrThrow(Integer id) {
        return mediaRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("média", id));
    }

    private MediaResponseDTO toDto(Media m) {
        String bucket = fileStorageService.getBucketMedias();
        return MediaResponseDTO.builder()
                .idMedia(m.getIdMedia())
                .entiteType(m.getEntiteType())
                .entiteId(m.getEntiteId())
                .typeMedia(m.getTypeMedia())
                .url(fileStorageService.genererUrlPresignee(bucket, m.getMediaPath()))
                .urlThumbnail(fileStorageService.genererUrlPresignee(bucket, m.getMediaPathThumbnail()))
                .statutThumbnail(m.getStatutThumbnail())
                .isPrincipal(m.getIsPrincipal())
                .ordre(m.getOrdre())
                .dateUpload(m.getDateUpload())
                .build();
    }
}