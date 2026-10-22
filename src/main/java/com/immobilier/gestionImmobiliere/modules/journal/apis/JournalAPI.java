package com.immobilier.gestionImmobiliere.modules.journal.apis;

import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RequestMapping("/api/journaux")
public interface JournalAPI {

    // Dates inclusives : dateFin couvre toute la journée indiquée
    @GetMapping
    ResponseEntity<?> getAll(@RequestParam(required = false) Integer idUser,
                             @RequestParam(required = false) String action,
                             @RequestParam(required = false) String entite,
                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
                             Pageable pageable);

    // Types d'éléments présents dans le journal, pour alimenter le filtre
    @GetMapping("/entites")
    ResponseEntity<?> getEntites();
}
