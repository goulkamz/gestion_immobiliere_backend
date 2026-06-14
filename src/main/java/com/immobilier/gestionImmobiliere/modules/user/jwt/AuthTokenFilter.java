package com.immobilier.gestionImmobiliere.modules.user.jwt;

import com.immobilier.gestionImmobiliere.modules.user.jwtService.UserDetailsImpl;
import com.immobilier.gestionImmobiliere.modules.user.jwtService.UserDetailsServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

@Component
public class AuthTokenFilter extends OncePerRequestFilter {

    private final UserDetailsServiceImpl userDetailsService;
    private final JwtUtils jwtUtils;

    public AuthTokenFilter(UserDetailsServiceImpl userDetailsService, JwtUtils jwtUtils) {
        this.userDetailsService = userDetailsService;
        this.jwtUtils = jwtUtils;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getServletPath();
        // Liste des chemins à ignorer complètement
        return path.startsWith("/auth/") || path.startsWith("/public/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        logger.debug("Processing request: {} {}");
        try {
            String jwt = jwtUtils.extractAccessToken(request);
            if (jwt != null && jwtUtils.validateAccessToken(jwt)) {
                String username = jwtUtils.getUserNameFromJwtToken(jwt);

                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails,
                                null,
                                userDetails.getAuthorities());

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
                logger.debug("Authenticated user: {}");
            }
        } catch (Exception e) {
            logger.error("Cannot set user authentication: {}", e);
        }

        filterChain.doFilter(request, response);
    }
}
