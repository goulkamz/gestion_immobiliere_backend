package com.immobilier.gestionImmobiliere.modules.medias.services;

import com.immobilier.gestionImmobiliere.donnees.medias.model.TypeEntiteMedia;
import com.immobilier.gestionImmobiliere.donnees.medias.repository.MediaRepository;
import com.immobilier.gestionImmobiliere.donnees.parametres.model.CleParametre;
import com.immobilier.gestionImmobiliere.modules.parametres.services.ParametreService;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import io.minio.Result;
import io.minio.messages.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class MediaReconciliationService {

    private static final Logger log = LoggerFactory.getLogger(MediaReconciliationService.class);

    private final MinioClient minioClient;
    private final MediaRepository mediaRepository;
    private final FileStorageService fileStorageService;
    private final ParametreService parametreService;

    @Value("${app.minio.bucket-medias-public}")
    private String bucketPublic;

    @Value("${app.minio.bucket-medias-prive}")
    private String bucketPrive;


    public MediaReconciliationService(@Qualifier("minioClient") MinioClient minioClient,
                                      MediaRepository mediaRepository,
                                      FileStorageService fileStorageService, ParametreService parametreService) {
        this.minioClient = minioClient;
        this.mediaRepository = mediaRepository;
        this.fileStorageService = fileStorageService;
        this.parametreService = parametreService;
    }

    /**
     * Détecte et supprime les objets MinIO qui n'ont JAMAIS eu de ligne DB
     * associée (crash entre l'upload MinIO et le save() en base).
     *
     * Inclut volontairement les médias soft-deleted (is_deleted=true) dans les
     * clés "référencées" : leur suppression physique relève exclusivement de
     * purgerMediasSupprimes(), après le délai de rétention.
     */
    public void nettoyerFichiersOrphelins() {
        Set<String> clesReferencees = new HashSet<>();
        for (Object[] row : mediaRepository.findAllCheminsIncluantSupprimes()) {
            if (row[0] != null) clesReferencees.add((String) row[0]);
            if (row[1] != null) clesReferencees.add((String) row[1]);
        }

        nettoyerBucket(bucketPublic, clesReferencees);
        nettoyerBucket(bucketPrive, clesReferencees);
    }

    private void nettoyerBucket(String bucket, Set<String> clesReferencees) {
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
                log.warn("Erreur lors de la réconciliation d'un objet MinIO (bucket {})", bucket, e);
            }
        }
        log.info("Réconciliation bucket {} : {} objet(s) orphelin(s) supprimé(s)", bucket, supprimes);
    }

    /**
     * Purge définitivement les médias soft-deleted depuis plus de retentionJours :
     * supprime le fichier réel sur MinIO PUIS la ligne DB (hard delete).
     * Si la suppression MinIO échoue, la ligne DB reste soft-deleted pour être
     * retentée au prochain passage — jamais de hardDelete si le fichier persiste.
     */
    @Transactional
    public void purgerMediasSupprimes() {
        int tolerance = parametreService.getEntier(CleParametre.TOLERANCE_LOCATION_JOURS, 30);
        LocalDateTime seuil = LocalDateTime.now().minusDays(tolerance);
        List<Object[]> candidats = mediaRepository.findCandidatsPurge(seuil);

        int purges = 0;
        for (Object[] row : candidats) {
            Integer idMedia = (Integer) row[0];
            String mediaPath = (String) row[1];
            String mediaPathThumbnail = (String) row[2];
            TypeEntiteMedia entiteType = TypeEntiteMedia.valueOf((String) row[3]);

            String bucket = fileStorageService.bucketPour(entiteType);
            try {
                fileStorageService.delete(bucket, mediaPath);
                fileStorageService.delete(bucket, mediaPathThumbnail);
                mediaRepository.hardDelete(idMedia);
                purges++;
            } catch (Exception e) {
                log.warn("Échec purge média id {}, sera retenté : {}", idMedia, e.getMessage());
            }
        }

        log.info("Purge médias : {} média(s) définitivement supprimé(s) (rétention {} jours)", purges, tolerance);
    }
}