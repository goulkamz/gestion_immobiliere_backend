package com.immobilier.gestionImmobiliere.modules.documents.controllers;

import com.immobilier.gestionImmobiliere.modules.documents.apis.EtatDesLieuxDocumentAPI;
import com.immobilier.gestionImmobiliere.modules.documents.services.EtatDesLieuxDocumentService;
import com.immobilier.gestionImmobiliere.modules.user.jwtService.UserDetailsImpl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EtatDesLieuxDocumentController implements EtatDesLieuxDocumentAPI {

    private final EtatDesLieuxDocumentService etatDesLieuxDocumentService;

    public EtatDesLieuxDocumentController(EtatDesLieuxDocumentService etatDesLieuxDocumentService) {
        this.etatDesLieuxDocumentService = etatDesLieuxDocumentService;
    }

    @Override
    public ResponseEntity<byte[]> genererEntree(Integer idContrat, UserDetailsImpl currentUser) {
        return pdfResponse(etatDesLieuxDocumentService.genererEntree(idContrat, currentUser.getIdUser()), "etat-des-lieux-entree.pdf");
    }

    @Override
    public ResponseEntity<byte[]> genererSortie(Integer idContrat, UserDetailsImpl currentUser) {
        return pdfResponse(etatDesLieuxDocumentService.genererSortie(idContrat, currentUser.getIdUser()), "etat-des-lieux-sortie.pdf");
    }

    private ResponseEntity<byte[]> pdfResponse(byte[] pdf, String filename) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}