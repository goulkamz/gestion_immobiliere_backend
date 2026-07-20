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
        String redisKey = "ratelimit:" + cle;
        Long tentatives = stringRedisTemplate.opsForValue().increment(redisKey);

        if (tentatives != null && tentatives == 1) {
            stringRedisTemplate.expire(redisKey, FENETRE);
        }

        return tentatives != null && tentatives > MAX_TENTATIVES;
    }
}