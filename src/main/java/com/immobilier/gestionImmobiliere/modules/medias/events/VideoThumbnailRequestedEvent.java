package com.immobilier.gestionImmobiliere.modules.medias.events;

public record VideoThumbnailRequestedEvent(
        Integer idMedia,
        String bucket,
        String cleVideo,
        String extension,
        String sousDossier
) {}