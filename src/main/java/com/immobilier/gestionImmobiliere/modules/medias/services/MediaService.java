package com.immobilier.gestionImmobiliere.modules.medias.services;

import com.immobilier.gestionImmobiliere.donnees.medias.model.Media;
import com.immobilier.gestionImmobiliere.donnees.medias.model.TypeEntiteMedia;
import com.immobilier.gestionImmobiliere.donnees.medias.repository.MediaRepository;
import com.immobilier.gestionImmobiliere.exceptions.FormatMediaInvalideException;
import com.immobilier.gestionImmobiliere.exceptions.ResourceNotFoundException;
import com.immobilier.gestionImmobiliere.exceptions.TailleMediaExcessiveException;
import com.immobilier.gestionImmobiliere.modules.medias.dto.requests.ReorderMediaDTO;
import com.immobilier.gestionImmobiliere.modules.medias.dto.requests.UploadMediaDTO;
import com.immobilier.gestionImmobiliere.modules.medias.dto.responses.MediaResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.immobilier.gestionImmobiliere.utils.BuildSuccessResponse.buildSuccessResponse;

@Service
public class MediaService {

    private static final Set<String> FORMATS_ACCEPTES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final long TAILLE_MAX_OCTETS = 5L * 1024 * 1024; // 5 Mo

    private final MediaRepository mediaRepository;
    private final FileStorageService fileStorageService;

    public MediaService(MediaRepository mediaRepository, FileStorageService fileStorageService) {
        this.mediaRepository = mediaRepository;
        this.fileStorageService = fileStorageService;
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
        validerFichier(fichier);

        String sousDossier = dto.getEntiteType().name().toLowerCase();
        String chemin = fileStorageService.store(fichier, sousDossier);
        String cheminThumbnail = fileStorageService.storeThumbnail(fichier, sousDossier, 400);

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
                .isPrincipal(vouluPrincipal || estPremiere)
                .ordre((short) ordreSuivant)
                .dateUpload(LocalDateTime.now())
                .build();
        mediaRepository.save(media);

        return buildSuccessResponse(HttpStatus.CREATED, "Média uploadé avec succès", "MEDIA_UPLOADED", toDto(media));
    }

    @Transactional
    public ResponseEntity<?> delete(Integer id) {
        Media media = findOrThrow(id);
        fileStorageService.delete(media.getMediaPath());
        fileStorageService.delete(media.getMediaPathThumbnail()); // ajout
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

    private void validerFichier(MultipartFile fichier) {
        if (fichier == null || fichier.isEmpty()) {
            throw new IllegalArgumentException("Aucun fichier fourni");
        }
        if (!FORMATS_ACCEPTES.contains(fichier.getContentType())) {
            throw new FormatMediaInvalideException(fichier.getContentType());
        }
        if (fichier.getSize() > TAILLE_MAX_OCTETS) {
            throw new TailleMediaExcessiveException(fichier.getSize());
        }
    }

    private Media findOrThrow(Integer id) {
        return mediaRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("média", id));
    }

    private MediaResponseDTO toDto(Media m) {
        return MediaResponseDTO.builder()
                .idMedia(m.getIdMedia())
                .entiteType(m.getEntiteType())
                .entiteId(m.getEntiteId())
                .typeMedia(m.getTypeMedia())
                .url(fileStorageService.genererUrlPresignee(m.getMediaPath()))
                .urlThumbnail(fileStorageService.genererUrlPresignee(m.getMediaPathThumbnail()))
                .isPrincipal(m.getIsPrincipal())
                .ordre(m.getOrdre())
                .dateUpload(m.getDateUpload())
                .build();
    }

    // corrigé
    @Transactional
    public ResponseEntity<?> reorder(ReorderMediaDTO dto) {
        List<Integer> ids = dto.getIdsMediaOrdonnes();

        List<Media> medias = mediaRepository.findAllById(ids);
        if (medias.size() != ids.size()) {
            throw new ResourceNotFoundException("média", ids);
        }

        // Validation : tous les médias doivent appartenir à la même entité
        TypeEntiteMedia typeReference = medias.get(0).getEntiteType();
        Integer entiteIdReference = medias.get(0).getEntiteId();

        boolean melange = medias.stream().anyMatch(m ->
                m.getEntiteType() != typeReference || !m.getEntiteId().equals(entiteIdReference));

        if (melange) {
            throw new IllegalArgumentException("Tous les médias à réordonner doivent appartenir à la même entité");
        }

        // Application de l'ordre — respecte l'ordre fourni dans la liste, pas l'ordre de retour de findAllById
        Map<Integer, Media> mediaParId = medias.stream().collect(Collectors.toMap(Media::getIdMedia, m -> m));
        for (short i = 0; i < ids.size(); i++) {
            Media media = mediaParId.get(ids.get(i));
            media.setOrdre(i);
            mediaRepository.save(media);
        }

        return buildSuccessResponse(HttpStatus.OK, "Ordre des médias mis à jour", "MEDIA_REORDERED", null);
    }

}