package com.immobilier.gestionImmobiliere.modules.documents.controllers;

import com.immobilier.gestionImmobiliere.modules.documents.apis.DecompteSortieDocumentAPI;
import com.immobilier.gestionImmobiliere.modules.documents.services.DecompteSortieDocumentService;
import com.immobilier.gestionImmobiliere.modules.user.jwtService.UserDetailsImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DecompteSortieDocumentController implements DecompteSortieDocumentAPI {

    private final DecompteSortieDocumentService decompteSortieDocumentService;

    public DecompteSortieDocumentController(DecompteSortieDocumentService decompteSortieDocumentService) {
        this.decompteSortieDocumentService = decompteSortieDocumentService;
    }

    @Override
    public ResponseEntity<?> genererOuRecuperer(Integer idDecompte, UserDetailsImpl currentUser) {
        return decompteSortieDocumentService.genererOuRecuperer(idDecompte, currentUser.getIdUser());
    }
}