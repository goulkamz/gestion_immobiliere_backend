package com.immobilier.gestionImmobiliere.utils;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Filtres de liste communs : statut exact (optionnel) + recherche texte insensible à la casse
 * sur un ou plusieurs champs de l'entité. Appliqués côté base pour que la pagination
 * reste cohérente avec les filtres.
 */
public final class RechercheSpecification {

    private RechercheSpecification() {
    }

    public static <T> Specification<T> statutEtTexte(Enum<?> statut, String recherche, String... champs) {
        return (root, query, cb) -> {
            List<Predicate> predicats = new ArrayList<>();
            if (statut != null) {
                predicats.add(cb.equal(root.get("statut"), statut));
            }
            if (recherche != null && !recherche.isBlank() && champs.length > 0) {
                String motif = "%" + recherche.trim().toLowerCase() + "%";
                Predicate[] alternatives = new Predicate[champs.length];
                for (int i = 0; i < champs.length; i++) {
                    alternatives[i] = cb.like(cb.lower(root.get(champs[i]).as(String.class)), motif);
                }
                predicats.add(cb.or(alternatives));
            }
            return cb.and(predicats.toArray(new Predicate[0]));
        };
    }
}
