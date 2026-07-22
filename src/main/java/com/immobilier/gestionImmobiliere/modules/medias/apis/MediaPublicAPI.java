package com.immobilier.gestionImmobiliere.modules.medias.apis;

import com.immobilier.gestionImmobiliere.donnees.medias.model.TypeEntiteMedia;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// Consultation des photos d'annonces/maisons, sans compte
@RequestMapping("/api/public/medias")
public interface MediaPublicAPI {

    // L'implémentation doit rejeter/filtrer toute entiteType hors {ANNONCE, MAISON}
    @GetMapping
    ResponseEntity<?> getByEntite(@RequestParam TypeEntiteMedia entiteType, @RequestParam Integer entiteId);
}