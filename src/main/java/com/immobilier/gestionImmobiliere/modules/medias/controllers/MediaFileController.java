package com.immobilier.gestionImmobiliere.modules.medias.controllers;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
public class MediaFileController {

    @Value("${app.medias.storage-path:/data/medias}")
    private String storagePath;

    @GetMapping("/api/medias/fichier/**")
    public ResponseEntity<Resource> servirFichier(HttpServletRequest request) {
        String cheminComplet = request.getRequestURI().split("/api/medias/fichier/")[1];
        try {
            Path chemin = Paths.get(storagePath, cheminComplet).normalize();

            if (!chemin.startsWith(Paths.get(storagePath).normalize())) {
                return ResponseEntity.badRequest().build();
            }

            Resource resource = new UrlResource(chemin.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok().body(resource);
        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}