package com.immobilier.gestionImmobiliere.modules.annonces.apis;

import com.immobilier.gestionImmobiliere.donnees.annonces.model.StatutDemande;
import com.immobilier.gestionImmobiliere.modules.annonces.dto.requests.CreateDemandeDTO;
import com.immobilier.gestionImmobiliere.modules.annonces.dto.requests.UpdateStatutDemandeDTO;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/demandes")
public interface DemandeAPI {

    @GetMapping
    ResponseEntity<?> getAll(@RequestParam(required = false) StatutDemande statut, Pageable pageable);

    @GetMapping("/{id}")
    ResponseEntity<?> getById(@PathVariable Integer id);

    @PostMapping
    ResponseEntity<?> create(@Valid @RequestBody CreateDemandeDTO dto);

    @PatchMapping("/{id}/statut")
    ResponseEntity<?> updateStatut(@PathVariable Integer id, @Valid @RequestBody UpdateStatutDemandeDTO dto);
}