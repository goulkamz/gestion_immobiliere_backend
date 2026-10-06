package com.immobilier.gestionImmobiliere.modules.annonces;

import com.immobilier.gestionImmobiliere.support.AbstractIntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Annonces (agence) et formulaires publics : demandes, offres, contacts. */
class AnnoncesIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    private Cookie agent;
    private Cookie admin;
    private Cookie client;

    @BeforeEach
    void connexions() throws Exception {
        agent = connexion("agent@gestimmo.test");
        admin = connexion("admin@gestimmo.test");
        client = connexion("client@gestimmo.test");
    }

    @Test
    void annonce_cycleDeVie_creationSuspensionSuppression() throws Exception {
        String expiration = LocalDateTime.now().plusDays(30).truncatedTo(ChronoUnit.SECONDS).toString();
        mockMvc.perform(post("/api/annonces").cookie(agent).contentType(MediaType.APPLICATION_JSON).content("""
                        {"titre":"Villa test integration","description":"3 pieces","typeAnnonce":"LOCATION",
                         "dateExpiration":"%s","prix":150000,"localisation":"Ouaga 2000"}""".formatted(expiration)))
                .andExpect(status().isCreated());
        int id = jdbc.queryForObject("SELECT id_annonce FROM annonce WHERE titre = 'Villa test integration'", Integer.class);

        mockMvc.perform(get("/api/public/annonces/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.statut").value("ACTIVE"));

        mockMvc.perform(patch("/api/annonces/" + id + "/statut").cookie(agent)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"statut\":\"SUSPENDUE\"}"))
                .andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT statut FROM annonce WHERE id_annonce = ?", String.class, id))
                .isEqualTo("SUSPENDUE");

        mockMvc.perform(delete("/api/annonces/" + id).cookie(agent)).andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/annonces/" + id).cookie(admin)).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT is_deleted FROM annonce WHERE id_annonce = ?", Boolean.class, id))
                .as("soft delete").isTrue();
        mockMvc.perform(get("/api/public/annonces/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void annonce_expirationAvantPublication_refusee() throws Exception {
        mockMvc.perform(post("/api/annonces").cookie(agent).contentType(MediaType.APPLICATION_JSON).content("""
                        {"titre":"Annonce deja expiree","dateExpiration":"2020-01-01T00:00:00"}"""))
                .andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM annonce WHERE titre = 'Annonce deja expiree'",
                Integer.class)).isZero();
    }

    @Test
    void annonce_creationReserveeALAgence() throws Exception {
        mockMvc.perform(post("/api/annonces").cookie(client).contentType(MediaType.APPLICATION_JSON).content("""
                        {"titre":"Tentative client","dateExpiration":"2099-01-01T00:00:00"}"""))
                .andExpect(status().isForbidden());
    }

    @Test
    void formulairesPublics_deposesAnonymement_puisTraitesParLAgence() throws Exception {
        mockMvc.perform(post("/api/public/demandes").contentType(MediaType.APPLICATION_JSON).content("""
                        {"nomComplet":"Demandeur Test","email":"demandeur@test.com","telephone":"70111111",
                         "typeBien":"MAISON","localisationSouhaite":"Secteur 15","budgetMax":80000}"""))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/public/offres").contentType(MediaType.APPLICATION_JSON).content("""
                        {"nomComplet":"Proprietaire Test","email":"proprio@test.com","typeOffre":"MAISON",
                         "titre":"Cour a confier","adresse":"Secteur 12"}"""))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/public/contacts").contentType(MediaType.APPLICATION_JSON).content("""
                        {"nomComplet":"Visiteur Test","email":"visiteur@test.com","sujet":"Question",
                         "message":"Le studio est-il libre ?"}"""))
                .andExpect(status().isCreated());

        int demande = jdbc.queryForObject("SELECT id_demande FROM demande WHERE email = 'demandeur@test.com'", Integer.class);
        int offre = jdbc.queryForObject("SELECT id_offre FROM offre WHERE email = 'proprio@test.com'", Integer.class);
        int contact = jdbc.queryForObject("SELECT id_contact FROM contact WHERE email = 'visiteur@test.com'", Integer.class);

        mockMvc.perform(patch("/api/demandes/" + demande + "/statut").cookie(agent)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"statut\":\"EN_COURS\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/offres/" + offre + "/statut").cookie(agent)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"statut\":\"SUSPENDUE\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/contacts/" + contact + "/statut").cookie(agent)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"statut\":\"TRAITE\"}"))
                .andExpect(status().isOk());

        assertThat(jdbc.queryForObject("SELECT statut FROM demande WHERE id_demande = ?", String.class, demande))
                .isEqualTo("EN_COURS");
        assertThat(jdbc.queryForObject("SELECT statut FROM offre WHERE id_offre = ?", String.class, offre))
                .isEqualTo("SUSPENDUE");
        assertThat(jdbc.queryForObject("SELECT statut FROM contact WHERE id_contact = ?", String.class, contact))
                .isEqualTo("TRAITE");
    }

    @Test
    void listeOffres_filtreParStatut() throws Exception {
        mockMvc.perform(post("/api/public/offres").contentType(MediaType.APPLICATION_JSON).content("""
                        {"nomComplet":"Proprietaire Filtre","email":"filtre@test.com","typeOffre":"MAISON",
                         "titre":"Offre filtre","adresse":"Secteur 3"}"""))
                .andExpect(status().isCreated());
        int offre = jdbc.queryForObject("SELECT id_offre FROM offre WHERE email = 'filtre@test.com'", Integer.class);
        mockMvc.perform(patch("/api/offres/" + offre + "/statut").cookie(agent)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"statut\":\"SUSPENDUE\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/offres?statut=SUSPENDUE").cookie(agent))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[?(@.statut != 'SUSPENDUE')]").isEmpty())
                .andExpect(jsonPath("$.data.content[?(@.email == 'filtre@test.com')]").isNotEmpty());
        mockMvc.perform(get("/api/offres?statut=ACTIVE").cookie(agent))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[?(@.email == 'filtre@test.com')]").isEmpty());
    }

    @Test
    void formulairePublic_emailInvalide_refuse() throws Exception {
        mockMvc.perform(post("/api/public/contacts").contentType(MediaType.APPLICATION_JSON).content("""
                        {"nomComplet":"X","email":"pas-un-email","message":"test"}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listesDesFormulaires_reserveesALAgence() throws Exception {
        for (String url : new String[]{"/api/demandes", "/api/offres", "/api/contacts"}) {
            mockMvc.perform(get(url)).andExpect(status().isUnauthorized());
            mockMvc.perform(get(url).cookie(client)).andExpect(status().isForbidden());
            mockMvc.perform(get(url).cookie(agent)).andExpect(status().isOk());
        }
    }

    @Test
    void temoignages_publics_filtresParFlagActif_etTriesParDateDecroissante() throws Exception {
        jdbc.update("""
                INSERT INTO temoignage (nom_auteur, role, texte, note, date_temoignage) VALUES
                ('Ancien Temoin', 'LOCATAIRE', 'Texte ancien', NULL, '2026-01-10'),
                ('Recent Temoin', 'PROPRIETAIRE', 'Texte recent', 5, '2026-08-12')""");
        int ancien = jdbc.queryForObject("SELECT id_temoignage FROM temoignage WHERE nom_auteur = 'Ancien Temoin'", Integer.class);

        // Public (sans cookie), tri par date decroissante, flagActif non expose, note/photoUrl absents si null
        mockMvc.perform(get("/api/temoignages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("TEMOIGNAGE_LIST"))
                .andExpect(jsonPath("$.data[0].nomAuteur").value("Recent Temoin"))
                .andExpect(jsonPath("$.data[0].role").value("PROPRIETAIRE"))
                .andExpect(jsonPath("$.data[0].note").value(5))
                .andExpect(jsonPath("$.data[0].date").value("2026-08-12"))
                .andExpect(jsonPath("$.data[0].flagActif").doesNotExist())
                .andExpect(jsonPath("$.data[1].nomAuteur").value("Ancien Temoin"))
                .andExpect(jsonPath("$.data[1].note").doesNotExist());

        // Seul un admin peut desactiver
        String corps = "{\"flagActif\":false}";
        mockMvc.perform(patch("/api/admin/temoignages/" + ancien + "/statut")
                        .contentType(MediaType.APPLICATION_JSON).content(corps))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(patch("/api/admin/temoignages/" + ancien + "/statut").cookie(agent)
                        .contentType(MediaType.APPLICATION_JSON).content(corps))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/admin/temoignages/" + ancien + "/statut").cookie(admin)
                        .contentType(MediaType.APPLICATION_JSON).content(corps))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/temoignages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.nomAuteur == 'Ancien Temoin')]").isEmpty());

        mockMvc.perform(patch("/api/admin/temoignages/999999/statut").cookie(admin)
                        .contentType(MediaType.APPLICATION_JSON).content(corps))
                .andExpect(status().isNotFound());
    }

    @Test
    void temoignage_creation_parAgent_publieImmediatement() throws Exception {
        String corps = """
                {"nomAuteur":"Auteur Cree","role":"LOCATAIRE","texte":"Tres bon service","note":4}""";

        mockMvc.perform(post("/api/admin/temoignages").contentType(MediaType.APPLICATION_JSON).content(corps))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/admin/temoignages").cookie(client).contentType(MediaType.APPLICATION_JSON).content(corps))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/admin/temoignages").cookie(agent).contentType(MediaType.APPLICATION_JSON).content(corps))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("TEMOIGNAGE_CREATED"))
                .andExpect(jsonPath("$.data.nomAuteur").value("Auteur Cree"))
                .andExpect(jsonPath("$.data.date").value(java.time.LocalDate.now().toString()));

        mockMvc.perform(get("/api/temoignages"))
                .andExpect(jsonPath("$.data[?(@.nomAuteur == 'Auteur Cree')]").isNotEmpty());
        // Nettoyage : la date du jour le placerait en tete de liste dans les autres tests
        jdbc.update("DELETE FROM temoignage WHERE nom_auteur = 'Auteur Cree'");

        // Validation : role manquant, note hors 1..5, texte vide
        for (String invalide : new String[]{
                "{\"nomAuteur\":\"X\",\"texte\":\"t\"}",
                "{\"nomAuteur\":\"X\",\"role\":\"AUTRE\",\"texte\":\"t\",\"note\":6}",
                "{\"nomAuteur\":\"X\",\"role\":\"AUTRE\",\"texte\":\" \"}"}) {
            mockMvc.perform(post("/api/admin/temoignages").cookie(admin).contentType(MediaType.APPLICATION_JSON).content(invalide))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void temoignage_depotPublic_valideEtRestreint() throws Exception {
        // Depot anonyme valide : enregistre inactif (a valider) ; photoUrl/date fournis par le client sont ignores
        mockMvc.perform(post("/api/temoignages").contentType(MediaType.APPLICATION_JSON).content("""
                        {"nomAuteur":"  Visiteur Public ","role":"LOCATAIRE","texte":"Un service vraiment sérieux.",
                         "note":5,"photoUrl":"http://evil.example/x.png","date":"2000-01-01"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.nomAuteur").value("Visiteur Public"))
                .andExpect(jsonPath("$.data.date").value(java.time.LocalDate.now().toString()))
                .andExpect(jsonPath("$.data.photoUrl").doesNotExist())
                .andExpect(jsonPath("$.data.flagActif").doesNotExist());
        // Invisible du public tant qu'un admin ne l'a pas active
        mockMvc.perform(get("/api/temoignages"))
                .andExpect(jsonPath("$.data[?(@.nomAuteur == 'Visiteur Public')]").isEmpty());
        int depose = jdbc.queryForObject("SELECT id_temoignage FROM temoignage WHERE nom_auteur = 'Visiteur Public'", Integer.class);
        mockMvc.perform(get("/api/admin/temoignages?flagActif=false").cookie(agent))
                .andExpect(jsonPath("$.data.content[?(@.nomAuteur == 'Visiteur Public')]").isNotEmpty());
        mockMvc.perform(patch("/api/admin/temoignages/" + depose + "/statut").cookie(admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"flagActif\":true}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/temoignages"))
                .andExpect(jsonPath("$.data[?(@.nomAuteur == 'Visiteur Public')]").isNotEmpty());
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM temoignage WHERE nom_auteur = 'Visiteur Public' AND photo_url IS NULL",
                Integer.class)).isEqualTo(1);

        // Piege a robots : succes apparent, rien d'enregistre
        mockMvc.perform(post("/api/temoignages").contentType(MediaType.APPLICATION_JSON).content("""
                        {"nomAuteur":"Robot","role":"AUTRE","texte":"Texte de robot automatique","siteWeb":"http://spam"}"""))
                .andExpect(status().isCreated());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM temoignage WHERE nom_auteur = 'Robot'", Integer.class)).isZero();

        // Refus : role AGENCE, HTML, lien, texte trop court, note hors bornes
        for (String invalide : new String[]{
                "{\"nomAuteur\":\"X\",\"role\":\"AGENCE\",\"texte\":\"Texte assez long ici\"}",
                "{\"nomAuteur\":\"X\",\"role\":\"AUTRE\",\"texte\":\"<script>alert(1)</script> bravo\"}",
                "{\"nomAuteur\":\"<b>X</b>\",\"role\":\"AUTRE\",\"texte\":\"Texte assez long ici\"}",
                "{\"nomAuteur\":\"X\",\"role\":\"AUTRE\",\"texte\":\"Visitez https://spam.example maintenant\"}",
                "{\"nomAuteur\":\"X\",\"role\":\"AUTRE\",\"texte\":\"Allez sur www.spam.example vite\"}",
                "{\"nomAuteur\":\"X\",\"role\":\"AUTRE\",\"texte\":\"court\"}",
                "{\"nomAuteur\":\"X\",\"role\":\"AUTRE\",\"texte\":\"Texte assez long ici\",\"note\":9}"}) {
            mockMvc.perform(post("/api/temoignages").contentType(MediaType.APPLICATION_JSON).content(invalide))
                    .andExpect(status().isBadRequest());
        }
        jdbc.update("DELETE FROM temoignage WHERE nom_auteur = 'Visiteur Public'");
    }

    @Test
    void temoignage_depotPublic_limiteParIp() throws Exception {
        String corps = """
                {"nomAuteur":"Limite","role":"AUTRE","texte":"Texte assez long pour passer"}""";
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/temoignages").contentType(MediaType.APPLICATION_JSON).content(corps))
                    .andExpect(status().isCreated());
        }
        mockMvc.perform(post("/api/temoignages").contentType(MediaType.APPLICATION_JSON).content(corps))
                .andExpect(status().isTooManyRequests());
        jdbc.update("DELETE FROM temoignage WHERE nom_auteur = 'Limite'");
    }

    @Test
    void temoignage_listeAdmin_incluDesactives_etReserveALAgence() throws Exception {
        jdbc.update("INSERT INTO temoignage (nom_auteur, role, texte, flag_actif) VALUES ('Desactive Liste', 'AUTRE', 'texte', false)");

        mockMvc.perform(get("/api/admin/temoignages")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/temoignages").cookie(client)).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/temoignages").cookie(agent))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[?(@.nomAuteur == 'Desactive Liste')].flagActif").value(false));
        mockMvc.perform(get("/api/temoignages"))
                .andExpect(jsonPath("$.data[?(@.nomAuteur == 'Desactive Liste')]").isEmpty());
        jdbc.update("DELETE FROM temoignage WHERE nom_auteur = 'Desactive Liste'");
    }

    private Cookie connexion(String email) throws Exception {
        return mockMvc.perform(post("/api/auth/signin").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, MOT_DE_PASSE_SEED)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getCookie("access_token");
    }
}
