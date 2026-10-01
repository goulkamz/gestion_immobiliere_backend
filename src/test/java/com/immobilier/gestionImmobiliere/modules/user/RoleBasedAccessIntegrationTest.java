package com.immobilier.gestionImmobiliere.modules.user;

import com.immobilier.gestionImmobiliere.support.AbstractIntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifie le quadrillage par role de UserAdminController (@PreAuthorize desormais
 * au niveau methode, pas classe) : getAll/getById ouverts a ADMIN+AGENT (l'agent a
 * besoin de lister les bailleurs/gestionnaires pour les selecteurs de creation de
 * cour/bien-service), le reste (create/update/updateRole/updateStatus/delete)
 * reserve a ADMIN seul. Point sensible en cas de changement de comportement de
 * @EnableMethodSecurity / Spring Security lors de la migration.
 */
class RoleBasedAccessIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @ParameterizedTest
    @ValueSource(strings = {"bailleur@gestimmo.test", "client@gestimmo.test"})
    void endpointAdmin_avecRoleNonAdminNonAgent_retourne403(String email) throws Exception {
        Cookie accessCookie = seConnecterEtRecupererCookieAcces(email);

        mockMvc.perform(get("/api/admin/users").cookie(accessCookie))
                .andExpect(status().isForbidden());
    }

    @org.junit.jupiter.api.Test
    void endpointAdmin_avecRoleAgent_lectureAutorisee_ecritureRefusee() throws Exception {
        Cookie accessCookie = seConnecterEtRecupererCookieAcces("agent@gestimmo.test");

        mockMvc.perform(get("/api/admin/users").cookie(accessCookie))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/admin/users/1").cookie(accessCookie))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/admin/users/1").cookie(accessCookie))
                .andExpect(status().isForbidden());
    }

    @org.junit.jupiter.api.Test
    void endpointAdmin_avecRoleAdmin_retourne200() throws Exception {
        Cookie accessCookie = seConnecterEtRecupererCookieAcces("admin@gestimmo.test");

        mockMvc.perform(get("/api/admin/users").cookie(accessCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.page.totalElements").isNumber())
                .andExpect(jsonPath("$.data.page.totalPages").isNumber())
                .andExpect(jsonPath("$.data.pageable").doesNotExist());
    }

    @org.junit.jupiter.api.Test
    void statsPubliques_sansAuthentification_retourne200() throws Exception {
        mockMvc.perform(get("/api/stats/public")).andExpect(status().isOk());
        mockMvc.perform(get("/api/stats/public/annonces")).andExpect(status().isOk());
    }

    @org.junit.jupiter.api.Test
    void statsNonPubliques_sansAuthentification_retourne401() throws Exception {
        mockMvc.perform(get("/api/stats/admin")).andExpect(status().isUnauthorized());
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
