package com.immobilier.gestionImmobiliere.modules.documents.controllers;

import com.immobilier.gestionImmobiliere.modules.documents.apis.DecompteSortieDocumentAPI;
import com.immobilier.gestionImmobiliere.modules.documents.services.DecompteSortieDocumentService;
import com.immobilier.gestionImmobiliere.modules.user.jwtService.UserDetailsImpl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DecompteSortieDocumentController implements DecompteSortieDocumentAPI {

    private final DecompteSortieDocumentService decompteSortieDocumentService;

    public DecompteSortieDocumentController(DecompteSortieDocumentService decompteSortieDocumentService) {
        this.decompteSortieDocumentService = decompteSortieDocumentService;
    }

    @Override
    public ResponseEntity<byte[]> genererOuRecuperer(Integer idDecompte, UserDetailsImpl currentUser) {
        byte[] pdf = decompteSortieDocumentService.genererOuRecuperer(idDecompte, currentUser.getIdUser());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment().filename("decompte-sortie.pdf").build());

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}