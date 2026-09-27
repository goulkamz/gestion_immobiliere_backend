package com.immobilier.gestionImmobiliere.modules;

import com.immobilier.gestionImmobiliere.support.AbstractIntegrationTest;
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
 * Operations d'ecriture metier et regles de gestion (RG2/RG3), verifiees jusqu'en base.
 * Donnees du seed : maisons 1=A et 4=C louees, 2=B (reservation EN_ATTENTE a J+5..J+10),
 * 3=E et 5=D disponibles ; mandats 1 (cour 1) et 2 (cour 2) actifs ; echeances 1
 * (EN_RETARD) et 3 du contrat 1, 5 du contrat 2, 6 du contrat 3.
 * Chaque test travaille sur ses propres maisons/echeances pour rester independant.
 */
class EcritureMetierIntegrationTest extends AbstractIntegrationTest {

    private static final int CLIENT2 = 6;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    private Cookie agent;
    private Cookie client;

    @BeforeEach
    void connexions() throws Exception {
        agent = connexion("agent@gestimmo.test");
        client = connexion("client2@gestimmo.test");
    }

    // ---------- Reservations -> contrat de location ----------

    @Test
    void parcoursComplet_reservationConfirmationConversionPaiementResiliation() throws Exception {
        LocalDateTime debut = LocalDateTime.now().plusDays(30).truncatedTo(ChronoUnit.SECONDS);
        appel(client, post("/api/reservations"), """
                {"idMaison":5,"dateDebut":"%s","dateFin":"%s"}""".formatted(debut, debut.plusMonths(6)),
                status().isCreated());
        assertThat(statutMaison(5)).isEqualTo("RESERVEE");
        int idResa = jdbc.queryForObject(
                "SELECT MAX(id_reservation) FROM reservation_maison WHERE id_maison = 5", Integer.class);

        appel(agent, patch("/api/reservations/" + idResa + "/confirmer"), null, status().isOk());
        assertThat(statutReservation(idResa)).isEqualTo("CONFIRMEE");

        appel(agent, patch("/api/reservations/" + idResa + "/convertir")
                .param("montantLoyer", "55000").param("typeContrat", "HABITATION"), null, status().isCreated());
        assertThat(statutReservation(idResa)).isEqualTo("CONVERTIE");
        assertThat(statutMaison(5)).isEqualTo("LOUEE");
        int idContrat = jdbc.queryForObject(
                "SELECT id_contra_location FROM contra_location WHERE id_maison = 5 AND statut = 'ACTIF' AND id_user = ?",
                Integer.class, CLIENT2);
        assertThat(echeancesActives(idContrat)).as("echeances generees a la creation du contrat").isPositive();

        int premiereEcheance = jdbc.queryForObject("""
                SELECT id_echeance FROM echeance_loyer WHERE entite_echeance_type = 'LOCATION'
                AND entite_echeance_id = ? AND is_deleted = FALSE ORDER BY date_echeance LIMIT 1""",
                Integer.class, idContrat);
        payer(premiereEcheance, "55000", status().isCreated());

        appel(agent, patch("/api/contrats-location/" + idContrat + "/resilier"), """
                {"etatDesLieuxSortie":"RAS","coutReparation":0}""", status().isOk());
        assertThat(jdbc.queryForObject("SELECT statut FROM contra_location WHERE id_contra_location = ?",
                String.class, idContrat)).isEqualTo("RESILIE");
        assertThat(statutMaison(5)).isEqualTo("DISPONIBLE");
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM echeance_loyer WHERE entite_echeance_type = 'LOCATION'
                AND entite_echeance_id = ? AND is_deleted = FALSE AND statut <> 'PAYE'""", Integer.class, idContrat))
                .as("echeances futures non payees supprimees").isZero();
        assertThat(statutEcheance(premiereEcheance)).as("echeance payee conservee").isEqualTo("PAYE");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM decompte_sortie WHERE id_contrat_location = ?",
                Integer.class, idContrat)).isEqualTo(1);

        appel(agent, patch("/api/contrats-location/" + idContrat + "/resilier"), """
                {"etatDesLieuxSortie":"RAS","coutReparation":0}""", status().isConflict());
    }

    @Test
    void reservation_chevauchantUneReservationExistante_refusee() throws Exception {
        LocalDateTime debut = LocalDateTime.now().plusDays(6).truncatedTo(ChronoUnit.SECONDS);
        appel(client, post("/api/reservations"), """
                {"idMaison":2,"dateDebut":"%s","dateFin":"%s"}""".formatted(debut, debut.plusDays(2)),
                status().isConflict());
        assertThat(statutMaison(2)).isEqualTo("DISPONIBLE");
    }

    @Test
    void reservation_maisonLouee_refusee() throws Exception {
        LocalDateTime debut = LocalDateTime.now().plusDays(40).truncatedTo(ChronoUnit.SECONDS);
        appel(client, post("/api/reservations"), """
                {"idMaison":1,"dateDebut":"%s","dateFin":"%s"}""".formatted(debut, debut.plusDays(5)),
                status().isConflict());
    }

    @Test
    void reservation_finAvantDebut_refusee() throws Exception {
        LocalDateTime debut = LocalDateTime.now().plusDays(40).truncatedTo(ChronoUnit.SECONDS);
        appel(client, post("/api/reservations"), """
                {"idMaison":3,"dateDebut":"%s","dateFin":"%s"}""".formatted(debut, debut.minusDays(1)),
                status().isBadRequest());
        assertThat(statutMaison(3)).isEqualTo("DISPONIBLE");
    }

    @Test
    void reservation_annulee_libereLaMaison() throws Exception {
        LocalDateTime debut = LocalDateTime.now().plusDays(50).truncatedTo(ChronoUnit.SECONDS);
        appel(client, post("/api/reservations"), """
                {"idMaison":3,"dateDebut":"%s","dateFin":"%s"}""".formatted(debut, debut.plusDays(10)),
                status().isCreated());
        int idResa = jdbc.queryForObject(
                "SELECT MAX(id_reservation) FROM reservation_maison WHERE id_maison = 3", Integer.class);

        appel(client, patch("/api/reservations/" + idResa + "/annuler"), null, status().isOk());
        assertThat(statutReservation(idResa)).isEqualTo("ANNULEE");
        assertThat(statutMaison(3)).isEqualTo("DISPONIBLE");

        appel(agent, patch("/api/reservations/" + idResa + "/confirmer"), null, status().isConflict());
    }

    // ---------- Paiements ----------

    @Test
    void paiement_soldeUneEcheance_puisRefuseLeDoublePaiement() throws Exception {
        payer(1, "150000", status().isCreated());
        assertThat(statutEcheance(1)).isEqualTo("PAYE");
        assertThat(montantPaye(1)).isEqualByComparingTo("150000");

        payer(1, "150000", status().isConflict());
    }

    @Test
    void paiement_superieurAuResteDu_refuse() throws Exception {
        payer(3, "200000", status().isBadRequest());
        assertThat(montantPaye(3)).isEqualByComparingTo("0");
    }

    @Test
    void paiement_partielPuisPaiementGroupeDePlusieursEcheances() throws Exception {
        payer(5, "40000", status().isCreated());
        assertThat(montantPaye(5)).isEqualByComparingTo("40000");
        assertThat(statutEcheance(5)).isNotEqualTo("PAYE");

        appel(agent, post("/api/paiements"), """
                {"montantPaiement":130000,"modePaiement":"ESPECES","idEcheances":[5,6]}""", status().isCreated());
        assertThat(statutEcheance(5)).isEqualTo("PAYE");
        assertThat(statutEcheance(6)).isEqualTo("PAYE");
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(DISTINCT id_paiement) FROM paiement_echeance WHERE id_echeance = 5""", Integer.class))
                .as("echeance 5 liee aux deux paiements").isEqualTo(2);
    }

    @Test
    void paiement_parUnClient_refuse() throws Exception {
        appel(client, post("/api/paiements"), """
                {"montantPaiement":100000,"modePaiement":"ESPECES","idEcheances":[5]}""", status().isForbidden());
    }

    @Test
    void paiementPartiel_surEcheanceEnRetard_laLaisseEnRetardJusquAuSoldePenaliteComprise() throws Exception {
        int id = jdbc.queryForObject("""
                INSERT INTO echeance_loyer (entite_echeance_type, entite_echeance_id, date_echeance,
                    montant_du, montant_paye, penalite, statut, created_at)
                VALUES ('LOCATION', 1, CURRENT_DATE - 60, 100000, 0, 5000, 'EN_RETARD', NOW())
                RETURNING id_echeance""", Integer.class);

        payer(id, "60000", status().isCreated());
        assertThat(statutEcheance(id)).isEqualTo("EN_RETARD");

        payer(id, "40000", status().isCreated());
        assertThat(statutEcheance(id)).as("loyer regle mais penalite due").isEqualTo("EN_RETARD");

        payer(id, "5000", status().isCreated());
        assertThat(statutEcheance(id)).isEqualTo("PAYE");
    }

    // ---------- Dates (RG2) ----------

    @Test
    void mandat_finAvantDebut_refuse() throws Exception {
        int avant = jdbc.queryForObject("SELECT COUNT(*) FROM contrat_mandat", Integer.class);
        appel(agent, post("/api/contrats-mandat"), """
                {"idCour":1,"idAgent":2,"dateDebut":"2027-01-01T00:00:00","dateFin":"2026-01-01T00:00:00",
                 "typeMandat":"GESTION","commission":10}""", status().isBadRequest());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM contrat_mandat", Integer.class)).isEqualTo(avant);
    }

    @Test
    void contratLocation_sortieAvantEntree_refuse() throws Exception {
        appel(agent, post("/api/contrats-location"), """
                {"idLocataire":6,"idMaison":2,"dateEntree":"2027-01-01T00:00:00",
                 "dateSortie":"2026-01-01T00:00:00","montantLoyer":60000}""", status().isBadRequest());
    }

    @Test
    void resiliation_sortieAvantEntree_refusee() throws Exception {
        appel(agent, patch("/api/contrats-location/1/resilier"), """
                {"etatDesLieuxSortie":"RAS","coutReparation":0,"dateSortie":"2020-01-01T00:00:00"}""",
                status().isBadRequest());
        assertThat(jdbc.queryForObject("SELECT statut FROM contra_location WHERE id_contra_location = 1",
                String.class)).isEqualTo("ACTIF");
        assertThat(statutMaison(1)).isEqualTo("LOUEE");
    }

    // ---------- Mandats ----------

    @Test
    void mandat_unSeulActifParCour() throws Exception {
        appel(agent, post("/api/contrats-mandat"), mandat(1), status().isCreated());
        int idNouveau = dernierMandat(1);
        assertThat(statutMandat(idNouveau)).isEqualTo("EN_ATTENTE");

        appel(agent, patch("/api/contrats-mandat/" + idNouveau + "/activer"), null, status().isConflict());
        assertThat(statutMandat(idNouveau)).isEqualTo("EN_ATTENTE");
    }

    @Test
    void mandat_activableApresResiliationDuPrecedent() throws Exception {
        appel(agent, patch("/api/contrats-mandat/2/resilierContratLocation"), """
                {"motifResiliation":"Fin de collaboration"}""", status().isOk());
        assertThat(statutMandat(2)).isEqualTo("RESILIE");

        appel(agent, post("/api/contrats-mandat"), mandat(2), status().isCreated());
        int idNouveau = dernierMandat(2);
        appel(agent, patch("/api/contrats-mandat/" + idNouveau + "/activer"), null, status().isOk());
        assertThat(statutMandat(idNouveau)).isEqualTo("ACTIF");
    }

    // ---------- Outils ----------

    private String mandat(int idCour) {
        LocalDateTime debut = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        return """
                {"idCour":%d,"idAgent":2,"dateDebut":"%s","dateFin":"%s","typeMandat":"GESTION",
                 "commission":10,"modeFacturation":"MENSUEL"}""".formatted(idCour, debut, debut.plusYears(1));
    }

    private void payer(int idEcheance, String montant, ResultMatcher attendu) throws Exception {
        appel(agent, post("/api/paiements"), """
                {"montantPaiement":%s,"modePaiement":"ESPECES","idEcheances":[%d]}""".formatted(montant, idEcheance),
                attendu);
    }

    private void appel(Cookie utilisateur, MockHttpServletRequestBuilder requete, String json, ResultMatcher attendu)
            throws Exception {
        requete.cookie(utilisateur);
        if (json != null) requete.contentType(MediaType.APPLICATION_JSON).content(json);
        mockMvc.perform(requete).andExpect(attendu);
    }

    private Cookie connexion(String email) throws Exception {
        return mockMvc.perform(post("/api/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}""".formatted(email, MOT_DE_PASSE_SEED)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getCookie("access_token");
    }

    private String statutMaison(int id) {
        return jdbc.queryForObject("SELECT statut FROM maison WHERE id_maison = ?", String.class, id);
    }

    private String statutReservation(int id) {
        return jdbc.queryForObject("SELECT statut FROM reservation_maison WHERE id_reservation = ?", String.class, id);
    }

    private String statutEcheance(int id) {
        return jdbc.queryForObject("SELECT statut FROM echeance_loyer WHERE id_echeance = ?", String.class, id);
    }

    private BigDecimal montantPaye(int id) {
        return jdbc.queryForObject("SELECT montant_paye FROM echeance_loyer WHERE id_echeance = ?", BigDecimal.class, id);
    }

    private int echeancesActives(int idContrat) {
        return jdbc.queryForObject("""
                SELECT COUNT(*) FROM echeance_loyer WHERE entite_echeance_type = 'LOCATION'
                AND entite_echeance_id = ? AND is_deleted = FALSE""", Integer.class, idContrat);
    }

    private String statutMandat(int id) {
        return jdbc.queryForObject("SELECT statut FROM contrat_mandat WHERE id_mandat = ?", String.class, id);
    }

    private int dernierMandat(int idCour) {
        return jdbc.queryForObject("SELECT MAX(id_mandat) FROM contrat_mandat WHERE id_cour = ?", Integer.class, idCour);
    }
}
