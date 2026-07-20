package com.immobilier.gestionImmobiliere.modules.user.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class IpBlacklistFilter extends OncePerRequestFilter {

    private final IpBlacklistService ipBlacklistService;

    public IpBlacklistFilter(IpBlacklistService ipBlacklistService) {
        this.ipBlacklistService = ipBlacklistService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String ip = extraireIp(request);

        if (ipBlacklistService.estBlackliste(ip)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write("{\"success\":false,\"message\":\"Accès temporairement bloqué\",\"code\":\"IP_BLACKLISTED\"}");
            return;
        }

        chain.doFilter(request, response);
    }

    private String extraireIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return (forwarded != null && !forwarded.isBlank()) ? forwarded.split(",")[0].trim() : request.getRemoteAddr();
    }
}