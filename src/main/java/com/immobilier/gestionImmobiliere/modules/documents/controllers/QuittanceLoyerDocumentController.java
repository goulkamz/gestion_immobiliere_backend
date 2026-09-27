package com.immobilier.gestionImmobiliere.modules.documents.controllers;

import com.immobilier.gestionImmobiliere.modules.documents.apis.QuittanceLoyerDocumentAPI;
import com.immobilier.gestionImmobiliere.modules.documents.services.QuittanceLoyerDocumentService;
import com.immobilier.gestionImmobiliere.modules.user.jwtService.UserDetailsImpl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class QuittanceLoyerDocumentController implements QuittanceLoyerDocumentAPI {

    private final QuittanceLoyerDocumentService quittanceLoyerDocumentService;

    public QuittanceLoyerDocumentController(QuittanceLoyerDocumentService quittanceLoyerDocumentService) {
        this.quittanceLoyerDocumentService = quittanceLoyerDocumentService;
    }

    @Override
    public ResponseEntity<byte[]> genererOuRecuperer(Integer idEcheance, UserDetailsImpl currentUser) {
        byte[] pdf = quittanceLoyerDocumentService.genererOuRecuperer(idEcheance, currentUser.getIdUser());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment().filename("quittance-loyer.pdf").build());

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}