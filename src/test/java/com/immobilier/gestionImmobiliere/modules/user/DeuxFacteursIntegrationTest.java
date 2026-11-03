package com.immobilier.gestionImmobiliere.modules.user;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.immobilier.gestionImmobiliere.modules.user.deuxfacteurs.TotpService;
import com.immobilier.gestionImmobiliere.support.AbstractIntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Double authentification TOTP : activation, connexion en deux etapes, codes de secours, appareil de confiance. */
class DeuxFacteursIntegrationTest extends AbstractIntegrationTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TotpService totpService;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbc;

    // La base est partagee entre les tests : on remet le 2FA a zero pour qu'aucun test ne depende d'un autre
    @org.junit.jupiter.api.BeforeEach
    @org.junit.jupiter.api.AfterEach
    void reinitialiserDeuxFacteurs() {
        jdbc.update("DELETE FROM codes_secours_2fa");
        jdbc.update("UPDATE users SET totp_actif = FALSE, totp_secret = NULL");
    }

    @Test
    void parcoursComplet_activation_connexionEnDeuxEtapes_codeDeSecours_etAppareilDeConfiance() throws Exception {
        String email = "agent@gestimmo.test";
        Cookie[] session = connexionSimple(email);

        // Activation : le secret est genere, la fonction reste inactive tant que le code n'est pas confirme
        MvcResult init = mockMvc.perform(post("/api/users/me/2fa/initier").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.qrCode").value(org.hamcrest.Matchers.startsWith("data:image/png;base64,")))
                .andReturn();
        String secret = data(init).get("secret").asText();
        mockMvc.perform(get("/api/users/me/2fa").cookie(session))
                .andExpect(jsonPath("$.data.actif").value(false));

        // Un code faux est refuse, le bon active la fonction et remet 10 codes de secours
        mockMvc.perform(post("/api/users/me/2fa/confirmer").cookie(session)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"code\":\"000000\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_TWO_FACTOR_CODE"));
        long periode = System.currentTimeMillis() / 1000 / 30;
        MvcResult confirmation = mockMvc.perform(post("/api/users/me/2fa/confirmer").cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"" + totpService.codePourPeriode(secret, periode) + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.codesSecours.length()").value(10))
                .andReturn();
        JsonNode codesSecours = data(confirmation).get("codesSecours");

        // Connexion : le mot de passe seul ne donne plus de session
        MvcResult etape1 = signin(email)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("TWO_FACTOR_REQUIRED"))
                .andExpect(cookie().doesNotExist("access_token"))
                .andReturn();
        String defi = data(etape1).get("challengeToken").asText();

        // Code faux refuse, bon code accepte (periode suivante : celle de l'activation est deja consommee)
        verifier(defi, "123456", false).andExpect(status().isUnauthorized());
        verifier(defi, totpService.codePourPeriode(secret, periode), false).andExpect(status().isUnauthorized());
        verifier(defi, totpService.codePourPeriode(secret, periode + 1), false)
                .andExpect(status().isOk())
                .andExpect(cookie().exists("access_token"))
                .andExpect(cookie().doesNotExist("appareil_confiance"));
        // Le defi est a usage unique
        verifier(defi, totpService.codePourPeriode(secret, periode + 1), false).andExpect(status().isUnauthorized());

        // Code de secours : accepte une fois, refuse ensuite ; avec "faire confiance" l'appareil est memorise
        String secours = codesSecours.get(0).asText();
        String defi2 = data(signin(email).andReturn()).get("challengeToken").asText();
        MvcResult confiance = verifier(defi2, secours, true)
                .andExpect(status().isOk())
                .andExpect(cookie().exists("appareil_confiance"))
                .andReturn();
        String defi3 = data(signin(email).andReturn()).get("challengeToken").asText();
        verifier(defi3, secours, false).andExpect(status().isUnauthorized());

        // Appareil de confiance : le mot de passe suffit, sans code
        Cookie appareil = confiance.getResponse().getCookie("appareil_confiance");
        assertNotNull(appareil);
        signin(email, appareil)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("LOGIN_SUCCESS"))
                .andExpect(cookie().exists("access_token"));
    }

    @Test
    void desactivation_exigeLeMotDePasseEtUnCode_puisLaConnexionRedevientSimple() throws Exception {
        String email = "agent@gestimmo.test";
        Cookie[] session = connexionSimple(email);
        String secret = data(mockMvc.perform(post("/api/users/me/2fa/initier").cookie(session)).andReturn())
                .get("secret").asText();
        long periode = System.currentTimeMillis() / 1000 / 30;
        mockMvc.perform(post("/api/users/me/2fa/confirmer").cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"" + totpService.codePourPeriode(secret, periode) + "\"}"))
                .andExpect(status().isOk());

        String corps = "{\"motDePasse\":\"%s\",\"code\":\"%s\"}";
        mockMvc.perform(post("/api/users/me/2fa/desactiver").cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corps.formatted("mauvais", totpService.codePourPeriode(secret, periode + 1))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_PASSWORD"));
        mockMvc.perform(post("/api/users/me/2fa/desactiver").cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corps.formatted(MOT_DE_PASSE_SEED, totpService.codePourPeriode(secret, periode + 1))))
                .andExpect(status().isOk());

        signin(email).andExpect(jsonPath("$.code").value("LOGIN_SUCCESS"));
    }

    @Test
    void adminPeutReinitialiserLeDeuxFacteursD_unAgent() throws Exception {
        Cookie[] sessionAgent = connexionSimple("agent@gestimmo.test");
        String secret = data(mockMvc.perform(post("/api/users/me/2fa/initier").cookie(sessionAgent)).andReturn())
                .get("secret").asText();
        long periode = System.currentTimeMillis() / 1000 / 30;
        mockMvc.perform(post("/api/users/me/2fa/confirmer").cookie(sessionAgent)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"" + totpService.codePourPeriode(secret, periode) + "\"}"))
                .andExpect(status().isOk());
        JsonNode statut = data(mockMvc.perform(get("/api/users/me/2fa").cookie(sessionAgent)).andReturn());
        assertEquals(10, statut.get("codesSecoursRestants").asInt());

        // Un agent ne peut pas reinitialiser le compte d'un autre
        mockMvc.perform(delete("/api/admin/users/1/2fa").cookie(sessionAgent)).andExpect(status().isForbidden());

        Integer idAgent = idUtilisateur(sessionAgent);
        mockMvc.perform(delete("/api/admin/users/" + idAgent + "/2fa").cookie(connexionSimple("admin@gestimmo.test")))
                .andExpect(status().isOk());
        signin("agent@gestimmo.test").andExpect(jsonPath("$.code").value("LOGIN_SUCCESS"));
    }

    @Test
    void dureeDeConfianceDeLAppareil_suitLeParametreModifieParLAdmin() throws Exception {
        Cookie[] admin = connexionSimple("admin@gestimmo.test");
        Cookie[] agent = connexionSimple("agent@gestimmo.test");
        try {
            mockMvc.perform(get("/api/users/me/2fa").cookie(agent))
                    .andExpect(jsonPath("$.data.heuresConfiance").value(12));

            mockMvc.perform(patch("/api/parametres/DUREE_CONFIANCE_APPAREIL_2FA_HEURES").cookie(admin)
                            .contentType(MediaType.APPLICATION_JSON).content("{\"valeur\":\"24\"}"))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/users/me/2fa").cookie(agent))
                    .andExpect(jsonPath("$.data.heuresConfiance").value(24));
        } finally {
            mockMvc.perform(patch("/api/parametres/DUREE_CONFIANCE_APPAREIL_2FA_HEURES").cookie(admin)
                    .contentType(MediaType.APPLICATION_JSON).content("{\"valeur\":\"12\"}"));
        }
    }

    @Test
    void bailleur_nePeutPasActiverLeDeuxFacteurs() throws Exception {
        Cookie[] session = connexionSimple("bailleur@gestimmo.test");
        mockMvc.perform(get("/api/users/me/2fa").cookie(session))
                .andExpect(jsonPath("$.data.disponible").value(false));
        mockMvc.perform(post("/api/users/me/2fa/initier").cookie(session))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("TWO_FACTOR_NOT_AVAILABLE"));
    }

    @Test
    void cinqCodesErrones_bloquentLesEssaisSuivants() throws Exception {
        String email = "agent@gestimmo.test";
        Cookie[] session = connexionSimple(email);
        String secret = data(mockMvc.perform(post("/api/users/me/2fa/initier").cookie(session)).andReturn())
                .get("secret").asText();
        long periode = System.currentTimeMillis() / 1000 / 30;
        mockMvc.perform(post("/api/users/me/2fa/confirmer").cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"" + totpService.codePourPeriode(secret, periode) + "\"}"))
                .andExpect(status().isOk());

        String defi = data(signin(email).andReturn()).get("challengeToken").asText();
        for (int i = 0; i < 5; i++) {
            verifier(defi, "111111", false).andExpect(status().isUnauthorized());
        }
        verifier(defi, totpService.codePourPeriode(secret, periode + 1), false)
                .andExpect(status().isTooManyRequests());
    }

    // ---------------------------------------------------------------

    private org.springframework.test.web.servlet.ResultActions signin(String email, Cookie... cookies) throws Exception {
        var requete = post("/api/auth/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + MOT_DE_PASSE_SEED + "\"}");
        if (cookies.length > 0) {
            requete.cookie(cookies);
        }
        return mockMvc.perform(requete);
    }

    private org.springframework.test.web.servlet.ResultActions verifier(String defi, String code, boolean confiance) throws Exception {
        return mockMvc.perform(post("/api/auth/2fa/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"challengeToken\":\"%s\",\"code\":\"%s\",\"faireConfiance\":%s}".formatted(defi, code, confiance)));
    }

    private Cookie[] connexionSimple(String email) throws Exception {
        MvcResult login = signin(email).andExpect(status().isOk()).andExpect(cookie().exists("access_token")).andReturn();
        return login.getResponse().getCookies();
    }

    private Integer idUtilisateur(Cookie[] session) throws Exception {
        MvcResult me = mockMvc.perform(get("/api/users/me").cookie(session)).andExpect(status().isOk()).andReturn();
        JsonNode corps = JSON.readTree(me.getResponse().getContentAsString());
        JsonNode id = corps.at("/data/idUser");
        assertFalse(id.isMissingNode(), "idUser absent de /api/users/me : " + corps);
        return id.asInt();
    }

    private static JsonNode data(MvcResult resultat) throws Exception {
        return JSON.readTree(resultat.getResponse().getContentAsString()).get("data");
    }
}
