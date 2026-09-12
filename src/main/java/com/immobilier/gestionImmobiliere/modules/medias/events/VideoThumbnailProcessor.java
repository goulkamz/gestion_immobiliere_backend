package com.immobilier.gestionImmobiliere.modules.medias.events;

import com.immobilier.gestionImmobiliere.donnees.medias.model.StatutThumbnail;
import com.immobilier.gestionImmobiliere.donnees.medias.repository.MediaRepository;
import com.immobilier.gestionImmobiliere.modules.medias.events.VideoThumbnailRequestedEvent;
import com.immobilier.gestionImmobiliere.modules.medias.services.FileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@Component
public class VideoThumbnailProcessor {

    private static final Logger log = LoggerFactory.getLogger(VideoThumbnailProcessor.class);

    private final FileStorageService fileStorageService;
    private final MediaRepository mediaRepository;

    public VideoThumbnailProcessor(FileStorageService fileStorageService,
                                   MediaRepository mediaRepository) {
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
        Path tempVideo = null;
        Path tempFrame = null;
        try {
            // 1. Téléchargement streaming MinIO -> fichier temp (pas de RAM)
            tempVideo = Files.createTempFile("video-", event.extension());
            fileStorageService.downloadToFile(event.bucket(), event.cleVideo(), tempVideo);

            // 2. Extraction de la frame par ffmpeg
            tempFrame = Files.createTempFile("frame-", ".jpg");
            executerFfmpeg(tempVideo, tempFrame);

            // 3. Upload de la miniature depuis le fichier temp
            String cheminThumbnail = fileStorageService.storeVideoThumbnailFromFile(
                    tempFrame, event.bucket(), event.sousDossier());

            // 4. Mise à jour du média
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
        } finally {
            supprimerSiExiste(tempVideo);
            supprimerSiExiste(tempFrame);
        }
    }

    private void executerFfmpeg(Path video, Path frame) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(
                "ffmpeg", "-y",
                "-i", video.toString(),
                "-ss", "00:00:01",
                "-vframes", "1",
                "-vf", "scale=400:-1",
                frame.toString()
        );
        pb.redirectErrorStream(true);
        Process process = pb.start();

        try (var reader = process.getInputStream()) {
            reader.readAllBytes();
        }

        boolean termine = process.waitFor(30, TimeUnit.SECONDS);
        if (!termine) {
            process.destroyForcibly();
            throw new IOException("Timeout ffmpeg (>30s)");
        }
        if (process.exitValue() != 0) {
            throw new IOException("ffmpeg a échoué avec code " + process.exitValue());
        }
    }

    private void supprimerSiExiste(Path chemin) {
        if (chemin == null) return;
        try {
            Files.deleteIfExists(chemin);
        } catch (IOException ignored) {
            // best-effort : les fichiers temp sont nettoyés par l'OS de toute façon
        }
    }
}