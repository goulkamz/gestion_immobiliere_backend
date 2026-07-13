package com.immobilier.gestionImmobiliere.modules.medias.apis;

import com.immobilier.gestionImmobiliere.donnees.medias.model.TypeEntiteMedia;
import com.immobilier.gestionImmobiliere.modules.medias.dto.requests.ReorderMediaDTO;
import com.immobilier.gestionImmobiliere.modules.medias.dto.requests.UploadMediaDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/medias")
public interface MediaAPI {

    @GetMapping
    ResponseEntity<?> getByEntite(@RequestParam TypeEntiteMedia entiteType, @RequestParam Integer entiteId);

    @PostMapping(consumes = "multipart/form-data")
    ResponseEntity<?> upload(@Valid @ModelAttribute UploadMediaDTO dto);

    @DeleteMapping("/{id}")
    ResponseEntity<?> delete(@PathVariable Integer id);

    @PatchMapping("/{id}/principal")
    ResponseEntity<?> setPrincipal(@PathVariable Integer id);

    @PatchMapping("/reorder")
    ResponseEntity<?> reorder(@Valid @RequestBody ReorderMediaDTO dto);
}