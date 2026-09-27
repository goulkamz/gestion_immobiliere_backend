package com.immobilier.gestionImmobiliere.modules.documents.controllers;

import com.immobilier.gestionImmobiliere.modules.documents.apis.ContratMandatDocumentAPI;
import com.immobilier.gestionImmobiliere.modules.documents.services.ContratMandatDocumentService;
import com.immobilier.gestionImmobiliere.modules.user.jwtService.UserDetailsImpl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ContratMandatDocumentController implements ContratMandatDocumentAPI {

    private final ContratMandatDocumentService contratMandatDocumentService;

    public ContratMandatDocumentController(ContratMandatDocumentService contratMandatDocumentService) {
        this.contratMandatDocumentService = contratMandatDocumentService;
    }

    @Override
    public ResponseEntity<byte[]> genererOuRecuperer(Integer idMandat, UserDetailsImpl currentUser) {
        byte[] pdf = contratMandatDocumentService.genererOuRecuperer(idMandat, currentUser.getIdUser());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment().filename("contrat-mandat.pdf").build());

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}