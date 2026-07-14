package com.immobilier.gestionImmobiliere.modules.journal.audit;

import com.immobilier.gestionImmobiliere.modules.user.jwtService.UserDetailsImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Capture l'id de l'utilisateur authentifié au début de chaque requête,
 * pour qu'il soit disponible côté listener Hibernate (même thread).
 */
@Component
public class AuditRequestFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        try {
            Integer userId = extraireUserId();
            AuditContextHolder.set(userId);
            chain.doFilter(request, response);
        } finally {
            AuditContextHolder.clear();
        }
    }

    private Integer extraireUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserDetailsImpl userDetails) {
            return userDetails.getIdUser();
        }
        return null;
    }
}