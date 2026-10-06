package com.immobilier.gestionImmobiliere.modules.annonces.apis;

import com.immobilier.gestionImmobiliere.modules.annonces.dto.requests.CreateOffreDTO;
import com.immobilier.gestionImmobiliere.modules.medias.dto.requests.UploadPhotosOffreDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/public/offres")
public interface OffrePublicAPI {
    @PostMapping
    ResponseEntity<?> create(@Valid @RequestBody CreateOffreDTO dto, HttpServletRequest request);

    // Photos du proposant : images uniquement, autorisees par le jeton renvoye a la creation
    @PostMapping(value = "/{id}/medias", consumes = "multipart/form-data")
    ResponseEntity<?> deposerPhotos(@PathVariable Integer id,
                                    @RequestHeader(value = "X-Upload-Token", required = false) String jeton,
                                    @Valid @ModelAttribute UploadPhotosOffreDTO dto,
                                    HttpServletRequest request);
}