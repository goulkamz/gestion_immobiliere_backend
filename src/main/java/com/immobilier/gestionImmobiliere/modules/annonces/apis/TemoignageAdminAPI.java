package com.immobilier.gestionImmobiliere.modules.annonces.apis;

import com.immobilier.gestionImmobiliere.modules.annonces.dto.requests.CreateTemoignageDTO;
import com.immobilier.gestionImmobiliere.modules.annonces.dto.requests.UpdateStatutTemoignageDTO;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// Droits precises par methode dans le controller (lecture/creation : agent/admin, statut : admin)
@RequestMapping("/api/admin/temoignages")
public interface TemoignageAdminAPI {

    // Moderation : inclut les temoignages inactifs ; ?flagActif=false = file d'attente de validation
    @GetMapping
    ResponseEntity<?> getAll(@RequestParam(required = false) Boolean flagActif,
                             @PageableDefault(sort = "idTemoignage", direction = Sort.Direction.DESC) Pageable pageable);

    // Saisie par l'agence
    @PostMapping
    ResponseEntity<?> create(@Valid @RequestBody CreateTemoignageDTO dto);

    @PatchMapping("/{id}/statut")
    ResponseEntity<?> updateStatut(@PathVariable Integer id, @Valid @RequestBody UpdateStatutTemoignageDTO dto);
}
