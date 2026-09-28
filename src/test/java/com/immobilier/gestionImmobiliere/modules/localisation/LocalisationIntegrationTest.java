package com.immobilier.gestionImmobiliere.modules.localisation;

import com.immobilier.gestionImmobiliere.support.AbstractIntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Hierarchie pays -> ville -> secteur. Chaque test cree sa propre hierarchie : supprimer
 * un pays du seed ferait disparaitre en cascade les cours et maisons des autres tests.
 */
class LocalisationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    private Cookie agent;
    private Cookie admin;

    @BeforeEach
    void connexions() throws Exception {
        agent = connexion("agent@gestimmo.test");
        admin = connexion("admin@gestimmo.test");
    }

    @Test
    void hierarchie_creeeParLAgence_etConsultablePubliquement() throws Exception {
        int pays = creerPays("901", "Pays Test A");
        int ville = creerVille(pays, "V01", "Ville Test A");
        creerSecteur(ville, "S01", "Secteur Test A");

        mockMvc.perform(get("/api/public/villes").param("idPays", String.valueOf(pays)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].nomVille").value("Ville Test A"));
        mockMvc.perform(get("/api/public/secteurs").param("idVille", String.valueOf(ville)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1));
    }

    @Test
    void codesEnDouble_refuses() throws Exception {
        int pays = creerPays("902", "Pays Test B");
        creerVille(pays, "V02", "Ville Test B");

        appel(post("/api/pays"), agent, "{\"codePays\":\"902\",\"nomPays\":\"Doublon\"}", status().isConflict());
        appel(post("/api/villes"), agent, """
                {"idPays":%d,"codeVille":"v02","nomVille":"Doublon casse differente"}""".formatted(pays),
                status().isConflict());
    }

    @Test
    void suppressionDUnPays_cascadeEnSoftDeleteSurVillesEtSecteurs() throws Exception {
        int pays = creerPays("903", "Pays Test C");
        int ville = creerVille(pays, "V03", "Ville Test C");
        int secteur = creerSecteur(ville, "S03", "Secteur Test C");

        mockMvc.perform(delete("/api/pays/" + pays).cookie(agent)).andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/pays/" + pays).cookie(admin)).andExpect(status().isOk());

        assertThat(estSupprime("pays", "id_pays", pays)).isTrue();
        assertThat(estSupprime("ville", "id_ville", ville)).as("cascade pays -> ville").isTrue();
        assertThat(estSupprime("secteur", "id_secteur", secteur)).as("cascade ville -> secteur").isTrue();
        mockMvc.perform(get("/api/public/pays/" + pays)).andExpect(status().isNotFound());
    }

    private int creerPays(String code, String nom) throws Exception {
        appel(post("/api/pays"), agent, "{\"codePays\":\"%s\",\"nomPays\":\"%s\"}".formatted(code, nom),
                status().isCreated());
        return jdbc.queryForObject("SELECT id_pays FROM pays WHERE code_pays = ?", Integer.class, code);
    }

    private int creerVille(int pays, String code, String nom) throws Exception {
        appel(post("/api/villes"), agent, """
                {"idPays":%d,"codeVille":"%s","nomVille":"%s"}""".formatted(pays, code, nom), status().isCreated());
        return jdbc.queryForObject("SELECT id_ville FROM ville WHERE code_ville = ? AND id_pays = ?",
                Integer.class, code, pays);
    }

    private int creerSecteur(int ville, String code, String nom) throws Exception {
        appel(post("/api/secteurs"), agent, """
                {"idVille":%d,"codeSecteur":"%s","nomSecteur":"%s"}""".formatted(ville, code, nom),
                status().isCreated());
        return jdbc.queryForObject("SELECT id_secteur FROM secteur WHERE code_secteur = ? AND id_ville = ?",
                Integer.class, code, ville);
    }

    private boolean estSupprime(String table, String colonneId, int id) {
        return jdbc.queryForObject("SELECT is_deleted FROM " + table + " WHERE " + colonneId + " = ?",
                Boolean.class, id);
    }

    private void appel(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder requete,
                       Cookie utilisateur, String json, ResultMatcher attendu) throws Exception {
        mockMvc.perform(requete.cookie(utilisateur).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(attendu);
    }

    private Cookie connexion(String email) throws Exception {
        return mockMvc.perform(post("/api/auth/signin").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, MOT_DE_PASSE_SEED)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getCookie("access_token");
    }
}
