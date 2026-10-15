package com.immobilier.gestionImmobiliere.modules.documents.services;

import com.immobilier.gestionImmobiliere.donnees.documents.model.Document;
import com.immobilier.gestionImmobiliere.donnees.documents.repository.DocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Enregistre un document généré en s'appuyant sur l'index unique de la table document
 * (un seul document valide par type et par entité / période).
 * <p>
 * Si deux demandes simultanées génèrent le même document, la seconde est refusée par la base :
 * son fichier PDF en trop est supprimé et le document déjà enregistré est renvoyé. L'écriture se fait
 * dans une transaction indépendante pour ne pas invalider celle de l'appelant.
 */
@Component
public class DocumentRegistrar {

    private static final Logger log = LoggerFactory.getLogger(DocumentRegistrar.class);

    private final DocumentRepository documentRepository;
    private final DocumentStorageService documentStorageService;
    private final TransactionTemplate transactionIndependante;

    public DocumentRegistrar(DocumentRepository documentRepository,
                             DocumentStorageService documentStorageService,
                             PlatformTransactionManager transactionManager) {
        this.documentRepository = documentRepository;
        this.documentStorageService = documentStorageService;
        this.transactionIndependante = new TransactionTemplate(transactionManager);
        this.transactionIndependante.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /** @return les octets du PDF à servir : celui qu'on vient de générer, ou celui déjà enregistré en cas de doublon. */
    public byte[] enregistrer(Document document, byte[] pdf) {
        try {
            transactionIndependante.executeWithoutResult(status -> documentRepository.saveAndFlush(document));
            return pdf;
        } catch (DataIntegrityViolationException e) {
            log.info("Document {} déjà enregistré en parallèle, le doublon est ignoré", document.getTypeDocument());
            supprimerFichierEnTrop(document.getCheminFichier());
            return documentStorageService.telecharger(trouverExistant(document).getCheminFichier());
        }
    }

    private Document trouverExistant(Document document) {
        if (document.getPeriodeMois() != null) {
            return documentRepository.findByTypeDocumentAndPeriodeMois(document.getTypeDocument(), document.getPeriodeMois())
                    .orElseThrow(() -> new IllegalStateException("Document introuvable après conflit d'unicité"));
        }
        return documentRepository.findByEntiteTypeAndEntiteIdOrderByCreatedAtDesc(document.getEntiteType(), document.getEntiteId())
                .stream()
                .filter(d -> d.getTypeDocument() == document.getTypeDocument())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Document introuvable après conflit d'unicité"));
    }

    private void supprimerFichierEnTrop(String cle) {
        try {
            documentStorageService.supprimer(cle);
        } catch (RuntimeException e) {
            log.warn("Fichier PDF en double non supprimé ({}) : {}", cle, e.getMessage());
        }
    }
}
