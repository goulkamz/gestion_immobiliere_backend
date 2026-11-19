package com.immobilier.gestionImmobiliere.modules.depenses.services;

import com.immobilier.gestionImmobiliere.donnees.depenses.model.DevisTravaux;
import com.immobilier.gestionImmobiliere.donnees.depenses.model.StatutDevis;
import com.immobilier.gestionImmobiliere.donnees.depenses.repository.DevisTravauxRepository;
import com.immobilier.gestionImmobiliere.donnees.documents.model.Document;
import com.immobilier.gestionImmobiliere.donnees.documents.model.TypeDocument;
import com.immobilier.gestionImmobiliere.donnees.documents.model.TypeEntiteDocument;
import com.immobilier.gestionImmobiliere.donnees.documents.repository.DocumentRepository;
import com.immobilier.gestionImmobiliere.exceptions.ResourceNotFoundException;
import com.immobilier.gestionImmobiliere.modules.documents.services.DocumentStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static com.immobilier.gestionImmobiliere.utils.BuildSuccessResponse.buildSuccessResponse;

/**
 * Devis PDF de l'artisan joint à un devis de travaux, pour que le bailleur voie le document réel avant de répondre.
 * Un seul PDF par devis ; le joindre à nouveau remplace le précédent. Il ne se modifie plus une fois le devis accepté :
 * l'accord porte sur ce que le bailleur a eu sous les yeux.
 */
@Service
public class DevisPieceJointeService {

    private static final Logger log = LoggerFactory.getLogger(DevisPieceJointeService.class);
    private static final long TAILLE_MAX = 10L * 1024 * 1024;
    private static final List<StatutDevis> STATUTS_MODIFIABLES = List.of(StatutDevis.EN_ATTENTE, StatutDevis.A_RENEGOCIER, StatutDevis.EXPIRE);

    private final DevisTravauxRepository devisRepository;
    private final DocumentRepository documentRepository;
    private final DocumentStorageService storageService;

    public DevisPieceJointeService(DevisTravauxRepository devisRepository, DocumentRepository documentRepository,
                                   DocumentStorageService storageService) {
        this.devisRepository = devisRepository;
        this.documentRepository = documentRepository;
        this.storageService = storageService;
    }

    @Transactional
    public ResponseEntity<?> joindre(Integer idDevis, MultipartFile fichier, Integer currentUserId) {
        DevisTravaux devis = findOrThrow(idDevis);
        verifierModifiable(devis);
        if (fichier == null || fichier.isEmpty()) {
            throw new IllegalArgumentException("Aucun fichier reçu");
        }
        if (fichier.getSize() > TAILLE_MAX) {
            throw new IllegalArgumentException("Le fichier dépasse 10 Mo");
        }
        byte[] contenu;
        try {
            contenu = fichier.getBytes();
        } catch (IOException e) {
            throw new IllegalStateException("Le fichier n'a pas pu être lu", e);
        }
        // Contenu réel vérifié (et non le type déclaré par le client) : un PDF commence toujours par « %PDF- »
        if (contenu.length < 5 || !new String(contenu, 0, 5, StandardCharsets.ISO_8859_1).equals("%PDF-")) {
            throw new IllegalArgumentException("Le devis joint doit être un fichier PDF");
        }

        String cle = storageService.store(contenu, "devis");
        try {
            remplacerExistant(idDevis);
            documentRepository.save(Document.builder()
                    .typeDocument(TypeDocument.DEVIS_PIECE_JOINTE)
                    .entiteType(TypeEntiteDocument.DEVIS_TRAVAUX)
                    .entiteId(idDevis)
                    .cheminFichier(cle)
                    .createdAt(LocalDateTime.now())
                    .userCreate(currentUserId)
                    .build());
        } catch (RuntimeException e) {
            supprimerFichier(cle);
            throw e;
        }
        return buildSuccessResponse(HttpStatus.CREATED, "Devis PDF joint", "DEVIS_ATTACHMENT_ADDED", null);
    }

    @Transactional
    public ResponseEntity<?> retirer(Integer idDevis) {
        DevisTravaux devis = findOrThrow(idDevis);
        verifierModifiable(devis);
        if (!remplacerExistant(idDevis)) {
            throw new ResourceNotFoundException("piece jointe du devis", idDevis);
        }
        return buildSuccessResponse(HttpStatus.OK, "Devis PDF retiré", "DEVIS_ATTACHMENT_REMOVED", null);
    }

    /**
     * Nettoyage quand un devis est retiré : le PDF ne sert plus (appelé par DevisService, sauf si le devis avait été accepté :
     * le document est alors la preuve de l'accord). Le fichier n'est supprimé du stockage qu'une fois la transaction validée.
     */
    @Transactional
    public void supprimerSiPresent(Integer idDevis) {
        var existant = documentRepository.findFirstByTypeDocumentAndEntiteTypeAndEntiteId(
                TypeDocument.DEVIS_PIECE_JOINTE, TypeEntiteDocument.DEVIS_TRAVAUX, idDevis);
        if (existant.isEmpty()) return;
        String cle = existant.get().getCheminFichier();
        documentRepository.delete(existant.get());
        org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                new org.springframework.transaction.support.TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        supprimerFichier(cle);
                    }
                });
    }

    /** Admin et agent : tous les devis. Bailleur : ceux de ses cours uniquement. */
    @Transactional(readOnly = true)
    public byte[] telecharger(Integer idDevis, Integer currentUserId, boolean isAdminOrAgent) {
        DevisTravaux devis = findOrThrow(idDevis);
        if (!isAdminOrAgent && (devis.getCour().getProprietaire() == null
                || !devis.getCour().getProprietaire().getIdUser().equals(currentUserId))) {
            throw new AccessDeniedException("Vous n'avez pas accès à ce devis");
        }
        Document document = documentRepository
                .findFirstByTypeDocumentAndEntiteTypeAndEntiteId(TypeDocument.DEVIS_PIECE_JOINTE, TypeEntiteDocument.DEVIS_TRAVAUX, idDevis)
                .orElseThrow(() -> new ResourceNotFoundException("piece jointe du devis", idDevis));
        return storageService.telecharger(document.getCheminFichier());
    }

    private void verifierModifiable(DevisTravaux devis) {
        if (!STATUTS_MODIFIABLES.contains(devis.getStatut())) {
            throw new IllegalStateException("Ce devis n'attend plus de réponse : son devis PDF ne peut plus être modifié");
        }
    }

    /** Supprime le PDF actuel s'il existe (base puis fichier) ; renvoie vrai s'il y en avait un. */
    private boolean remplacerExistant(Integer idDevis) {
        var existant = documentRepository.findFirstByTypeDocumentAndEntiteTypeAndEntiteId(
                TypeDocument.DEVIS_PIECE_JOINTE, TypeEntiteDocument.DEVIS_TRAVAUX, idDevis);
        if (existant.isEmpty()) return false;
        documentRepository.delete(existant.get());
        documentRepository.flush();
        supprimerFichier(existant.get().getCheminFichier());
        return true;
    }

    private void supprimerFichier(String cle) {
        try {
            storageService.supprimer(cle);
        } catch (RuntimeException e) {
            log.warn("Fichier de devis non supprimé du stockage ({}) : {}", cle, e.getMessage());
        }
    }

    private DevisTravaux findOrThrow(Integer id) {
        return devisRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("devis", id));
    }
}
