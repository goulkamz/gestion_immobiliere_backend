package com.immobilier.gestionImmobiliere.modules.documents.controllers;

import com.immobilier.gestionImmobiliere.modules.documents.apis.ContratLocationDocumentAPI;
import com.immobilier.gestionImmobiliere.modules.documents.services.ContratLocationDocumentService;
import com.immobilier.gestionImmobiliere.modules.user.jwtService.UserDetailsImpl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ContratLocationDocumentController implements ContratLocationDocumentAPI {

    private final ContratLocationDocumentService contratLocationDocumentService;

    public ContratLocationDocumentController(ContratLocationDocumentService contratLocationDocumentService) {
        this.contratLocationDocumentService = contratLocationDocumentService;
    }

    @Override
    public ResponseEntity<byte[]> genererOuRecuperer(Integer idContrat, UserDetailsImpl currentUser) {
        byte[] pdf = contratLocationDocumentService.genererOuRecuperer(idContrat, currentUser.getIdUser());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment().filename("contrat-location.pdf").build());

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}