package com.immobilier.gestionImmobiliere.modules.medias.services;

import com.immobilier.gestionImmobiliere.donnees.medias.model.Media;
import com.immobilier.gestionImmobiliere.donnees.medias.repository.MediaRepository;
import com.immobilier.gestionImmobiliere.exceptions.MediaStorageException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MediaPersistenceService {

    private static final int MAX_TENTATIVES = 5;

    private final MediaRepository mediaRepository;

    public MediaPersistenceService(MediaRepository mediaRepository) {
        this.mediaRepository = mediaRepository;
    }

    /**
     * Persiste le média avec retry sur collision d'ordre (contrainte unique
     * uk_media_entite_ordre). Chaque tentative ouvre sa propre transaction
     * (REQUIRES_NEW) pour qu'un échec n'invalide pas la transaction appelante.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Media enregistrerAvecRetry(Media media, boolean vouluPrincipal) {
        for (int tentative = 0; tentative < MAX_TENTATIVES; tentative++) {
            try {
                return enregistrer(media, vouluPrincipal);
            } catch (DataIntegrityViolationException e) {
                if (!estCollisionOrdre(e)) throw e;
                long ordre = mediaRepository.countByEntiteTypeAndEntiteId(
                        media.getEntiteType(), media.getEntiteId());
                media.setOrdre((short) ordre);
            }
        }
        throw new MediaStorageException("Impossible d'attribuer un ordre après "
                + MAX_TENTATIVES + " tentatives", null);
    }

    private boolean estCollisionOrdre(DataIntegrityViolationException e) {
        return e.getMessage() != null && e.getMessage().contains("uk_media_entite_ordre");
    }

    /**
     * Persiste le média après que le fichier ait déjà été envoyé sur MinIO.
     * Isolé du réseau MinIO pour ne pas garder une connexion DB ouverte
     * pendant les appels I/O externes.
     */
    @Transactional
    public Media enregistrer(Media media, boolean vouluPrincipal) {
        if (vouluPrincipal) {
            mediaRepository.findByEntiteTypeAndEntiteIdAndIsPrincipalTrue(media.getEntiteType(), media.getEntiteId())
                    .ifPresent(ancien -> {
                        ancien.setIsPrincipal(false);
                        mediaRepository.save(ancien);
                    });
        }
        mediaRepository.save(media);
        return media;
    }
}