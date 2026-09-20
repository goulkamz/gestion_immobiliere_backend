package com.immobilier.gestionImmobiliere.modules.documents.controllers;

import com.immobilier.gestionImmobiliere.modules.documents.apis.RapportMensuelDocumentAPI;
import com.immobilier.gestionImmobiliere.modules.documents.services.RapportMensuelDocumentService;
import com.immobilier.gestionImmobiliere.modules.user.jwtService.UserDetailsImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
public class RapportMensuelDocumentController implements RapportMensuelDocumentAPI {

    private final RapportMensuelDocumentService rapportMensuelDocumentService;

    public RapportMensuelDocumentController(RapportMensuelDocumentService rapportMensuelDocumentService) {
        this.rapportMensuelDocumentService = rapportMensuelDocumentService;
    }


    @Override
    public ResponseEntity<?> genererOuRecuperer(LocalDate periode, UserDetailsImpl currentUser) {
        return rapportMensuelDocumentService.genererOuRecuperer(periode, currentUser.getIdUser());
    }
}