package com.immobilier.gestionImmobiliere.modules.user.jwt;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.UUID;

@Service
public class FingerPrintService {

    private static final Logger log = LoggerFactory.getLogger(FingerPrintService.class);

    @Value("${finger.secret.key}")
    private String fingerSecret;

    /**
     * Génère une empreinte unique pour la requête
     */
    public String generateFingerprint(HttpServletRequest request, HttpServletResponse response) {
        FingerprintData data = collectFingerprintData(request, response);
        String fingerprintData = buildFingerprintString(data);

        String fingerprint = UUID.nameUUIDFromBytes(fingerprintData.getBytes()).toString();
        log.debug("Fingerprint généré: {} pour appareil: {}", fingerprint, data.deviceId);

        return fingerprint;
    }

    private FingerprintData collectFingerprintData(HttpServletRequest request, HttpServletResponse response) {
        FingerprintData data = new FingerprintData();

        data.userAgent = cleanUserAgent(request.getHeader("User-Agent"));
        data.ip = getRealIp(request);
        data.deviceId = getOrCreateSecureDeviceId(request, response);
        data.acceptLanguage = request.getHeader("Accept-Language");
        data.secFetchSite = request.getHeader("Sec-Fetch-Site");
        data.secFetchMode = request.getHeader("Sec-Fetch-Mode");
        data.timezone = request.getHeader("X-Timezone");

        return data;
    }

    private String buildFingerprintString(FingerprintData data) {
        if (isMobile(data.userAgent)) {
            return String.join("|",
                    data.userAgent != null ? data.userAgent : "unknown",
                    data.deviceId != null ? data.deviceId : "unknown",
                    data.acceptLanguage != null ? data.acceptLanguage : "unknown"
            );
        } else {
            return String.join("|",
                    data.userAgent != null ? data.userAgent : "unknown",
                    data.ip != null ? data.ip : "unknown",
                    data.deviceId != null ? data.deviceId : "unknown",
                    data.acceptLanguage != null ? data.acceptLanguage : "unknown",
                    data.secFetchSite != null ? data.secFetchSite : "unknown",
                    data.secFetchMode != null ? data.secFetchMode : "unknown"
            );
        }
    }

    private String getRealIp(HttpServletRequest request) {
        String[] headers = {"X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP", "WL-Proxy-Client-IP"};

        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                if (header.equals("X-Forwarded-For")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        }
        return request.getRemoteAddr();
    }

    /**
     * Device ID sécurisé avec signature.
     * Si le cookie est absent OU falsifié (signature invalide), un nouveau
     * device_id est régénéré — évite de retomber sur "unknown" côté fingerprint,
     * ce qui affaiblirait silencieusement la détection de vol de token.
     */
    private String getOrCreateSecureDeviceId(HttpServletRequest request, HttpServletResponse response) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("device_id".equals(cookie.getName())) {
                    String deviceIdValide = validateAndExtractDeviceId(cookie.getValue());
                    if (deviceIdValide != null) {
                        return deviceIdValide;
                    }
                    log.warn("Cookie device_id présent mais invalide/falsifié — régénération");
                    break;
                }
            }
        }

        String deviceId = UUID.randomUUID().toString();
        String signedDeviceId = signDeviceId(deviceId);

        Cookie deviceCookie = new Cookie("device_id", signedDeviceId);
        deviceCookie.setHttpOnly(true);
        deviceCookie.setSecure(true);
        deviceCookie.setPath("/");
        deviceCookie.setMaxAge(365 * 24 * 60 * 60);
        deviceCookie.setAttribute("SameSite", "Strict");
        response.addCookie(deviceCookie);

        return deviceId;
    }

    private String signDeviceId(String deviceId) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret = new SecretKeySpec(fingerSecret.getBytes(), "HmacSHA256");
            mac.init(secret);
            byte[] signature = mac.doFinal(deviceId.getBytes());
            String signatureB64 = Base64.getEncoder().encodeToString(signature);
            return deviceId + "." + signatureB64;
        } catch (Exception e) {
            log.error("Échec signature device_id", e);
            return deviceId;
        }
    }

    /**
     * Valide et extrait le device ID signé.
     * Retourne null si le cookie est absent, malformé, ou si la signature ne correspond pas.
     */
    private String validateAndExtractDeviceId(String signedDeviceId) {
        if (signedDeviceId == null || !signedDeviceId.contains(".")) {
            return null;
        }

        String[] parts = signedDeviceId.split("\\.");
        if (parts.length != 2) {
            return null;
        }

        String deviceId = parts[0];
        String providedSignature = parts[1];

        String[] expectedParts = signDeviceId(deviceId).split("\\.");
        if (expectedParts.length != 2) {
            return null;
        }
        String expectedSignature = expectedParts[1];

        return providedSignature.equals(expectedSignature) ? deviceId : null;
    }

    private String cleanUserAgent(String userAgent) {
        if (userAgent == null) return null;

        return userAgent
                .replaceAll("Chrome/[0-9.]+", "Chrome")
                .replaceAll("Firefox/[0-9.]+", "Firefox")
                .replaceAll("Safari/[0-9.]+", "Safari")
                .replaceAll("Version/[0-9.]+", "Version")
                .replaceAll("Edge/[0-9.]+", "Edge")
                .replaceAll("rv:[0-9.]+", "rv");
    }

    private boolean isMobile(String userAgent) {
        return userAgent != null &&
                (userAgent.contains("Android") ||
                        userAgent.contains("iPhone") ||
                        userAgent.contains("iPad") ||
                        userAgent.contains("Mobile"));
    }

    /**
     * Vérifie si deux fingerprints correspondent. Un mismatch peut indiquer
     * un changement légitime (nouveau réseau) ou une tentative de vol de token.
     */
    public boolean fingerprintsMatch(String storedFingerprint, String currentFingerprint) {
        if (storedFingerprint.equals(currentFingerprint)) {
            return true;
        }

        log.warn("Fingerprint mismatch - Possible vol de token ou changement de réseau");
        log.warn("Stored: {}, Current: {}", storedFingerprint, currentFingerprint);

        return false;
    }

    @Data
    private static class FingerprintData {
        private String userAgent;
        private String ip;
        private String deviceId;
        private String acceptLanguage;
        private String secFetchSite;
        private String secFetchMode;
        private String timezone;
    }

    @Data
    public static class DeviceInfo {
        private final String deviceId;
        private long firstSeen;
        private long lastSeen;

        public DeviceInfo(String deviceId) {
            this.deviceId = deviceId;
            this.firstSeen = System.currentTimeMillis();
            this.lastSeen = System.currentTimeMillis();
        }

        public void updateLastSeen() {
            this.lastSeen = System.currentTimeMillis();
        }
    }
}