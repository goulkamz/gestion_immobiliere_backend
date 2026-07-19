package com.immobilier.gestionImmobiliere.modules.journal.audit;

import jakarta.persistence.Entity;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.proxy.HibernateProxy;

import org.springframework.stereotype.Component;

import java.lang.reflect.Array;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.UUID;

@Component
public class AuditValueNormalizer {



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

            return proxy.getHibernateLazyInitializer()
                    .getPersistentClass()
                    .getSimpleName()
                    + "#" + id;
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

            return valeur.getClass().getSimpleName();
        }

        // ----------------------------
        // Objet complexe
        // ----------------------------
        //return String.valueOf(valeur);
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
}
