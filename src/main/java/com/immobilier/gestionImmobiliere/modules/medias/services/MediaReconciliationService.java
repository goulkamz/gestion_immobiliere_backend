package com.immobilier.gestionImmobiliere.modules.medias.services;

import com.immobilier.gestionImmobiliere.donnees.medias.repository.MediaRepository;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import io.minio.Result;
import io.minio.messages.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MediaReconciliationService {

    private static final Logger log = LoggerFactory.getLogger(MediaReconciliationService.class);

    private final MinioClient minioClient;
    private final MediaRepository mediaRepository;

    @Value("${app.minio.bucket}")
    private String bucket;

    public MediaReconciliationService(@Qualifier("minioClient") MinioClient minioClient, MediaRepository mediaRepository) {
        this.minioClient = minioClient;
        this.mediaRepository = mediaRepository;
    }

    /**
     * Job hebdomadaire — détecte et supprime les objets MinIO qui ne sont
     * référencés par aucune ligne active dans la table `medias`.
     */
    public void nettoyerFichiersOrphelins() {
        Set<String> clesReferencees = mediaRepository.findAll().stream()
                .flatMap(m -> java.util.stream.Stream.of(m.getMediaPath(), m.getMediaPathThumbnail()))
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());

        int supprimes = 0;
        Iterable<Result<Item>> objets = minioClient.listObjects(
                ListObjectsArgs.builder().bucket(bucket).recursive(true).build());

        for (Result<Item> resultat : objets) {
            try {
                String cle = resultat.get().objectName();
                if (!clesReferencees.contains(cle)) {
                    minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(cle).build());
                    supprimes++;
                }
            } catch (Exception e) {
                log.warn("Erreur lors de la réconciliation d'un objet MinIO", e);
            }
        }

        log.info("Réconciliation médias terminée : {} objet(s) orphelin(s) supprimé(s)", supprimes);
    }
}