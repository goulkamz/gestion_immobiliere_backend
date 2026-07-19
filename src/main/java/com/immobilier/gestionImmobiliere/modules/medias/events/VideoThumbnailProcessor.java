package com.immobilier.gestionImmobiliere.modules.medias.events;

import com.immobilier.gestionImmobiliere.donnees.medias.model.Media;
import com.immobilier.gestionImmobiliere.donnees.medias.model.StatutThumbnail;
import com.immobilier.gestionImmobiliere.donnees.medias.repository.MediaRepository;
import com.immobilier.gestionImmobiliere.modules.medias.services.FileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class VideoThumbnailProcessor {

    private static final Logger log = LoggerFactory.getLogger(VideoThumbnailProcessor.class);

    private final FileStorageService fileStorageService;
    private final MediaRepository mediaRepository;

    public VideoThumbnailProcessor(FileStorageService fileStorageService, MediaRepository mediaRepository) {
        this.fileStorageService = fileStorageService;
        this.mediaRepository = mediaRepository;
    }

    /**
     * S'exécute APRÈS le commit de la transaction d'upload, sur le pool
     * "thumbnailExecutor" (jamais sur le thread de la requête HTTP originale).
     */
    @Async("thumbnailExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onVideoThumbnailRequested(VideoThumbnailRequestedEvent event) {
        try {
            String cheminThumbnail = fileStorageService.storeVideoThumbnailFromBytes(
                    event.videoBytes(), event.extension(), event.bucket(), event.sousDossier());

            mediaRepository.findById(event.idMedia()).ifPresent(media -> {
                media.setMediaPathThumbnail(cheminThumbnail);
                media.setStatutThumbnail(StatutThumbnail.PRET);
                mediaRepository.save(media);
            });

            log.info("Miniature vidéo générée avec succès pour media id {}", event.idMedia());
        } catch (Exception e) {
            log.error("Échec génération miniature vidéo pour media id {}", event.idMedia(), e);
            mediaRepository.findById(event.idMedia()).ifPresent(media -> {
                media.setStatutThumbnail(StatutThumbnail.ECHEC);
                mediaRepository.save(media);
            });
        }
    }
}