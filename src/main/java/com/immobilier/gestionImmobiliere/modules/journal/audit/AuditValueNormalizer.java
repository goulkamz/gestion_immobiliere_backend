package com.immobilier.gestionImmobiliere.modules.journal.audit;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.proxy.HibernateProxy;

import org.springframework.stereotype.Component;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class AuditValueNormalizer {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AuditValueNormalizer.class);


    /** Évite de sérialiser proxys non initialisés et collections lazy (risque LazyInitializationException). */

    public Object normalize(Object valeur) {

        if (valeur == null) {
            return null;
        }

        // ----------------------------
        // Valeurs simples
        // ----------------------------
        if (estTypeSimple(valeur)) {
            return valeur;
        }

        // ----------------------------
        // Enum
        // ----------------------------
        if (valeur instanceof Enum<?> e) {
            return e.name();
        }

        // ----------------------------
        // Proxy Hibernate
        // ----------------------------
        if (valeur instanceof HibernateProxy proxy) {

            Object id = proxy.getHibernateLazyInitializer().getIdentifier();

            return proxy.getHibernateLazyInitializer().getPersistentClass().getSimpleName() + "#" + id;
        }

        // ----------------------------
        // Collections Hibernate
        // ----------------------------
        if (valeur instanceof PersistentCollection collection) {

            if (!collection.wasInitialized()) {
                return "[collection non chargée]";
            }

            Object contenu = collection.getValue();

            if (contenu instanceof Collection<?> vraieCollection) {
                return "[collection : "
                        + vraieCollection.size()
                        + " élément(s)]";
            }

            return "[collection Hibernate]";
        }

        // ----------------------------
        // Collections Java
        // ----------------------------
        if (valeur instanceof Collection<?> collection) {
            return "[collection : " + collection.size() + " élément(s)]";
        }

        // ----------------------------
        // Tableau
        // ----------------------------
        if (valeur.getClass().isArray()) {

            return "[tableau : "
                    + Array.getLength(valeur)
                    + " élément(s)]";
        }

        // ----------------------------
        // Entité JPA
        // ----------------------------
        if (valeur.getClass().isAnnotationPresent(Entity.class)) {
            return valeur.getClass().getSimpleName() + "#" + extraireId(valeur);
        }

        // ----------------------------
        // Objet embarqué (@Embeddable)
        // ----------------------------
        if (valeur.getClass().isAnnotationPresent(Embeddable.class)) {
            return normaliserEmbeddable(valeur);
        }

        // ----------------------------
        // Objet complexe
        // ----------------------------
        log.warn("Type non géré par AuditValueNormalizer, données potentiellement perdues: {}", valeur.getClass().getName());
        return valeur.getClass().getSimpleName();
    }

    private boolean estTypeSimple(Object valeur) {

        return valeur instanceof String
                || valeur instanceof Number
                || valeur instanceof Boolean
                || valeur instanceof Character
                || valeur instanceof Enum<?>
                || valeur instanceof UUID
                || valeur instanceof LocalDate
                || valeur instanceof LocalDateTime
                || valeur instanceof OffsetDateTime
                || valeur instanceof Instant
                || valeur instanceof java.util.Date;
    }

    private Object extraireId(Object entite) {
        try {
            return entite.getClass().getMethod("getId").invoke(entite);
        } catch (Exception e) {
            return "id-inconnu"; // ne doit jamais faire échouer l'audit
        }
    }

    /**
     * Sérialise un @Embeddable champ par champ (récursivement via normalize)
     * plutôt que de perdre son contenu derrière un simple nom de classe.
     * Utile pour les objets valeur comme Adresse, PlageHoraire, Coordonnees...
     */
    private Object normaliserEmbeddable(Object embeddable) {
        Map<String, Object> champs = new LinkedHashMap<>();
        for (Field field : embeddable.getClass().getDeclaredFields()) {
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            field.setAccessible(true);
            try {
                Object valeurChamp = field.get(embeddable);
                champs.put(field.getName(), normalize(valeurChamp)); // récursif
            } catch (IllegalAccessException e) {
                champs.put(field.getName(), "inaccessible");
            }
        }
        return champs;
    }
}
