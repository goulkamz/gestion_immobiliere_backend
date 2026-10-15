package com.immobilier.gestionImmobiliere.modules.user.jwt;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RateLimitService {

    private static final int MAX_TENTATIVES = 5;
    private static final Duration FENETRE = Duration.ofMinutes(1);

    private final StringRedisTemplate stringRedisTemplate;

    public RateLimitService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * Compteur glissant par clé (IP ou userId). Retourne true si la limite est dépassée.
     */
    public boolean estLimiteDepassee(String cle) {
        return estLimiteDepassee(cle, MAX_TENTATIVES, FENETRE);
    }

    /** Variante avec quota et fenêtre propres (ex. dépôt public : quelques envois par heure). */
    public boolean estLimiteDepassee(String cle, int maxTentatives, Duration fenetre) {
        String redisKey = "ratelimit:" + cle;
        Long tentatives = stringRedisTemplate.opsForValue().increment(redisKey);

        if (tentatives != null && tentatives == 1) {
            stringRedisTemplate.expire(redisKey, fenetre);
        }

        return tentatives != null && tentatives > maxTentatives;
    }

    /** Lecture seule : true si au moins {@code max} échecs sont déjà enregistrés pour cette clé. */
    public boolean estBloque(String cle, int max) {
        String valeur = stringRedisTemplate.opsForValue().get("ratelimit:" + cle);
        return valeur != null && Long.parseLong(valeur) >= max;
    }

    /** Enregistre un échec (ex. mot de passe erroné) ; la fenêtre démarre au premier échec. */
    public void enregistrerEchec(String cle, Duration fenetre) {
        String redisKey = "ratelimit:" + cle;
        Long echecs = stringRedisTemplate.opsForValue().increment(redisKey);
        if (echecs != null && echecs == 1) {
            stringRedisTemplate.expire(redisKey, fenetre);
        }
    }

    public void reinitialiser(String cle) {
        stringRedisTemplate.delete("ratelimit:" + cle);
    }
}