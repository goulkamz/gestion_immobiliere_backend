package com.immobilier.gestionImmobiliere.modules.depenses;

import com.immobilier.gestionImmobiliere.support.AbstractIntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Devis de travaux soumis au bailleur : l'agence propose, le bailleur accepte, demande une renégociation
 * ou rejette ; un devis accepté donne lieu à une seule dépense, jamais supérieure au montant accepté.
 * Comme pour les dépenses, on crée un mandat de gestion actif sur la cour 3 du seed.
 */
class DevisIntegrationTest extends AbstractIntegrationTest {

    private static final int COUR = 3;
    private static final String AGENT = "agent@gestimmo.test";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    private final List<Integer> mandatsCrees = new ArrayList<>();
    private int derniereNotification;

    @BeforeEach
    void memoriserEtat() {
        derniereNotification = jdbc.queryForObject("SELECT COALESCE(MAX(id_notification), 0) FROM notification", Integer.class);
    }

    @AfterEach
    void nettoyer() {
        for (int idMandat : mandatsCrees) {
            jdbc.update("DELETE FROM depense_bien WHERE id_mandat = ?", idMandat);
            jdbc.update("DELETE FROM devis_travaux WHERE id_mandat = ?", idMandat);
            jdbc.update("DELETE FROM contrat_mandat WHERE id_mandat = ?", idMandat);
        }
        jdbc.update("DELETE FROM notification WHERE id_notification > ?", derniereNotification);
        mandatsCrees.clear();
    }

    @Test
    void devisAccepte_donneUneDepenseSansAttestationMemeAuDessusDuPlafond() throws Exception {
        int idMandat = preparerMandat();
        jdbc.update("UPDATE contrat_mandat SET plafond_depense_sans_accord = 20000 WHERE id_mandat = ?", idMandat);
        Cookie agent = connexion(AGENT);
        Cookie bailleur = connexion(emailProprietaire());
        int idDevis = creerDevis(agent, "150000", 3);

        assertThat(lienNotification("DEVIS_SOUMIS:" + idDevis + ":1")).isEqualTo("/espace-bailleur/devis?cible=" + idDevis);

        // Tant que le bailleur n'a pas répondu, aucune dépense ne peut être enregistrée
        mockMvc.perform(post("/api/devis/{id}/depense", idDevis).cookie(agent)).andExpect(status().isConflict());

        mockMvc.perform(patch("/api/devis/{id}/accepter", idDevis).cookie(bailleur).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"montant":150000,"nombreMois":3}"""))
                .andExpect(status().isOk());
        assertThat(statut(idDevis)).isEqualTo("ACCEPTE");
        assertThat(lienNotification("DEVIS_REPONSE:" + idDevis + ":1:ACCEPTE")).isEqualTo("/espace-agent/devis?cible=" + idDevis);

        // Le montant accepté est un maximum
        mockMvc.perform(post("/api/devis/{id}/depense", idDevis).cookie(agent).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"montant":150001}"""))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/devis/{id}/depense", idDevis).cookie(agent).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"montant":140000}"""))
                .andExpect(status().isCreated());

        assertThat(statut(idDevis)).isEqualTo("REALISE");
        assertThat(jdbc.queryForObject("SELECT montant FROM depense_bien WHERE id_devis = ?", BigDecimal.class, idDevis)).isEqualByComparingTo("140000");
        assertThat(jdbc.queryForObject("SELECT nombre_mois FROM depense_bien WHERE id_devis = ?", Integer.class, idDevis)).isEqualTo(3);
        assertThat(jdbc.queryForObject("SELECT accord_bailleur FROM depense_bien WHERE id_devis = ?", Boolean.class, idDevis)).isTrue();

        // Une seule dépense par devis
        mockMvc.perform(post("/api/devis/{id}/depense", idDevis).cookie(agent)).andExpect(status().isConflict());
    }

    @Test
    void renegociation_leDevisReviseRepartEnAttenteEtLeBailleurEstPrevenu() throws Exception {
        preparerMandat();
        Cookie agent = connexion(AGENT);
        Cookie bailleur = connexion(emailProprietaire());
        int idDevis = creerDevis(agent, "200000", 1);

        // Un commentaire est obligatoire
        mockMvc.perform(patch("/api/devis/{id}/renegocier", idDevis).cookie(bailleur).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"commentaire":" "}"""))
                .andExpect(status().isBadRequest());
        mockMvc.perform(patch("/api/devis/{id}/renegocier", idDevis).cookie(bailleur).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"commentaire":"Trop cher, demandez un autre artisan"}"""))
                .andExpect(status().isOk());
        assertThat(statut(idDevis)).isEqualTo("A_RENEGOCIER");

        // Le bailleur ne peut plus répondre tant que l'agence n'a pas révisé le devis
        mockMvc.perform(patch("/api/devis/{id}/accepter", idDevis).cookie(bailleur).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"montant":200000,"nombreMois":1}"""))
                .andExpect(status().isConflict());

        mockMvc.perform(patch("/api/devis/{id}", idDevis).cookie(agent).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"montant":160000,"nombreMois":2,"prestataire":"Ets Sawadogo"}"""))
                .andExpect(status().isOk());
        assertThat(statut(idDevis)).isEqualTo("EN_ATTENTE");
        assertThat(jdbc.queryForObject("SELECT nombre_soumissions FROM devis_travaux WHERE id_devis = ?", Integer.class, idDevis)).isEqualTo(2);
        assertThat(lienNotification("DEVIS_SOUMIS:" + idDevis + ":2")).isEqualTo("/espace-bailleur/devis?cible=" + idDevis);

        // L'accord ne vaut que pour le montant affiché : l'ancien montant est refusé
        mockMvc.perform(patch("/api/devis/{id}/accepter", idDevis).cookie(bailleur).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"montant":200000,"nombreMois":1}"""))
                .andExpect(status().isConflict());
        mockMvc.perform(patch("/api/devis/{id}/accepter", idDevis).cookie(bailleur).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"montant":160000,"nombreMois":2}"""))
                .andExpect(status().isOk());
        assertThat(statut(idDevis)).isEqualTo("ACCEPTE");
    }

    @Test
    void rejet_leDevisEstClosEtNeDonneAucuneDepense() throws Exception {
        preparerMandat();
        Cookie agent = connexion(AGENT);
        int idDevis = creerDevis(agent, "80000", 1);

        mockMvc.perform(patch("/api/devis/{id}/rejeter", idDevis).cookie(connexion(emailProprietaire())).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"commentaire":"Je ferai faire les travaux moi-même"}"""))
                .andExpect(status().isOk());

        assertThat(statut(idDevis)).isEqualTo("REJETE");
        mockMvc.perform(post("/api/devis/{id}/depense", idDevis).cookie(agent)).andExpect(status().isConflict());
        mockMvc.perform(patch("/api/devis/{id}", idDevis).cookie(agent).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"montant":50000}"""))
                .andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM depense_bien WHERE id_devis = ?", Integer.class, idDevis)).isZero();
    }

    @Test
    void retraitParLAgence_etAnnulationDeLaDepenseQuiClotLeDevis() throws Exception {
        preparerMandat();
        Cookie agent = connexion(AGENT);
        Cookie bailleur = connexion(emailProprietaire());

        int idRetire = creerDevis(agent, "50000", 1);
        mockMvc.perform(patch("/api/devis/{id}/annuler", idRetire).cookie(agent).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"commentaire":"Travaux reportés"}"""))
                .andExpect(status().isOk());
        assertThat(statut(idRetire)).isEqualTo("ANNULE");
        mockMvc.perform(patch("/api/devis/{id}/accepter", idRetire).cookie(bailleur).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"montant":50000,"nombreMois":1}"""))
                .andExpect(status().isConflict());

        int idDevis = creerDevis(agent, "50000", 1);
        mockMvc.perform(patch("/api/devis/{id}/accepter", idDevis).cookie(bailleur).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"montant":50000,"nombreMois":1}"""))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/devis/{id}/depense", idDevis).cookie(agent)).andExpect(status().isCreated());
        int idDepense = jdbc.queryForObject("SELECT id_depense FROM depense_bien WHERE id_devis = ?", Integer.class, idDevis);

        // La dépense issue d'un devis ne peut pas dépasser le montant accepté, ni changer d'étalement
        mockMvc.perform(patch("/api/depenses/{id}", idDepense).cookie(agent).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"montant":60000}"""))
                .andExpect(status().isConflict());
        mockMvc.perform(patch("/api/depenses/{id}", idDepense).cookie(agent).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombreMois":2}"""))
                .andExpect(status().isConflict());

        mockMvc.perform(patch("/api/depenses/{id}/annuler", idDepense).cookie(agent).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"motif":"Travaux abandonnés"}"""))
                .andExpect(status().isOk());
        assertThat(statut(idDevis)).as("le devis est clos avec sa dépense").isEqualTo("ANNULE");
    }

    @Test
    void acces_seulLeBailleurDeLaCourRepond_etSeuleLAgenceSoumet() throws Exception {
        preparerMandat();
        Cookie agent = connexion(AGENT);
        String proprietaire = emailProprietaire();
        Cookie bailleur = connexion(proprietaire);
        Cookie autreBailleur = connexion(jdbc.queryForObject(
                "SELECT u.email FROM users u JOIN role r ON r.id_role = u.id_role "
                        + "WHERE r.libelle_role = 'ROLE_BAILLEUR' AND u.email <> ? AND u.is_deleted = false ORDER BY u.id_user LIMIT 1",
                String.class, proprietaire));
        int idDevis = creerDevis(agent, "80000", 1);
        String accord = """
                {"montant":80000,"nombreMois":1}""";

        // Le bailleur ne soumet pas de devis ; l'agence ne répond pas à sa place
        mockMvc.perform(post("/api/devis").cookie(bailleur).contentType(MediaType.APPLICATION_JSON).content(corps("1000", 1)))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/devis/{id}/accepter", idDevis).cookie(agent).contentType(MediaType.APPLICATION_JSON).content(accord))
                .andExpect(status().isForbidden());

        // Un autre bailleur ne voit pas le devis et ne peut pas y répondre
        mockMvc.perform(get("/api/devis/{id}", idDevis).cookie(autreBailleur)).andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/devis/{id}/accepter", idDevis).cookie(autreBailleur).contentType(MediaType.APPLICATION_JSON).content(accord))
                .andExpect(status().isForbidden());
        assertThat(statut(idDevis)).isEqualTo("EN_ATTENTE");

        mockMvc.perform(get("/api/devis/{id}", idDevis).cookie(bailleur)).andExpect(status().isOk());
        mockMvc.perform(get("/api/devis").cookie(bailleur)).andExpect(status().isOk());
        mockMvc.perform(get("/api/devis").cookie(connexion("client2@gestimmo.test"))).andExpect(status().isForbidden());
    }

    @Test
    void sansMandatDeGestionActif_leDevisEstRefuse() throws Exception {
        mockMvc.perform(post("/api/devis").cookie(connexion(AGENT)).contentType(MediaType.APPLICATION_JSON).content(corps("30000", 1)))
                .andExpect(status().isConflict());
    }

    // ---------- Outils ----------

    private int preparerMandat() {
        int idAgent = jdbc.queryForObject("SELECT id_user FROM users WHERE email = ?", Integer.class, AGENT);
        int idMandat = jdbc.queryForObject(
                "INSERT INTO contrat_mandat (id_cour, id_user, date_debut, type_mandat, commission, mode_facturation, statut) "
                        + "VALUES (?, ?, ?, 'GESTION', 10, 'MENSUEL', 'ACTIF') RETURNING id_mandat",
                Integer.class, COUR, idAgent, Date.valueOf(LocalDate.now().withDayOfMonth(1)));
        mandatsCrees.add(idMandat);
        return idMandat;
    }

    private String emailProprietaire() {
        return jdbc.queryForObject("SELECT u.email FROM cour c JOIN users u ON u.id_user = c.id_user WHERE c.id_cour = ?", String.class, COUR);
    }

    private String corps(String montant, int nombreMois) {
        return """
                {"idCour":%d,"libelle":"Reprise de l'étanchéité du toit","categorie":"ETANCHEITE","prestataire":"Ets Kaboré","montant":%s,"nombreMois":%d}"""
                .formatted(COUR, montant, nombreMois);
    }

    private int creerDevis(Cookie agent, String montant, int nombreMois) throws Exception {
        mockMvc.perform(post("/api/devis").cookie(agent).contentType(MediaType.APPLICATION_JSON).content(corps(montant, nombreMois)))
                .andExpect(status().isCreated());
        return jdbc.queryForObject("SELECT MAX(id_devis) FROM devis_travaux", Integer.class);
    }

    private String statut(int idDevis) {
        return jdbc.queryForObject("SELECT statut FROM devis_travaux WHERE id_devis = ?", String.class, idDevis);
    }

    private String lienNotification(String cle) {
        return jdbc.queryForObject("SELECT MIN(lien) FROM notification WHERE id_notification > ? AND cle_alerte = ?",
                String.class, derniereNotification, cle);
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
