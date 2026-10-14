package com.immobilier.gestionImmobiliere.modules;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.immobilier.gestionImmobiliere.support.AbstractIntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Listes paginees cote serveur pour le bailleur et le client : statut, cour et recherche s'appliquent avant la
 * pagination, et le perimetre (mes cours / mes contrats) reste impose quoi que le navigateur demande.
 * Les totaux attendus sont recalcules en base avec les memes criteres, pour ne pas dependre de l'etat laisse
 * par les autres classes de tests (conteneurs partages).
 */
class ListesFiltreesBailleurClientIntegrationTest extends AbstractIntegrationTest {

    private static final String BAILLEUR = "bailleur@gestimmo.test";
    private static final String CLIENT = "client2@gestimmo.test";
    private static final String AUTRE_CLIENT = "client@gestimmo.test";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    private final ObjectMapper json = new ObjectMapper();

    private Cookie bailleur;
    private Cookie client;
    private int idBailleur;
    private int idClient;
    private int idAutreClient;

    @BeforeEach
    void connexions() throws Exception {
        bailleur = connexion(BAILLEUR);
        client = connexion(CLIENT);
        idBailleur = idUser(BAILLEUR);
        idClient = idUser(CLIENT);
        idAutreClient = idUser(AUTRE_CLIENT);
    }

    // ---------- Contrats de location ----------

    @Test
    void bailleur_contratsLocation_filtreParStatutEtParCour() throws Exception {
        String perimetre = """
                FROM contra_location cl JOIN maison m ON m.id_maison = cl.id_maison JOIN cour c ON c.id_cour = m.id_cour
                WHERE cl.is_deleted = FALSE AND c.id_user = ?""";

        assertThat(total(bailleur, "/api/contrats-location?size=50"))
                .isEqualTo(compter("SELECT COUNT(*) " + perimetre, idBailleur));
        for (String statut : new String[]{"ACTIF", "RESILIE"}) {
            assertThat(total(bailleur, "/api/contrats-location?size=50&statut=" + statut))
                    .as("statut " + statut)
                    .isEqualTo(compter("SELECT COUNT(*) " + perimetre + " AND cl.statut = ?", idBailleur, statut));
        }
        assertThat(total(bailleur, "/api/contrats-location?size=50&referenceCour=COUR-2026-004"))
                .isEqualTo(compter("SELECT COUNT(*) " + perimetre + " AND c.reference_cour = 'COUR-2026-004'", idBailleur));
        assertThat(total(bailleur, "/api/contrats-location?size=50&recherche=maison g"))
                .as("recherche sur le nom de la maison")
                .isEqualTo(compter("SELECT COUNT(*) " + perimetre + " AND LOWER(m.nom_commun_maison) LIKE '%maison g%'", idBailleur));
    }

    @Test
    void bailleur_contratsLocation_neVoitQueSesContrats() throws Exception {
        JsonNode contenu = lire(bailleur, "/api/contrats-location?size=50").get("content");
        for (JsonNode contrat : contenu) {
            int proprietaire = jdbc.queryForObject("""
                    SELECT c.id_user FROM contra_location cl JOIN maison m ON m.id_maison = cl.id_maison
                    JOIN cour c ON c.id_cour = m.id_cour WHERE cl.id_contra_location = ?""",
                    Integer.class, contrat.get("idContratLocation").asInt());
            assertThat(proprietaire).isEqualTo(idBailleur);
        }
    }

    @Test
    void client_contratsLocation_perimetreImposeEtFiltreParStatut() throws Exception {
        int siens = compter("SELECT COUNT(*) FROM contra_location WHERE is_deleted = FALSE AND id_user = ?", idClient);

        // idLocataire demande pour un autre client : ignore, le client ne voit que ses contrats
        assertThat(total(client, "/api/contrats-location?size=50&idLocataire=" + idAutreClient)).isEqualTo(siens);
        assertThat(total(client, "/api/contrats-location?size=50&statut=ACTIF"))
                .isEqualTo(compter("SELECT COUNT(*) FROM contra_location WHERE is_deleted = FALSE AND id_user = ? AND statut = 'ACTIF'", idClient));
        assertThat(total(client, "/api/contrats-location?size=50&statut=RESILIE"))
                .isEqualTo(compter("SELECT COUNT(*) FROM contra_location WHERE is_deleted = FALSE AND id_user = ? AND statut = 'RESILIE'", idClient));
    }

    // ---------- Echeances ----------

    @Test
    void bailleur_echeancesLocation_filtreParStatutCourEtRecherche() throws Exception {
        String perimetre = """
                FROM echeance_loyer e JOIN contra_location cl ON cl.id_contra_location = e.entite_echeance_id
                JOIN maison m ON m.id_maison = cl.id_maison JOIN cour c ON c.id_cour = m.id_cour
                WHERE e.is_deleted = FALSE AND e.entite_echeance_type = 'LOCATION' AND c.id_user = ?""";

        assertThat(total(bailleur, "/api/echeances?type=LOCATION&size=50"))
                .isEqualTo(compter("SELECT COUNT(*) " + perimetre, idBailleur));
        for (String statut : new String[]{"PAYE", "EN_ATTENTE", "EN_RETARD"}) {
            assertThat(total(bailleur, "/api/echeances?type=LOCATION&size=50&statut=" + statut))
                    .as("statut " + statut)
                    .isEqualTo(compter("SELECT COUNT(*) " + perimetre + " AND e.statut = ?", idBailleur, statut));
        }
        assertThat(total(bailleur, "/api/echeances?type=LOCATION&size=50&referenceCour=COUR-2026-004"))
                .as("filtre par cour sur les echeances de location")
                .isEqualTo(compter("SELECT COUNT(*) " + perimetre + " AND c.reference_cour = 'COUR-2026-004'", idBailleur));
        assertThat(total(bailleur, "/api/echeances?type=LOCATION&size=50&recherche=maison g"))
                .isEqualTo(compter("SELECT COUNT(*) " + perimetre + " AND LOWER(m.nom_commun_maison) LIKE '%maison g%'", idBailleur));
    }

    @Test
    void bailleur_echeancesMandat_filtreParModeDeFacturation() throws Exception {
        String perimetre = """
                FROM echeance_loyer e JOIN contrat_mandat cm ON cm.id_mandat = e.entite_echeance_id
                JOIN cour c ON c.id_cour = cm.id_cour
                WHERE e.is_deleted = FALSE AND e.entite_echeance_type = 'MANDAT' AND c.id_user = ?""";

        for (String mode : new String[]{"MENSUEL", "CUMUL"}) {
            assertThat(total(bailleur, "/api/echeances?type=MANDAT&size=50&modeFacturation=" + mode))
                    .as("mode " + mode)
                    .isEqualTo(compter("SELECT COUNT(*) " + perimetre + " AND cm.mode_facturation = ?", idBailleur, mode));
        }
    }

    @Test
    void client_echeances_typeForceALocationEtPerimetreImpose() throws Exception {
        String perimetre = """
                FROM echeance_loyer e JOIN contra_location cl ON cl.id_contra_location = e.entite_echeance_id
                WHERE e.is_deleted = FALSE AND e.entite_echeance_type = 'LOCATION' AND cl.id_user = ?""";

        // Le client demande des echeances de mandat : le serveur impose LOCATION sur ses seuls contrats
        JsonNode reponse = lire(client, "/api/echeances?type=MANDAT&size=50");
        assertThat(reponse.get("page").get("totalElements").asInt()).isEqualTo(compter("SELECT COUNT(*) " + perimetre, idClient));
        for (JsonNode echeance : reponse.get("content")) {
            assertThat(echeance.get("type").asText()).isEqualTo("LOCATION");
        }
        assertThat(total(client, "/api/echeances?size=50&statut=PAYE"))
                .isEqualTo(compter("SELECT COUNT(*) " + perimetre + " AND e.statut = 'PAYE'", idClient));
    }

    // ---------- Mandats ----------

    @Test
    void bailleur_mandats_filtreParStatut() throws Exception {
        String perimetre = """
                FROM contrat_mandat cm JOIN cour c ON c.id_cour = cm.id_cour
                WHERE cm.is_deleted = FALSE AND c.id_user = ?""";

        assertThat(total(bailleur, "/api/contrats-mandat?size=50")).isEqualTo(compter("SELECT COUNT(*) " + perimetre, idBailleur));
        for (String statut : new String[]{"ACTIF", "RESILIE", "EN_ATTENTE"}) {
            assertThat(total(bailleur, "/api/contrats-mandat?size=50&statut=" + statut))
                    .as("statut " + statut)
                    .isEqualTo(compter("SELECT COUNT(*) " + perimetre + " AND cm.statut = ?", idBailleur, statut));
        }
    }

    // ---------- Reservations ----------

    @Test
    void client_reservations_perimetreEtStatut() throws Exception {
        assertThat(total(client, "/api/reservations?size=50"))
                .isEqualTo(compter("SELECT COUNT(*) FROM reservation_maison WHERE is_deleted = FALSE AND id_user = ?", idClient));
        for (String statut : new String[]{"EN_ATTENTE", "ANNULEE"}) {
            assertThat(total(client, "/api/reservations?size=50&statut=" + statut))
                    .as("statut " + statut)
                    .isEqualTo(compter("SELECT COUNT(*) FROM reservation_maison WHERE is_deleted = FALSE AND id_user = ? AND statut = ?",
                            idClient, statut));
        }
    }

    @Test
    void bailleur_reservations_perimetreEtStatut() throws Exception {
        String perimetre = """
                FROM reservation_maison r JOIN maison m ON m.id_maison = r.id_maison JOIN cour c ON c.id_cour = m.id_cour
                WHERE r.is_deleted = FALSE AND c.id_user = ?""";

        assertThat(total(bailleur, "/api/reservations?size=50")).isEqualTo(compter("SELECT COUNT(*) " + perimetre, idBailleur));
        assertThat(total(bailleur, "/api/reservations?size=50&statut=EN_ATTENTE"))
                .isEqualTo(compter("SELECT COUNT(*) " + perimetre + " AND r.statut = 'EN_ATTENTE'", idBailleur));
    }

    // ---------- Pagination ----------

    @Test
    void pagination_tailleDePageRespecteeEtTotalInchange() throws Exception {
        JsonNode page = lire(bailleur, "/api/echeances?type=LOCATION&size=1&page=0");
        assertThat(page.get("content").size()).isEqualTo(1);
        assertThat(page.get("page").get("totalElements").asInt())
                .isEqualTo(total(bailleur, "/api/echeances?type=LOCATION&size=50"));
        assertThat(page.get("page").get("totalPages").asInt()).isEqualTo(page.get("page").get("totalElements").asInt());
    }

    // ---------- Outils ----------

    private int total(Cookie utilisateur, String url) throws Exception {
        return lire(utilisateur, url).get("page").get("totalElements").asInt();
    }

    private JsonNode lire(Cookie utilisateur, String url) throws Exception {
        String corps = mockMvc.perform(get(url).cookie(utilisateur))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return json.readTree(corps).get("data");
    }

    private int compter(String sql, Object... parametres) {
        return jdbc.queryForObject(sql, Integer.class, parametres);
    }

    private int idUser(String email) {
        return jdbc.queryForObject("SELECT id_user FROM users WHERE email = ?", Integer.class, email);
    }

    private Cookie connexion(String email) throws Exception {
        return mockMvc.perform(post("/api/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}""".formatted(email, MOT_DE_PASSE_SEED)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getCookie("access_token");
    }
}
