package com.immobilier.gestionImmobiliere.modules.biens;

import com.immobilier.gestionImmobiliere.support.AbstractIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cycle de statut des maisons (RG3) et location de biens/services : demande du client,
 * puis confirmation par l'agent a reception du paiement (paiement prealable obligatoire).
 * Bien 2 du seed : "Kit sonorisation", 15 000 / jour.
 */
class BiensIntegrationTest extends AbstractIntegrationTest {

    private static final int KIT_SONO = 2;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    private Cookie agent;
    private Cookie client;
    private Cookie autreClient;

    @BeforeEach
    void connexions() throws Exception {
        agent = connexion("agent@gestimmo.test");
        client = connexion("client@gestimmo.test");
        autreClient = connexion("client2@gestimmo.test");
    }

    @Test
    void maison_transitionsDeStatut() throws Exception {
        String reponse = mockMvc.perform(post("/api/maisons").cookie(agent).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idCour":1,"typeMaison":"Studio","nomCommunMaison":"Maison test statuts",
                                 "nombrePiece":1,"loyer":50000,"caution":100000,"nombreMoisCaution":2}"""))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        int id = JsonPath.read(reponse, "$.data.idMaison");
        assertThat(statutMaison(id)).isEqualTo("DISPONIBLE");

        changerStatut(id, "EN_MAINTENANCE", status().isOk());
        changerStatut(id, "LOUEE", status().isConflict());
        assertThat(statutMaison(id)).isEqualTo("EN_MAINTENANCE");
        changerStatut(id, "DISPONIBLE", status().isOk());
    }

    @Test
    void locationBienService_demandeClientPuisConfirmationContrePaiement() throws Exception {
        LocalDateTime debut = LocalDateTime.now().plusDays(10).truncatedTo(ChronoUnit.DAYS);
        int id = demanderLocation(client, debut, debut.plusDays(3));
        assertThat(statutLocation(id)).isEqualTo("EN_ATTENTE");
        assertThat(jdbc.queryForObject("SELECT montant_total FROM location_bien_service WHERE id_location_bien_service = ?",
                BigDecimal.class, id)).as("3 jours x 15 000").isEqualByComparingTo("45000");

        String confirmation = """
                {"montantPaiement":45000,"modePaiement":"ESPECES","dateDebut":"%s","dateFin":"%s"}"""
                .formatted(debut, debut.plusDays(3));
        appel(patch("/api/locations-biens-services/" + id + "/confirmer"), client, confirmation, status().isForbidden());
        appel(patch("/api/locations-biens-services/" + id + "/confirmer"), agent, confirmation, status().isOk());
        assertThat(statutLocation(id)).isEqualTo("ACTIF");
        assertThat(jdbc.queryForObject("""
                SELECT p.montant_paiement FROM paiement p
                JOIN paiement_location_bien_service pl ON pl.id_paiement = p.id_paiement
                WHERE pl.id_location_bien_service = ?""", BigDecimal.class, id)).isEqualByComparingTo("45000");

        appel(patch("/api/locations-biens-services/" + id + "/confirmer"), agent, confirmation, status().isConflict());
    }

    @Test
    void locationBienService_confirmationSansAjustementDeDates_reprendCellesDeLaDemande() throws Exception {
        LocalDateTime debut = LocalDateTime.now().plusDays(20).truncatedTo(ChronoUnit.DAYS);
        int id = demanderLocation(client, debut, debut.plusDays(2));

        appel(patch("/api/locations-biens-services/" + id + "/confirmer"), agent, """
                {"montantPaiement":30000,"modePaiement":"ESPECES"}""", status().isOk());
        assertThat(statutLocation(id)).isEqualTo("ACTIF");
        assertThat(jdbc.queryForObject("SELECT duree FROM location_bien_service WHERE id_location_bien_service = ?",
                Long.class, id)).isEqualTo(2L);
    }

    @Test
    void locationBienService_annulableSeulementParSonAuteurTantQuEnAttente() throws Exception {
        LocalDateTime debut = LocalDateTime.now().plusDays(30).truncatedTo(ChronoUnit.DAYS);
        int id = demanderLocation(client, debut, debut.plusDays(1));

        appel(patch("/api/locations-biens-services/" + id + "/annuler"), autreClient, null, status().isForbidden());
        appel(patch("/api/locations-biens-services/" + id + "/annuler"), client, null, status().isOk());
        assertThat(statutLocation(id)).isEqualTo("ANNULE");
        appel(patch("/api/locations-biens-services/" + id + "/annuler"), client, null, status().isConflict());
    }

    @Test
    void locationBienService_datesInvalides_refusee() throws Exception {
        LocalDateTime debut = LocalDateTime.now().plusDays(40).truncatedTo(ChronoUnit.DAYS);
        appel(post("/api/locations-biens-services"), client, """
                {"idBienService":%d,"destination":"Test","dateDebut":"%s","dateFin":"%s"}"""
                .formatted(KIT_SONO, debut, debut.minusDays(1)), status().isBadRequest());
    }

    private int demanderLocation(Cookie demandeur, LocalDateTime debut, LocalDateTime fin) throws Exception {
        String reponse = mockMvc.perform(post("/api/locations-biens-services").cookie(demandeur)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"idBienService":%d,"destination":"Mariage","dateDebut":"%s","dateFin":"%s"}"""
                                .formatted(KIT_SONO, debut, fin)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(reponse, "$.data.idLocationBienService");
    }

    private void changerStatut(int idMaison, String statut, ResultMatcher attendu) throws Exception {
        appel(patch("/api/maisons/" + idMaison + "/statut"), agent, "{\"statut\":\"%s\"}".formatted(statut), attendu);
    }

    private void appel(MockHttpServletRequestBuilder requete, Cookie utilisateur, String json, ResultMatcher attendu)
            throws Exception {
        requete.cookie(utilisateur);
        if (json != null) requete.contentType(MediaType.APPLICATION_JSON).content(json);
        mockMvc.perform(requete).andExpect(attendu);
    }

    private String statutMaison(int id) {
        return jdbc.queryForObject("SELECT statut FROM maison WHERE id_maison = ?", String.class, id);
    }

    private String statutLocation(int id) {
        return jdbc.queryForObject("SELECT statut FROM location_bien_service WHERE id_location_bien_service = ?",
                String.class, id);
    }

    private Cookie connexion(String email) throws Exception {
        return mockMvc.perform(post("/api/auth/signin").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, MOT_DE_PASSE_SEED)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getCookie("access_token");
    }
}
