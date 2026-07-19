package com.immobilier.gestionImmobiliere.modules.user.jwt;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

import java.security.Key;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class JwtUtils {

//    À améliorer pour production
//    Remplacer ConcurrentHashMap par Redis (persistance, partagé entre instances)
//
//    Ajouter rate limiting sur les tentatives de refresh
//
//    Implémenter blacklist d'IP en cas de détection de vol
//
//    Ajouter WebSocket pour déconnecter l'utilisateur en temps réel

    @Value("${jwt.secret.access}")
    private String accessSecret;

    @Value("${jwt.secret.refresh}")
    private String refreshSecret;

    @Getter
    @Value("${jwt.access.expiration:900000}")
    private long accessExpiration;

    @Value("${jwt.refresh.expiration:604800000}")
    private long refreshExpiration;

    @Value("${jwt.cookie.access.name:access_token}")
    private String accessCookieName;

    @Value("${jwt.cookie.refresh.name:refresh_token}")
    private String refreshCookieName;

    @Value("${jwt.cookie.secure:true}")
    private boolean cookieSecure;

    @Value("${jwt.cookie.same-site:Strict}")
    private String cookieSameSite;

    @Value("${jwt.redis.secure:true}")
    private boolean useRedis;

    private final RefreshTokenRedisService refreshTokenRedisService;
    private final FingerPrintService fingerprintService;
    private final PasswordEncoder encoder;

    public JwtUtils(RefreshTokenRedisService refreshTokenRedisService, FingerPrintService fingerprintService, PasswordEncoder encoder) {
        this.refreshTokenRedisService = refreshTokenRedisService;
        this.fingerprintService = fingerprintService;
        this.encoder = encoder;
    }



    // Stockage temporaire des refresh tokens valides (en production, utilisez Redis)
    private final Map<String, RefreshTokenRedisService.RefreshTokenData> fallbackStore = new ConcurrentHashMap<>();
    // ============================================================
    // GÉNÉRATION DES TOKENS
    // ============================================================

    /**
     * Génère un access token et l'ajoute aux cookies
     */
    public String generateAccessToken(String username,
                                      HttpServletRequest request,
                                      HttpServletResponse response) {
        String fingerprint = encoder.encode(fingerprintService.generateFingerprint(request, response));
        String tokenId = UUID.randomUUID().toString();

        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "access");
        claims.put("tokenId", tokenId);
        claims.put("fingerprint", fingerprint);
        //claims.put("roles", roles != null ? roles : Collections.emptyList());

        String accessToken = Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + accessExpiration))
                .signWith(getAccessKey(), SignatureAlgorithm.HS256)
                .compact();

        // Ajouter le cookie à la réponse
        ResponseCookie cookie = buildAccessCookie(accessToken);
        response.addHeader("Set-Cookie", cookie.toString());

        log.debug("Access token généré pour l'utilisateur: {}", username);
        return accessToken;
    }

    /**
     * Génère un refresh token et l'ajoute aux cookies
     */
    public String generateRefreshTokenCookie(String username,
                                                     HttpServletRequest request,
                                                     HttpServletResponse response) {
        String refreshTokenId = UUID.randomUUID().toString();
        String fingerprint = encoder.encode(fingerprintService.generateFingerprint(request, response));

        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "refresh");
        claims.put("tokenId", refreshTokenId);
        claims.put("fingerprint", fingerprint);

        String refreshToken = Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + refreshExpiration))
                .signWith(getRefreshKey(), SignatureAlgorithm.HS256)
                .compact();

        // Stocker pour vérification et rotation
        RefreshTokenRedisService.RefreshTokenData tokenData = RefreshTokenRedisService.RefreshTokenData.builder()
                .username(username)
                .fingerprint(fingerprint)
                .expiration(new Date(System.currentTimeMillis() + refreshExpiration))
                .used(false)
                .createdAt(new Date())
                .build();
        try {
            if (useRedis) {
                refreshTokenRedisService.save(refreshTokenId, tokenData);
            } else {
                fallbackStore.put(refreshTokenId, tokenData);
            }
        } catch (Exception e) {
            log.error("Redis indisponible, passage en fallback mémoire", e);
            useRedis = false;
            fallbackStore.put(refreshTokenId, tokenData);
        }

        ResponseCookie cookie = buildRefreshCookie(refreshToken);
        response.addHeader("Set-Cookie", cookie.toString());

        log.debug("Refresh token généré pour l'utilisateur: {}", username);
        return refreshToken;
    }


    // ============================================================
    // CONSTRUCTION DES COOKIES
    // ============================================================

    private ResponseCookie buildAccessCookie(String token) {
        return ResponseCookie.from(accessCookieName, token)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path("/")
                .maxAge(accessExpiration / 1000)
                .build();
    }

    private ResponseCookie buildRefreshCookie(String token) {
        return ResponseCookie.from(refreshCookieName, token)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path("/api/auth/refresh-token")  // Limiter le chemin pour plus de sécurité
                .maxAge(refreshExpiration / 1000)
                .build();
    }
    // 🔹 EXTRACTION DES TOKENS (Web + Mobile)
    public String extractAccessToken(HttpServletRequest request) {
        // Priorité au header Authorization (mobile, API)
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            log.debug("Access token extrait du header Authorization");
            return bearerToken.substring(7);
        }

        // Fallback sur le cookie (web)
        Cookie cookie = WebUtils.getCookie(request, accessCookieName);
        if (cookie != null) {
            log.debug("Access token extrait du cookie");
            return cookie.getValue();
        }

        log.debug("Aucun access token trouvé");
        return null;
    }

    public String extractRefreshToken(HttpServletRequest request) {
        Cookie cookie = WebUtils.getCookie(request, refreshCookieName);
        if (cookie == null) {
            log.debug("Aucun refresh token trouvé dans les cookies");
            return null;
        }

        log.debug("Refresh token extrait du cookie");
        return cookie.getValue();
    }

    // 🔹 VALIDATION DES TOKENS
    public boolean validateAccessToken(String token) {
        if (token == null) {
            return false;
        }

        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getAccessKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            // Vérifier le type
            if (!"access".equals(claims.get("type"))) {
                log.warn("Token n'est pas un access token");
                return false;
            }

            // Vérifier l'expiration
            if (claims.getExpiration().before(new Date())) {
                log.warn("Access token expiré");
                return false;
            }

            return true;

        } catch (ExpiredJwtException e) {
            log.warn("Access token expiré: {}", e.getMessage());
            return false;
        } catch (SignatureException e) {
            log.error("Signature access token invalide: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Validation access token échouée: {}", e.getMessage());
            return false;
        }
    }

    public boolean validateRefreshToken(String token, HttpServletRequest request, HttpServletResponse response) {
        if (token == null) {
            return false;
        }

        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getRefreshKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            // Vérifier le type
            if (!"refresh".equals(claims.get("type"))) {
                log.warn("Token n'est pas un refresh token");
                return false;
            }

            // Vérifier l'expiration
            if (claims.getExpiration().before(new Date())) {
                log.warn("Refresh token expiré");
                return false;
            }

            // Vérifier le fingerprint (anti-vol)
            String currentFingerprint = encoder.encode(fingerprintService.generateFingerprint(request, response));
            String tokenFingerprint = (String) claims.get("fingerprint");

            if (!fingerprintService.fingerprintsMatch(tokenFingerprint, currentFingerprint)) {
                return false;
            }


            // Vérifier dans le store
            String tokenId = (String) claims.get("tokenId");
            RefreshTokenRedisService.RefreshTokenData storedToken;

            if (useRedis) {
                storedToken = refreshTokenRedisService.findById(tokenId);
            } else {
                storedToken = fallbackStore.get(tokenId);
            }

            if (storedToken == null) {
                log.warn("Refresh token non trouvé dans le store");
                return false;
            }

            if (storedToken.isUsed()) {
                log.warn("Refresh token déjà utilisé - possible attack");
                return false;
            }

            if (storedToken.getExpiration().before(new Date())) {
                log.warn("Refresh token expiré (store)");
                return false;
            }

            return true;

        } catch (ExpiredJwtException e) {
            log.warn("Refresh token expiré: {}", e.getMessage());
            return false;
        } catch (SignatureException e) {
            log.error("Signature refresh token invalide: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Validation refresh token échouée: {}", e.getMessage());
            return false;
        }
    }
    // 🔹 ROTATION DU REFRESH TOKEN (sécurité maximale)
    public RefreshResult refreshTokens(String oldRefreshToken,
                                       HttpServletRequest request,
                                       HttpServletResponse response) {

        if (!validateRefreshToken(oldRefreshToken, request, response)) {
            throw new SecurityException("Refresh token invalide");
        }

        Claims claims = extractRefreshClaims(oldRefreshToken);
        String username = claims.getSubject();
        String oldTokenId = (String) claims.get("tokenId");

        // Récupérer et marquer l'ancien token comme utilisé
        RefreshTokenRedisService.RefreshTokenData oldToken;
        if (useRedis) {
            oldToken = refreshTokenRedisService.findById(oldTokenId);
        } else {
            oldToken = fallbackStore.get(oldTokenId);
        }
        if (oldToken == null) {
            throw new SecurityException("Refresh token non trouvé");
        }

        // Marquer comme utilisé
        if (useRedis) {
            refreshTokenRedisService.markAsUsed(oldTokenId);
        } else {
            oldToken.setUsed(true);
            fallbackStore.put(oldTokenId, oldToken);
        }

        // Générer nouveaux tokens

        String newAccessToken = generateAccessToken(username, request, response);
        String newRefreshToken = generateRefreshTokenCookie(username,  request, response);

        log.info("Rotation des tokens effectuée pour l'utilisateur: {}", username);

        return RefreshResult.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();
    }


    // 🔹 NETTOYAGE REFRESH TOKEN (logout)
    public ResponseCookie revokeRefreshToken(String refreshToken, HttpServletResponse response) {
        if (refreshToken != null) {
            try {
                Claims claims = extractRefreshClaims(refreshToken);
                String tokenId = (String) claims.get("tokenId");
                if (useRedis) {
                    refreshTokenRedisService.delete(tokenId);
                } else {
                    fallbackStore.remove(tokenId);
                }

                log.info("Refresh token révoqué pour l'utilisateur: {}", tokenId);

            } catch (Exception e) {
                log.warn("Token déjà invalide lors du logout: {}", e.getMessage());
            }
        }

        // Supprimer le cookie
        ResponseCookie clearCookie = ResponseCookie.from(refreshCookieName, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path("/api/auth/refresh-token")
                .maxAge(0)
                .build();

        response.addHeader("Set-Cookie", clearCookie.toString());

        // Supprimer aussi le cookie access token
        ResponseCookie clearAccessCookie = ResponseCookie.from(accessCookieName, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path("/")
                .maxAge(0)
                .build();

        response.addHeader("Set-Cookie", clearAccessCookie.toString());

        return clearCookie;
    }

    /**
     * Révoque tous les refresh tokens d'un utilisateur (déconnexion globale)
     */
    public int revokeAllUserTokens(String username) {
        int count ;
        if (useRedis) {
            count = refreshTokenRedisService.revokeAllUserTokens(username);
        } else {
            // Fallback: parcourir la map (inefficace, à éviter)
            count = 0;
            Iterator<Map.Entry<String, RefreshTokenRedisService.RefreshTokenData>> iter = fallbackStore.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry<String, RefreshTokenRedisService.RefreshTokenData> entry = iter.next();
                if (entry.getValue().getUsername().equals(username)) {
                    iter.remove();
                    count++;
                }
            }
        }

        log.info("{} tokens révoqués pour user: {}", count, username);
        return count;
    }

    // ============================================================
    // MÉTHODES UTILITAIRES
    // ============================================================



    /**
     * Vérifie si un token peut être rafraîchi
     */

    public boolean isRedisAvailable() {
        return useRedis;
    }

    public long getActiveTokensCount() {
        if (useRedis) {
            return refreshTokenRedisService.getActiveTokensCount();
        }
        return fallbackStore.size();
    }

    // ============================================================
    // EXTRACTION DES CLAIMS (PRIVÉES)
    // ============================================================


    private Claims extractRefreshClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getRefreshKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    @Data
    @Builder
    public static class RefreshResult {
        private String accessToken;
        private String refreshToken;

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("accessToken", accessToken);
            map.put("refreshToken", refreshToken);
            return map;
        }
    }

    public String getUserNameFromJwtToken(String token) {
        return Jwts.parserBuilder().setSigningKey(getAccessKey()).build()
                .parseClaimsJws(token).getBody().getSubject();
    }

    private Key getAccessKey() {
        byte[] keyBytes = Decoders.BASE64.decode(accessSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }


    private Key getRefreshKey() {
        byte[] keyBytes = Decoders.BASE64.decode(refreshSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }


    public void cleanupExpiredTokens() {
        Date now = new Date();
        int before = fallbackStore.size();

        // Utiliser un filtre plus agressif
        fallbackStore.entrySet().removeIf(entry -> {
            RefreshTokenRedisService.RefreshTokenData data = entry.getValue();

            // Supprimer SI :
            // Token expiré
            // Token utilisé
            // Token sans fingerprint valide
            return data.getExpiration().before(now) || data.isUsed() || data.getFingerprint() == null;
        });

        int after = fallbackStore.size();
        log.info("Nettoyage: {} tokens supprimés, {} restants", before - after, after);
    }

}
