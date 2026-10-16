package com.immobilier.gestionImmobiliere.utils;

import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

/**
 * Filtre « par ville » commun aux listes agent : une ville precise (idVille), ou « Autres villes » (sansVille) =
 * lignes sans ville repertoriee. Sans parametre, aucun filtre. Si les deux sont fournis, idVille l'emporte.
 * Le chemin donne les relations a suivre depuis l'entite jusqu'a la ville (ex. "maison", "cour", "secteur", "ville") ;
 * jointures gauches pour ne pas perdre les lignes dont une relation est vide.
 */
public final class VilleSpecification {

    private VilleSpecification() {
    }

    public static <T> Specification<T> parVille(Integer idVille, Boolean sansVille, String... chemin) {
        return (root, query, cb) -> {
            boolean autres = Boolean.TRUE.equals(sansVille);
            if (idVille == null && !autres) {
                return cb.conjunction();
            }
            From<?, ?> courant = root;
            for (String relation : chemin) {
                courant = courant.join(relation, JoinType.LEFT);
            }
            return idVille != null ? cb.equal(courant.get("idVille"), idVille) : cb.isNull(courant.get("idVille"));
        };
    }
}
