package com.immobilier.gestionImmobiliere.modules.medias.apis;

import com.immobilier.gestionImmobiliere.donnees.medias.model.TypeEntiteMedia;
import com.immobilier.gestionImmobiliere.modules.medias.dto.requests.ReorderMediaDTO;
import com.immobilier.gestionImmobiliere.modules.medias.dto.requests.UploadMediaDTO;
import com.immobilier.gestionImmobiliere.modules.medias.dto.requests.UploadMultipleMediaDTO;
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

    @PostMapping(value = "/multiple", consumes = "multipart/form-data")
    ResponseEntity<?> uploadMultiple(@ModelAttribute UploadMultipleMediaDTO dto);


    // Liste complete (prives compris, ex. OFFRE) : la consultation publique les refuse
    @GetMapping
    ResponseEntity<?> getByEntite(@RequestParam TypeEntiteMedia entiteType, @RequestParam Integer entiteId);

    // Fichier / miniature d'un media, y compris prive (binaire, jamais mis en cache partage)
    @GetMapping("/{id}/fichier")
    ResponseEntity<?> fichier(@PathVariable Integer id);

    @GetMapping("/{id}/miniature")
    ResponseEntity<?> miniature(@PathVariable Integer id);

    @DeleteMapping("/{id}")
    ResponseEntity<?> delete(@PathVariable Integer id);

    @PatchMapping("/{id}/principal")
    ResponseEntity<?> setPrincipal(@PathVariable Integer id);

    @PatchMapping("/reorder")
    ResponseEntity<?> reorder(@Valid @RequestBody ReorderMediaDTO dto);
}