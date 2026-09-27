package com.immobilier.gestionImmobiliere.modules.user;

import com.immobilier.gestionImmobiliere.support.AbstractIntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifie que @PreAuthorize("hasRole('ADMIN')") sur UserAdminController continue
 * de filtrer correctement les 4 roles applicatifs (ROLE_ADMIN, ROLE_AGENT,
 * ROLE_BAILLEUR, ROLE_CLIENT) : point sensible en cas de changement de
 * comportement de @EnableMethodSecurity / Spring Security lors de la migration.
 */
class RoleBasedAccessIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @ParameterizedTest
    @ValueSource(strings = {"agent@gestimmo.test", "bailleur@gestimmo.test", "client@gestimmo.test"})
    void endpointAdmin_avecRoleNonAdmin_retourne403(String email) throws Exception {
        Cookie accessCookie = seConnecterEtRecupererCookieAcces(email);

        mockMvc.perform(get("/api/admin/users").cookie(accessCookie))
                .andExpect(status().isForbidden());
    }

    @org.junit.jupiter.api.Test
    void endpointAdmin_avecRoleAdmin_retourne200() throws Exception {
        Cookie accessCookie = seConnecterEtRecupererCookieAcces("admin@gestimmo.test");

        mockMvc.perform(get("/api/admin/users").cookie(accessCookie))
                .andExpect(status().isOk());
    }

    private Cookie seConnecterEtRecupererCookieAcces(String email) throws Exception {
        MvcResult login = mockMvc.perform(post("/api/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, MOT_DE_PASSE_SEED)))
                .andExpect(status().isOk())
                .andReturn();

        return login.getResponse().getCookie("access_token");
    }
}
