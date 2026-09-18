package com.immobilier.gestionImmobiliere.modules.documents.controllers;

import com.immobilier.gestionImmobiliere.modules.documents.apis.EtatDesLieuxDocumentAPI;
import com.immobilier.gestionImmobiliere.modules.documents.services.EtatDesLieuxDocumentService;
import com.immobilier.gestionImmobiliere.modules.user.jwtService.UserDetailsImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EtatDesLieuxDocumentController implements EtatDesLieuxDocumentAPI {

    private final EtatDesLieuxDocumentService etatDesLieuxDocumentService;

    public EtatDesLieuxDocumentController(EtatDesLieuxDocumentService etatDesLieuxDocumentService) {
        this.etatDesLieuxDocumentService = etatDesLieuxDocumentService;
    }

    @Override
    public ResponseEntity<?> genererEntree(Integer idContrat, UserDetailsImpl currentUser) {
        return etatDesLieuxDocumentService.genererEntree(idContrat, currentUser.getIdUser());
    }

    @Override
    public ResponseEntity<?> genererSortie(Integer idContrat, UserDetailsImpl currentUser) {
        return etatDesLieuxDocumentService.genererSortie(idContrat, currentUser.getIdUser());
    }
}