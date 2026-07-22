package com.immobilier.gestionImmobiliere.modules.medias.apis;

import com.immobilier.gestionImmobiliere.modules.medias.dto.requests.ReorderMediaDTO;
import com.immobilier.gestionImmobiliere.modules.medias.dto.requests.UploadMediaDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

// Gestion des fichiers, réservée aux agents/admins
@RequestMapping("/api/medias")
@PreAuthorize("hasAnyRole('AGENT','ADMIN')")
public interface MediaAdminAPI {

    @PostMapping(consumes = "multipart/form-data")
    ResponseEntity<?> upload(@Valid @ModelAttribute UploadMediaDTO dto);

    @DeleteMapping("/{id}")
    ResponseEntity<?> delete(@PathVariable Integer id);

    @PatchMapping("/{id}/principal")
    ResponseEntity<?> setPrincipal(@PathVariable Integer id);

    @PatchMapping("/reorder")
    ResponseEntity<?> reorder(@Valid @RequestBody ReorderMediaDTO dto);
}