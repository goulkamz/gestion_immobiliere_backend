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

    private Cookie connexion(String email) throws Exception {
        return mockMvc.perform(post("/api/auth/signin").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, MOT_DE_PASSE_SEED)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getCookie("access_token");
    }
}
