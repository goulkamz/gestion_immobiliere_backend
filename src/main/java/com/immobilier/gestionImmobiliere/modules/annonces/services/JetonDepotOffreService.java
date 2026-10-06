package com.immobilier.gestionImmobiliere.modules.annonces.services;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Jeton à usage limité dans le temps, remis au proposant à la création de son offre : seul
 * lui peut y rattacher des photos. Seul le hash SHA-256 est conservé (Redis, avec TTL) ;
 * rien en base, donc pas de migration.
 */
@Service
public class JetonDepotOffreService {

    public static final Duration VALIDITE = Duration.ofHours(1);

    private static final String PREFIXE = "offre-depot-jeton:";

    private final SecureRandom random = new SecureRandom();
    private final StringRedisTemplate redis;

    public JetonDepotOffreService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public String generer(Integer idOffre) {
        byte[] octets = new byte[32];
        random.nextBytes(octets);
        String jeton = Base64.getUrlEncoder().withoutPadding().encodeToString(octets);
        redis.opsForValue().set(PREFIXE + idOffre, hash(jeton), VALIDITE);
        return jeton;
    }

    public boolean estValide(Integer idOffre, String jeton) {
        if (jeton == null || jeton.isBlank()) return false;
        String attendu = redis.opsForValue().get(PREFIXE + idOffre);
        if (attendu == null) return false;
        return MessageDigest.isEqual(attendu.getBytes(StandardCharsets.UTF_8), hash(jeton).getBytes(StandardCharsets.UTF_8));
    }

    private static String hash(String jeton) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(jeton.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
