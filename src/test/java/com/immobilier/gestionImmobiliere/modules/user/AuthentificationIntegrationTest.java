package com.immobilier.gestionImmobiliere.modules.user;

import com.immobilier.gestionImmobiliere.support.AbstractIntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Arrays;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Filet de securite pour la chaine d'authentification JWT (cookies, filtre
 * AuthTokenFilter, refresh/logout) avant la migration vers une nouvelle
 * version majeure de Spring Boot : ce flux est le plus a risque en cas de
 * changement de comportement de Spring Security.
 */
class AuthentificationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void connexionReussie_definitLesCookiesEtRetourneLesRoles() throws Exception {
        mockMvc.perform(post("/api/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"admin@gestimmo.test","password":"%s"}
                                """.formatted(MOT_DE_PASSE_SEED)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roles[0]").value("ROLE_ADMIN"))
                .andExpect(cookie().exists("access_token"))
                .andExpect(cookie().exists("refresh_token"));
    }

    @Test
    void connexionAvecMauvaisMotDePasse_retourne401() throws Exception {
        mockMvc.perform(post("/api/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"admin@gestimmo.test","password":"mauvais-mot-de-passe"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void connexionAvecEmailInconnu_retourne401() throws Exception {
        mockMvc.perform(post("/api/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"inconnu@gestimmo.test","password":"%s"}
                                """.formatted(MOT_DE_PASSE_SEED)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void accesEndpointProtege_sansCookie_retourne401() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshToken_avecCookieValide_regenereUnAccessToken() throws Exception {
        MvcResult login = mockMvc.perform(post("/api/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"client@gestimmo.test","password":"%s"}
                                """.formatted(MOT_DE_PASSE_SEED)))
                .andExpect(status().isOk())
                .andReturn();

        var refreshCookie = login.getResponse().getCookie("refresh_token");
        var deviceIdCookie = dernierDeviceId(login);

        mockMvc.perform(post("/api/auth/refresh-token").cookie(refreshCookie, deviceIdCookie))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("access_token"));
    }

    @Test
    void logout_effaceLesCookies() throws Exception {
        MvcResult login = mockMvc.perform(post("/api/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"client2@gestimmo.test","password":"%s"}
                                """.formatted(MOT_DE_PASSE_SEED)))
                .andExpect(status().isOk())
                .andReturn();

        var accessCookie = login.getResponse().getCookie("access_token");
        var refreshCookie = login.getResponse().getCookie("refresh_token");
        var deviceIdCookie = dernierDeviceId(login);

        mockMvc.perform(post("/api/auth/logout").cookie(accessCookie, refreshCookie, deviceIdCookie))
                .andExpect(status().isOk())
                .andExpect(cookie().maxAge("access_token", 0))
                .andExpect(cookie().maxAge("refresh_token", 0));
    }

    // Au premier login, access et refresh token generent chacun un device_id : le
    // navigateur ne garde que le dernier, celui auquel est liee l'empreinte du
    // refresh token (verifiee au refresh, cf. FingerPrintService).
    private static Cookie dernierDeviceId(MvcResult login) {
        return Arrays.stream(login.getResponse().getCookies())
                .filter(c -> "device_id".equals(c.getName()))
                .reduce((premier, second) -> second)
                .orElseThrow();
    }
}
