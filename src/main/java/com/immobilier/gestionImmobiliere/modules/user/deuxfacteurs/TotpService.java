package com.immobilier.gestionImmobiliere.modules.user.deuxfacteurs;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/** Mots de passe a usage unique bases sur le temps (RFC 6238 : SHA-1, 6 chiffres, pas de 30 s). */
@Service
public class TotpService {

    private static final String ALPHABET_BASE32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final int TAILLE_SECRET_OCTETS = 20;
    private static final int PAS_SECONDES = 30;
    private static final int CHIFFRES = 6;
    // Tolerance d'une periode de chaque cote pour absorber le decalage d'horloge du telephone
    private static final int TOLERANCE_PERIODES = 1;

    private final SecureRandom random = new SecureRandom();

    /**
     * Génère un secret aléatoire de 20 octets (160 bits) encodé en base32 : c'est la clé partagée
     * entre le serveur et l'application d'authentification de l'utilisateur.
     */
    public String genererSecret() {
        byte[] octets = new byte[TAILLE_SECRET_OCTETS];
        random.nextBytes(octets);
        return encoderBase32(octets);
    }

    /**
     * Retourne le numero de periode du code s'il est valide (sert a refuser un meme code
     * rejoue), ou -1 s'il est faux.
     */
    public long verifier(String secretBase32, String code) {
        if (code == null || !code.matches("\\d{" + CHIFFRES + "}")) {
            return -1;
        }
        byte[] cle = decoderBase32(secretBase32);
        long periodeCourante = System.currentTimeMillis() / 1000 / PAS_SECONDES;
        for (int decalage = -TOLERANCE_PERIODES; decalage <= TOLERANCE_PERIODES; decalage++) {
            long periode = periodeCourante + decalage;
            if (egalEnTempsConstant(calculer(cle, periode), code)) {
                return periode;
            }
        }
        return -1;
    }

    /** Code attendu pour une periode donnee (utile aux tests). */
    public String codePourPeriode(String secretBase32, long periode) {
        return calculer(decoderBase32(secretBase32), periode);
    }

    /**
     * Construit l'adresse otpauth:// que contient le QR code : émetteur, compte, secret et paramètres
     * (SHA-1, 6 chiffres, pas de 30 s) que l'application d'authentification recopie au scan.
     */
    public String urlOtpAuth(String emetteur, String compte, String secretBase32) {
        String label = encoderUrl(emetteur) + ":" + encoderUrl(compte);
        return "otpauth://totp/" + label + "?secret=" + secretBase32 + "&issuer=" + encoderUrl(emetteur)
                + "&algorithm=SHA1&digits=" + CHIFFRES + "&period=" + PAS_SECONDES;
    }

    /** QR code en image PNG, au format data URI directement utilisable dans une balise img. */
    public String qrCodeDataUri(String contenu) {
        try {
            BitMatrix matrice = new QRCodeWriter().encode(contenu, BarcodeFormat.QR_CODE, 240, 240);
            ByteArrayOutputStream sortie = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrice, "PNG", sortie);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(sortie.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("Génération du QR code impossible", e);
        }
    }

    /**
     * Calcule le code à 6 chiffres d'une période : HMAC-SHA1(secret, numéro de période), puis
     * troncature dynamique de la RFC 4226 (4 octets choisis par les 4 derniers bits) et réduction modulo 10^6.
     */
    private String calculer(byte[] cle, long periode) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(cle, "HmacSHA1"));
            byte[] hash = mac.doFinal(ByteBuffer.allocate(8).putLong(periode).array());
            int offset = hash[hash.length - 1] & 0x0F;
            int binaire = ((hash[offset] & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8)
                    | (hash[offset + 3] & 0xFF);
            int modulo = 1;
            for (int i = 0; i < CHIFFRES; i++) {
                modulo *= 10;
            }
            return String.format("%0" + CHIFFRES + "d", binaire % modulo);
        } catch (Exception e) {
            throw new IllegalStateException("Calcul du code TOTP impossible", e);
        }
    }

    /**
     * Compare deux chaînes sans s'arrêter à la première différence, pour ne pas révéler par le temps
     * de réponse combien de chiffres du code étaient justes.
     */
    private static boolean egalEnTempsConstant(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Encode un texte pour une adresse (les espaces deviennent %20, comme l'attendent les applications).
     */
    private static String encoderUrl(String valeur) {
        return URLEncoder.encode(valeur, StandardCharsets.UTF_8).replace("+", "%20");
    }

    /**
     * Encode des octets en base32 (alphabet de la RFC 4648), le format de secret lu par les applications d'authentification.
     */
    static String encoderBase32(byte[] octets) {
        StringBuilder sb = new StringBuilder();
        int tampon = 0;
        int bits = 0;
        for (byte b : octets) {
            tampon = (tampon << 8) | (b & 0xFF);
            bits += 8;
            while (bits >= 5) {
                sb.append(ALPHABET_BASE32.charAt((tampon >> (bits - 5)) & 31));
                bits -= 5;
            }
        }
        if (bits > 0) {
            sb.append(ALPHABET_BASE32.charAt((tampon << (5 - bits)) & 31));
        }
        return sb.toString();
    }

    /**
     * Décode un secret base32 en octets (insensible à la casse, ignore les caractères hors alphabet).
     */
    static byte[] decoderBase32(String texte) {
        ByteArrayOutputStream sortie = new ByteArrayOutputStream();
        int tampon = 0;
        int bits = 0;
        for (char c : texte.toUpperCase().toCharArray()) {
            int valeur = ALPHABET_BASE32.indexOf(c);
            if (valeur < 0) {
                continue;
            }
            tampon = (tampon << 5) | valeur;
            bits += 5;
            if (bits >= 8) {
                sortie.write((tampon >> (bits - 8)) & 0xFF);
                bits -= 8;
            }
        }
        return sortie.toByteArray();
    }
}
