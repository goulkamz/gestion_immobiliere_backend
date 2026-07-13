package com.immobilier.gestionImmobiliere.modules.journal.controllers;

import com.immobilier.gestionImmobiliere.modules.journal.apis.JournalAPI;
import com.immobilier.gestionImmobiliere.modules.journal.services.JournalService;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@PreAuthorize("hasRole('ADMIN')")
public class JournalController implements JournalAPI {

    private final JournalService journalService;

    public JournalController(JournalService journalService) {
        this.journalService = journalService;
    }

    @Override
    public ResponseEntity<?> getAll(Integer idUser, String action, String entite,
                                    LocalDateTime dateDebut, LocalDateTime dateFin, Pageable pageable) {
        return journalService.getAll(idUser, action, entite, dateDebut, dateFin, pageable);
    }
}