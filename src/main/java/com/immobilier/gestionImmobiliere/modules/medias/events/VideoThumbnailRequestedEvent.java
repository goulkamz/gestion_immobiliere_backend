package com.immobilier.gestionImmobiliere.modules.medias.events;

public record VideoThumbnailRequestedEvent(
        Integer idMedia,
        byte[] videoBytes,
        String extension,
        String bucket,
        String sousDossier
) {}