package com.immobilier.gestionImmobiliere.modules.medias.services;

import com.immobilier.gestionImmobiliere.donnees.medias.model.Media;
import com.immobilier.gestionImmobiliere.donnees.medias.repository.MediaRepository;
import com.immobilier.gestionImmobiliere.exceptions.MediaStorageException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class MediaPersistenceService {

    private static final int MAX_TENTATIVES = 5;

    private final MediaRepository mediaRepository;
    private final TransactionTemplate nouvelleTransaction;

    public MediaPersistenceService(MediaRepository mediaRepository, PlatformTransactionManager transactionManager) {
        this.mediaRepository = mediaRepository;
        this.nouvelleTransaction = new TransactionTemplate(transactionManager);
        this.nouvelleTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /**
     * Persiste le média (fichier deja envoye sur MinIO) avec retry si un upload
     * concurrent a pris le meme ordre (contrainte uk_media_entite_ordre).
     * Chaque tentative a sa propre transaction : apres une violation, la session
     * Hibernate de la tentative echouee est inutilisable.
     */
    public Media enregistrerAvecRetry(Media media, boolean vouluPrincipal) {
        for (int tentative = 0; tentative < MAX_TENTATIVES; tentative++) {
            try {
                return nouvelleTransaction.execute(status -> enregistrer(media, vouluPrincipal));
            } catch (DataIntegrityViolationException e) {
                if (!estCollisionOrdre(e)) throw e;
                media.setIdMedia(null);
                media.setOrdre((short) mediaRepository.prochainOrdre(media.getEntiteType(), media.getEntiteId()));
            }
        }
        throw new MediaStorageException("Impossible d'attribuer un ordre après "
                + MAX_TENTATIVES + " tentatives", null);
    }

    private boolean estCollisionOrdre(DataIntegrityViolationException e) {
        return e.getMessage() != null && e.getMessage().contains("uk_media_entite_ordre");
    }

    private Media enregistrer(Media media, boolean vouluPrincipal) {
        if (vouluPrincipal) {
            mediaRepository.findByEntiteTypeAndEntiteIdAndIsPrincipalTrue(media.getEntiteType(), media.getEntiteId())
                    .ifPresent(ancien -> {
                        ancien.setIsPrincipal(false);
                        mediaRepository.save(ancien);
                    });
        }
        return mediaRepository.save(media);
    }
}
