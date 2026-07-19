package com.immobilier.gestionImmobiliere.modules.user.jwt;

import lombok.Data;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class RefreshTokenRedisService {

    private static final String REFRESH_TOKEN_KEY_PREFIX = "rt:";
    private static final String USER_TOKENS_KEY_PREFIX = "user:rt:";

    private final RedisTemplate<String, RefreshTokenData> refreshTokenRedisTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    public RefreshTokenRedisService(
            @Qualifier("refreshTokenRedisTemplate") RedisTemplate<String, RefreshTokenData> refreshTokenRedisTemplate, StringRedisTemplate stringRedisTemplate
    ) {
        this.refreshTokenRedisTemplate = refreshTokenRedisTemplate;
        this.stringRedisTemplate = stringRedisTemplate;
    }

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

        long ttl = data.getExpiration().getTime() - System.currentTimeMillis();

        if (ttl <= 0) {
            log.warn("Token déjà expiré, non sauvegardé: {}", tokenId);
            return;
        }

        refreshTokenRedisTemplate.opsForValue().set(key, data, ttl, TimeUnit.MILLISECONDS);

        String userKey = USER_TOKENS_KEY_PREFIX + data.getUsername();
        stringRedisTemplate.opsForSet().add(userKey, tokenId);
        stringRedisTemplate.expire(userKey, ttl, TimeUnit.MILLISECONDS);

        log.debug("Refresh token sauvegardé: {} pour user: {} (TTL: {} ms)", tokenId, data.getUsername(), ttl);
    }

    /**
     * Récupère un refresh token
     */
    public RefreshTokenData findById(String tokenId) {
        String key = REFRESH_TOKEN_KEY_PREFIX + tokenId;
        RefreshTokenData data = refreshTokenRedisTemplate.opsForValue().get(key);

        if (data == null) {
            log.debug("Refresh token non trouvé: {}", tokenId);
        }

        return data;
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

        String key = REFRESH_TOKEN_KEY_PREFIX + tokenId;
        Long ttl = refreshTokenRedisTemplate.getExpire(key, TimeUnit.MILLISECONDS);

        if (ttl != null && ttl > 0) {
            refreshTokenRedisTemplate.opsForValue().set(key, data, ttl, TimeUnit.MILLISECONDS);
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
            String userKey = USER_TOKENS_KEY_PREFIX + data.getUsername();
            stringRedisTemplate.opsForSet().remove(userKey, tokenId);
        }

        refreshTokenRedisTemplate.delete(key);
        log.debug("Refresh token supprimé: {}", tokenId);
    }

    /**
     * Révoque tous les tokens d'un utilisateur
     */
    public int revokeAllUserTokens(String username) {
        String userKey = USER_TOKENS_KEY_PREFIX + username;
        Set<String> tokenIds = stringRedisTemplate.opsForSet().members(userKey);

        if (tokenIds == null || tokenIds.isEmpty()) {
            log.debug("Aucun token trouvé pour user: {}", username);
            return 0;
        }

        List<String> keys = tokenIds.stream()
                .map(id -> REFRESH_TOKEN_KEY_PREFIX + id)
                .toList();

        refreshTokenRedisTemplate.delete(keys);
        stringRedisTemplate.delete(userKey);

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
//    public long getActiveTokensCount() {
//        // Redis ne supporte pas nativement le comptage de toutes les clés avec pattern
//        // Pour production, utilisez Redis SCAN ou maintenez un compteur séparé
//        Set<String> keys = redisTemplate.keys(REFRESH_TOKEN_KEY_PREFIX + "*");
//        return keys != null ? keys.size() : 0;
//    }

    public long getActiveTokensCount() {
        long count = 0;
        ScanOptions options = ScanOptions.scanOptions()
                .match(REFRESH_TOKEN_KEY_PREFIX + "*")
                .count(100)
                .build();

        try (Cursor<byte[]> cursor = refreshTokenRedisTemplate.getConnectionFactory()
                .getConnection().scan(options)) {
            while (cursor.hasNext()) {
                cursor.next();
                count++;
            }
        }
        return count;
    }
}
