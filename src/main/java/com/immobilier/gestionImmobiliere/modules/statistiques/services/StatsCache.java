package com.immobilier.gestionImmobiliere.modules.statistiques.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Cache mémoire à durée de vie courte pour les statistiques du tableau de bord admin.
 * Une ouverture du tableau déclenche une vingtaine de requêtes d'agrégation ; en garder le résultat
 * quelques dizaines de secondes suffit à absorber les ouvertures et rafraîchissements répétés.
 * Propre à chaque instance du backend. Durée réglable par {@code app.stats.cache-ttl-secondes} (0 = désactivé).
 */
@Component
public class StatsCache {

    private static final int TAILLE_MAX_AVANT_NETTOYAGE = 200;

    private record Entree(Object valeur, long expireA) {}

    private final ConcurrentHashMap<String, Entree> entrees = new ConcurrentHashMap<>();
    private final long dureeMs;

    public StatsCache(@Value("${app.stats.cache-ttl-secondes:60}") long ttlSecondes) {
        this.dureeMs = Math.max(0, ttlSecondes) * 1000;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String cle, Supplier<T> calcul) {
        if (dureeMs == 0) return calcul.get();

        long maintenant = System.currentTimeMillis();
        Entree existante = entrees.get(cle);
        if (existante != null && existante.expireA() > maintenant) {
            return (T) existante.valeur();
        }

        T valeur = calcul.get();
        if (entrees.size() >= TAILLE_MAX_AVANT_NETTOYAGE) {
            entrees.values().removeIf(e -> e.expireA() <= maintenant);
        }
        entrees.put(cle, new Entree(valeur, maintenant + dureeMs));
        return valeur;
    }

    /** Oublie tout : le prochain appel recalcule (bouton « Actualiser » du tableau de bord). */
    public void vider() {
        entrees.clear();
    }
}
