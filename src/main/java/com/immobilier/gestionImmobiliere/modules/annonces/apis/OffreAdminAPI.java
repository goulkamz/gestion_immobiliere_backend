package com.immobilier.gestionImmobiliere.modules.annonces.apis;

import com.immobilier.gestionImmobiliere.donnees.annonces.model.StatutOffre;
import com.immobilier.gestionImmobiliere.modules.annonces.dto.requests.CreateOffreDTO;
import com.immobilier.gestionImmobiliere.modules.annonces.dto.requests.UpdateStatutOffreDTO;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/offres")
@PreAuthorize("hasAnyRole('AGENT','ADMIN')")
public interface OffreAdminAPI {

    @GetMapping
    ResponseEntity<?> getAll(@RequestParam( required = false) StatutOffre statut, Pageable pageable);

    @GetMapping("/{id}")
    ResponseEntity<?> getById(@PathVariable Integer id);

    @PatchMapping("/{id}/statut")
    ResponseEntity<?> updateStatut(@PathVariable Integer id, @Valid @RequestBody UpdateStatutOffreDTO dto);

}