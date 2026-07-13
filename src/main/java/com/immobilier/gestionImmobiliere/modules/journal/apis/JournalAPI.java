package com.immobilier.gestionImmobiliere.modules.journal.apis;

import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RequestMapping("/api/journaux")
public interface JournalAPI {

    @GetMapping
    ResponseEntity<?> getAll(@RequestParam(required = false) Integer idUser,
                             @RequestParam(required = false) String action,
                             @RequestParam(required = false) String entite,
                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDateTime dateDebut,
                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDateTime dateFin,
                             Pageable pageable);
}