package com.immobilier.gestionImmobiliere.modules.medias.controllers;

import com.immobilier.gestionImmobiliere.donnees.medias.model.TypeEntiteMedia;
import com.immobilier.gestionImmobiliere.modules.medias.apis.MediaPublicAPI;
import com.immobilier.gestionImmobiliere.modules.medias.services.MediaService;
import org.springframework.http.ResponseEntity;

public class MediaPublicController implements MediaPublicAPI {

    private final MediaService mediaService;

    public MediaPublicController(MediaService mediaService) {
        this.mediaService = mediaService;
    }


    @Override // public — cohérent avec le catalogue de biens en lecture ouverte
    public ResponseEntity<?> getByEntite(TypeEntiteMedia entiteType, Integer entiteId) {
        return mediaService.getByEntite(entiteType, entiteId);
    }

}
