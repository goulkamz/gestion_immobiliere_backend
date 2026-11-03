package com.immobilier.gestionImmobiliere.modules.user.deuxfacteurs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Chiffre les secrets TOTP en base (AES-256-GCM). La cle (32 octets en base64) vient de
 * TWOFA_ENCRYPTION_KEY : une fuite de la base seule ne donne donc pas les secrets.
 */
@Service
public class ChiffrementSecretService {

    private static final int TAILLE_IV = 12;
    private static final int TAILLE_TAG_BITS = 128;

    private final String cleBase64;
    private final SecureRandom random = new SecureRandom();

    /**
     * Reçoit la clé de chiffrement (base64) depuis la configuration ; elle n'est contrôlée qu'à l'usage.
     */
    public ChiffrementSecretService(@Value("${app.security.twofa-key:}") String cleBase64) {
        this.cleBase64 = cleBase64;
    }

    /**
     * Chiffre un secret en AES-256-GCM avec un vecteur d'initialisation aléatoire placé devant le résultat,
     * le tout encodé en base64 pour tenir dans une colonne texte.
     */
    public String chiffrer(String clair) {
        SecretKeySpec cle = cle();
        try {
            byte[] iv = new byte[TAILLE_IV];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, cle, new GCMParameterSpec(TAILLE_TAG_BITS, iv));
            byte[] chiffre = cipher.doFinal(clair.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(ByteBuffer.allocate(iv.length + chiffre.length)
                    .put(iv).put(chiffre).array());
        } catch (Exception e) {
            throw new IllegalStateException("Chiffrement du secret impossible", e);
        }
    }

    /**
     * Opération inverse de chiffrer : relit le vecteur d'initialisation, déchiffre et vérifie l'intégrité
     * (GCM refuse toute donnée altérée ou chiffrée avec une autre clé).
     */
    public String dechiffrer(String chiffreBase64) {
        SecretKeySpec cle = cle();
        try {
            byte[] tout = Base64.getDecoder().decode(chiffreBase64);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, cle, new GCMParameterSpec(TAILLE_TAG_BITS, tout, 0, TAILLE_IV));
            return new String(cipher.doFinal(tout, TAILLE_IV, tout.length - TAILLE_IV), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Déchiffrement du secret impossible", e);
        }
    }

    /**
     * Décode et contrôle la clé de configuration (présente, 32 octets) ; sans elle la 2FA est indisponible.
     */
    private SecretKeySpec cle() {
        if (cleBase64 == null || cleBase64.isBlank()) {
            throw new IllegalStateException("TWOFA_ENCRYPTION_KEY n'est pas définie : double authentification indisponible");
        }
        byte[] octets = Base64.getDecoder().decode(cleBase64.trim());
        if (octets.length != 32) {
            throw new IllegalStateException("TWOFA_ENCRYPTION_KEY doit contenir 32 octets encodés en base64");
        }
        return new SecretKeySpec(octets, "AES");
    }
}
