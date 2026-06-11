package com.immobilier.gestionImmobiliere.modules.user.jwt;

import lombok.Data;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class RefreshTokenRedisService {

    private static final String REFRESH_TOKEN_KEY_PREFIX = "rt:";
    private static final String USER_TOKENS_KEY_PREFIX = "user:rt:";

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Data
    @Builder
    public static class RefreshTokenData {
        private String username;
        private String fingerprint;
        private Date expiration;
        private boolean used;
        private String deviceId;
        private Date createdAt;
    }

    /**
     * Sauvegarde un refresh token dans Redis avec TTL automatique
     */
    public void save(String tokenId, RefreshTokenData data) {
        String key = REFRESH_TOKEN_KEY_PREFIX + tokenId;

        // Calculer le TTL restant
        long ttl = data.getExpiration().getTime() - System.currentTimeMillis();

        if (ttl <= 0) {
            log.warn("Token déjà expiré, non sauvegardé: {}", tokenId);
            return;
        }

        // Sauvegarder le token
        redisTemplate.opsForValue().set(key, data, ttl, TimeUnit.MILLISECONDS);

        // Sauvegarder la référence user -> token (pour révocations massives)
        String userKey = USER_TOKENS_KEY_PREFIX + data.getUsername();
        redisTemplate.opsForSet().add(userKey, tokenId);
        redisTemplate.expire(userKey, ttl, TimeUnit.MILLISECONDS);

        log.debug("Refresh token sauvegardé: {} pour user: {} (TTL: {} ms)", tokenId, data.getUsername(), ttl);
    }

    /**
     * Récupère un refresh token
     */
    public RefreshTokenData findById(String tokenId) {
        String key = REFRESH_TOKEN_KEY_PREFIX + tokenId;
        Object data = redisTemplate.opsForValue().get(key);

        if (data instanceof RefreshTokenData) {
            return (RefreshTokenData) data;
        }

        log.debug("Refresh token non trouvé: {}", tokenId);
        return null;
    }

    /**
     * Marque un token comme utilisé (rotation)
     */
    public boolean markAsUsed(String tokenId) {
        RefreshTokenData data = findById(tokenId);
        if (data == null) {
            return false;
        }

        data.setUsed(true);

        // Mettre à jour avec le même TTL restant
        String key = REFRESH_TOKEN_KEY_PREFIX + tokenId;
        Long ttl = redisTemplate.getExpire(key, TimeUnit.MILLISECONDS);

        if (ttl != null && ttl > 0) {
            redisTemplate.opsForValue().set(key, data, ttl, TimeUnit.MILLISECONDS);
            return true;
        }

        return false;
    }

    /**
     * Supprime un refresh token spécifique
     */
    public void delete(String tokenId) {
        String key = REFRESH_TOKEN_KEY_PREFIX + tokenId;
        RefreshTokenData data = findById(tokenId);

        if (data != null) {
            // Supprimer aussi la référence user -> token
            String userKey = USER_TOKENS_KEY_PREFIX + data.getUsername();
            redisTemplate.opsForSet().remove(userKey, tokenId);
        }

        redisTemplate.delete(key);
        log.debug("Refresh token supprimé: {}", tokenId);
    }

    /**
     * Révoque tous les tokens d'un utilisateur
     */
    public int revokeAllUserTokens(String username) {
        String userKey = USER_TOKENS_KEY_PREFIX + username;
        Set<Object> tokenIds = redisTemplate.opsForSet().members(userKey);

        if (tokenIds == null || tokenIds.isEmpty()) {
            log.debug("Aucun token trouvé pour user: {}", username);
            return 0;
        }

        // Supprimer chaque token
        for (Object tokenIdObj : tokenIds) {
            String tokenId = tokenIdObj.toString();
            String key = REFRESH_TOKEN_KEY_PREFIX + tokenId;
            redisTemplate.delete(key);
        }

        // Supprimer la liste user -> tokens
        redisTemplate.delete(userKey);

        log.info("{} tokens révoqués pour user: {}", tokenIds.size(), username);
        return tokenIds.size();
    }

    /**
     * Révoque un token spécifique par son ID
     */
    public void revokeByTokenId(String tokenId) {
        delete(tokenId);
    }

    /**
     * Vérifie si un token existe et n'est pas utilisé
     */
    public boolean isValidAndNotUsed(String tokenId) {
        RefreshTokenData data = findById(tokenId);
        return data != null && !data.isUsed() && data.getExpiration().after(new Date());
    }

    /**
     * Nettoie les tokens expirés (Redis le fait automatiquement avec TTL)
     * Cette méthode peut être utilisée pour le monitoring
     */
    public long getActiveTokensCount() {
        // Redis ne supporte pas nativement le comptage de toutes les clés avec pattern
        // Pour production, utilisez Redis SCAN ou maintenez un compteur séparé
        Set<String> keys = redisTemplate.keys(REFRESH_TOKEN_KEY_PREFIX + "*");
        return keys != null ? keys.size() : 0;
    }
}
