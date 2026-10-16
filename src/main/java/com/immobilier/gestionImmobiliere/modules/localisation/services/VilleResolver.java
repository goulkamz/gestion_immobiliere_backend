package com.immobilier.gestionImmobiliere.modules.localisation.services;

import com.immobilier.gestionImmobiliere.donnees.localisation.model.Ville;
import com.immobilier.gestionImmobiliere.donnees.localisation.repository.VilleRepository;
import com.immobilier.gestionImmobiliere.exceptions.ResourceNotFoundException;
import org.springframework.stereotype.Component;

/**
 * Ville d'une annonce, d'une demande ou d'une offre : une ville repertoriee (id_ville) OU, a defaut,
 * un nom saisi librement (ville_autre) quand la ville n'est pas encore dans la base. Un visiteur ne cree
 * jamais de ville : seul l'admin les ajoute (page Localisation).
 */
@Component
public class VilleResolver {

    private static final int LONGUEUR_MAX_VILLE_AUTRE = 100;

    private final VilleRepository villeRepository;

    public VilleResolver(VilleRepository villeRepository) {
        this.villeRepository = villeRepository;
    }

    /** La ville repertoriee correspondant a l'identifiant, ou null si aucun identifiant n'est fourni (404 si inconnu). */
    public Ville resoudre(Integer idVille) {
        if (idVille == null) return null;
        return villeRepository.findById(idVille).orElseThrow(() -> new ResourceNotFoundException("ville", idVille));
    }

    /** Nom libre nettoye (espaces, longueur) ; null s'il est vide ou si une ville repertoriee est choisie. */
    public String villeAutre(Ville ville, String saisie) {
        if (ville != null || saisie == null || saisie.isBlank()) return null;
        String nettoye = saisie.trim().replaceAll("\\s+", " ");
        return nettoye.length() > LONGUEUR_MAX_VILLE_AUTRE ? nettoye.substring(0, LONGUEUR_MAX_VILLE_AUTRE) : nettoye;
    }
}
