package com.immobilier.gestionImmobiliere.utils;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Résout l'IP "réelle" d'un client à des fins de rate limiting / blacklisting.
 *
 * X-Forwarded-For est un en-tête envoyé par le client lui-même : n'importe qui
 * peut y mettre la valeur de son choix. On ne peut lui faire confiance que si
 * la requête arrive elle-même d'un proxy de confiance connu (qui l'aurait
 * réécrit) — sinon on retombe sur l'adresse socket réelle, non falsifiable.
 */
@Component
public class ClientIpResolver {

    @Value("${app.security.trusted-proxies:}")
    private String trustedProxiesRaw;

    public String resolve(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();

        if (!estProxyDeConfiance(remoteAddr)) {
            return remoteAddr;
        }

        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded == null || forwarded.isBlank()) {
            return remoteAddr;
        }

        // Le premier maillon de la chaîne est l'IP d'origine du client.
        return forwarded.split(",")[0].trim();
    }

    private boolean estProxyDeConfiance(String remoteAddr) {
        Set<String> proxies = proxiesDeConfiance();
        return !proxies.isEmpty() && proxies.contains(remoteAddr);
    }

    private Set<String> proxiesDeConfiance() {
        if (trustedProxiesRaw == null || trustedProxiesRaw.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(trustedProxiesRaw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }
}
