package com.immobilier.gestionImmobiliere.modules.medias.controllers;

import com.immobilier.gestionImmobiliere.donnees.medias.model.TypeEntiteMedia;
import com.immobilier.gestionImmobiliere.modules.medias.apis.MediaAdminAPI;
import com.immobilier.gestionImmobiliere.modules.medias.dto.requests.ReorderMediaDTO;
import com.immobilier.gestionImmobiliere.modules.medias.dto.requests.UploadMediaDTO;
import com.immobilier.gestionImmobiliere.modules.medias.services.MediaService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MediaAdminController implements MediaAdminAPI {

    private final MediaService mediaService;

    public MediaAdminController(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    @Override
    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    public ResponseEntity<?> upload(UploadMediaDTO dto) {
        return mediaService.upload(dto);
    }

    @Override
    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    public ResponseEntity<?> delete(Integer id) {
        return mediaService.delete(id);
    }

    @Override
    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    public ResponseEntity<?> setPrincipal(Integer id) {
        return mediaService.setPrincipal(id);
    }

    @Override
    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    public ResponseEntity<?> reorder(ReorderMediaDTO dto) {
        return mediaService.reorder(dto);
    }
}