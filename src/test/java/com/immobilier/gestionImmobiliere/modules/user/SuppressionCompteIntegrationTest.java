package com.immobilier.gestionImmobiliere.modules.user;

import com.immobilier.gestionImmobiliere.support.AbstractIntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Arrays;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * RG4 : un compte supprime reste en base (historique), perd tout acces immediatement
 * (y compris ses sessions en cours) et disparait des listes d'administration.
 * Utilisateurs crees par test pour ne pas toucher aux comptes du seed.
 */
class SuppressionCompteIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void suppression_conserveLeCompteEtSonHistorique_etCoupeTousLesAcces() throws Exception {
        int id = creerLocataire("supprime");
        int contrat = jdbc.queryForObject("""
                INSERT INTO contra_location (id_user, id_maison, date_entree, date_sortie, montant_loyer, statut, created_at)
                VALUES (?, 1, NOW() - INTERVAL '2 years', NOW() - INTERVAL '1 year', 150000, 'TERMINE', NOW())
                RETURNING id_contra_location""", Integer.class, id);
        Session session = connexion("supprime@gestimmo.test");
        Cookie admin = connexion("admin@gestimmo.test").access();

        mockMvc.perform(delete("/api/admin/users/" + id).cookie(admin)).andExpect(status().isOk());

        Map<String, Object> ligne = jdbc.queryForMap("SELECT is_deleted, flag_actif FROM users WHERE id_user = ?", id);
        assertThat(ligne).containsEntry("is_deleted", true).containsEntry("flag_actif", false);

        mockMvc.perform(get("/api/contrats-location/" + contrat).cookie(connexion("agent@gestimmo.test").access()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/users").param("size", "200").cookie(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[*].idUser", everyItem(not(id))));

        assertAccesCoupe(session, "supprime@gestimmo.test");
    }

    @Test
    void desactivation_coupeLesSessionsEnCours() throws Exception {
        int id = creerLocataire("desactive");
        Session session = connexion("desactive@gestimmo.test");
        Cookie admin = connexion("admin@gestimmo.test").access();

        mockMvc.perform(patch("/api/admin/users/" + id + "/status").cookie(admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"flagActif\":false}"))
                .andExpect(status().isOk());

        assertAccesCoupe(session, "desactive@gestimmo.test");
    }

    private void assertAccesCoupe(Session session, String email) throws Exception {
        mockMvc.perform(get("/api/users/me").cookie(session.access()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/refresh-token").cookie(session.refresh(), session.deviceId()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/signin").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, MOT_DE_PASSE_SEED)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCOUNT_DISABLED"));
    }

    private int creerLocataire(String prefixe) {
        return jdbc.queryForObject("""
                INSERT INTO users (id_role, nom, prenom, sexe, email, mot_de_passe, date_naissance, telephone, flag_actif, created_at)
                SELECT id_role, 'Test', ?, 'M', ?, mot_de_passe, '1990-01-01', ?, TRUE, NOW()
                FROM users WHERE email = 'client@gestimmo.test'
                RETURNING id_user""", Integer.class,
                prefixe, prefixe + "@gestimmo.test", "7" + (System.nanoTime() % 100_000_000L));
    }

    private record Session(Cookie access, Cookie refresh, Cookie deviceId) {}

    private Session connexion(String email) throws Exception {
        MvcResult login = mockMvc.perform(post("/api/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, MOT_DE_PASSE_SEED)))
                .andExpect(status().isOk())
                .andReturn();
        Cookie dernierDeviceId = Arrays.stream(login.getResponse().getCookies())
                .filter(c -> "device_id".equals(c.getName()))
                .reduce((premier, second) -> second)
                .orElseThrow();
        return new Session(login.getResponse().getCookie("access_token"),
                login.getResponse().getCookie("refresh_token"), dernierDeviceId);
    }
}
