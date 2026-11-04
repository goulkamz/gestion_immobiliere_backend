package com.immobilier.gestionImmobiliere.modules.user;

import com.immobilier.gestionImmobiliere.support.AbstractIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Un agent consulte les comptes clients, bailleurs et agents (sélecteurs, gestion des mandats) mais ne voit jamais
 * l'administrateur, et les fiches de ses collègues agents sont réduites à l'identité et à l'e-mail.
 * L'administrateur, lui, voit tout.
 */
class VisibiliteUtilisateursParAgentIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listeDeLAgent_necontientJamaisLAdministrateur() throws Exception {
        Cookie agent = connexion("agent@gestimmo.test");

        List<String> roles = JsonPath.read(liste(agent), "$.data.content[*].role");

        assertThat(roles).isNotEmpty().doesNotContain("ROLE_ADMIN");
        assertThat(roles).contains("ROLE_CLIENT", "ROLE_BAILLEUR", "ROLE_AGENT");
    }

    @Test
    void agentDemandantLeRoleAdmin_obtientUneListeVide() throws Exception {
        Cookie agent = connexion("agent@gestimmo.test");

        mockMvc.perform(get("/api/admin/users").param("role", "ROLE_ADMIN").cookie(agent))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isEmpty());
    }

    @Test
    void rechercheParEmailDeLAdmin_chezLAgent_necontientRien() throws Exception {
        Cookie agent = connexion("agent@gestimmo.test");

        mockMvc.perform(get("/api/admin/users").param("recherche", "admin@gestimmo.test").cookie(agent))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isEmpty());
    }

    @Test
    void ficheDeLAdmin_refuseALAgent_etFicheDUnClientComplete() throws Exception {
        Cookie agent = connexion("agent@gestimmo.test");
        Cookie admin = connexion("admin@gestimmo.test");

        mockMvc.perform(get("/api/admin/users/" + idDe(admin, "admin@gestimmo.test")).cookie(agent))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/users/" + idDe(admin, "client@gestimmo.test")).cookie(agent))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.telephone").isNotEmpty())
                .andExpect(jsonPath("$.data.dateNaissance").isNotEmpty());
    }

    @Test
    void ficheDUnCollegueAgent_estReduiteALIdentiteEtALEmail() throws Exception {
        Cookie agent = connexion("agent@gestimmo.test");
        Cookie admin = connexion("admin@gestimmo.test");
        // L'administrateur voit la fiche complète du même agent
        int idAgent = idDe(admin, "agent@gestimmo.test");
        mockMvc.perform(get("/api/admin/users/" + idAgent).cookie(admin))
                .andExpect(jsonPath("$.data.telephone").isNotEmpty())
                .andExpect(jsonPath("$.data.dateNaissance").isNotEmpty());

        mockMvc.perform(get("/api/admin/users/" + idAgent).cookie(agent))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("agent@gestimmo.test"))
                .andExpect(jsonPath("$.data.nom").isNotEmpty())
                .andExpect(jsonPath("$.data.telephone").doesNotExist())
                .andExpect(jsonPath("$.data.telephone1").doesNotExist())
                .andExpect(jsonPath("$.data.dateNaissance").doesNotExist())
                .andExpect(jsonPath("$.data.sexe").doesNotExist())
                .andExpect(jsonPath("$.data.dateLastLogin").doesNotExist())
                .andExpect(jsonPath("$.data.totpActif").doesNotExist());

        // Même réduction dans la liste
        mockMvc.perform(get("/api/admin/users").param("role", "ROLE_AGENT").cookie(agent))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].telephone").doesNotExist())
                .andExpect(jsonPath("$.data.content[0].dateNaissance").doesNotExist());
    }

    @Test
    void ladministrateur_voitTousLesRolesDontLuiMeme() throws Exception {
        Cookie admin = connexion("admin@gestimmo.test");

        List<String> roles = JsonPath.read(liste(admin), "$.data.content[*].role");

        assertThat(roles).contains("ROLE_ADMIN", "ROLE_AGENT", "ROLE_CLIENT", "ROLE_BAILLEUR");
    }

    private String liste(Cookie cookie) throws Exception {
        return mockMvc.perform(get("/api/admin/users").param("size", "200").cookie(cookie))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    private int idDe(Cookie admin, String email) throws Exception {
        String corps = mockMvc.perform(get("/api/admin/users").param("recherche", email).cookie(admin))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(corps, "$.data.content[0].idUser");
    }

    private Cookie connexion(String email) throws Exception {
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
