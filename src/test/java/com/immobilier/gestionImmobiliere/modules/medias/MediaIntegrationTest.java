package com.immobilier.gestionImmobiliere.modules.medias;

import com.immobilier.gestionImmobiliere.support.AbstractIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Regressions sur l'ordre des medias (contrainte uk_media_entite_ordre) : chaque
 * test cible sa propre maison du seed pour rester independant des autres.
 */
class MediaIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    private Cookie agent;

    @BeforeEach
    void connexionAgent() throws Exception {
        agent = mockMvc.perform(post("/api/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"agent@gestimmo.test","password":"%s"}
                                """.formatted(MOT_DE_PASSE_SEED)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getCookie("access_token");
    }

    @Test
    void uploadApresSuppression_reussit() throws Exception {
        int premier = uploader(3);
        mockMvc.perform(delete("/api/medias/" + premier).cookie(agent))
                .andExpect(status().isOk());

        uploader(3);
    }

    @Test
    void reorder_permuteDeuxMedias() throws Exception {
        int a = uploader(4);
        int b = uploader(4);

        mockMvc.perform(patch("/api/medias/reorder").cookie(agent)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idsMediaOrdonnes\":[%d,%d]}".formatted(b, a)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/public/medias").param("entiteType", "MAISON").param("entiteId", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].idMedia").value(b))
                .andExpect(jsonPath("$.data[1].idMedia").value(a));
    }

    @Test
    void photo_servieParLeBackend_avecCache_puisIntrouvableApresSuppression() throws Exception {
        int id = uploader(5);
        byte[] original = new ClassPathResource("image/etoile.png").getContentAsByteArray();

        mockMvc.perform(get("/api/public/medias").param("entiteType", "MAISON").param("entiteId", "5"))
                .andExpect(jsonPath("$.data[?(@.idMedia == %d)].url".formatted(id))
                        .value("https://api.test/api/public/medias/%d/fichier".formatted(id)))
                .andExpect(jsonPath("$.data[?(@.idMedia == %d)].urlThumbnail".formatted(id))
                        .value("https://api.test/api/public/medias/%d/miniature".formatted(id)));

        byte[] servi = mockMvc.perform(get("/api/public/medias/" + id + "/fichier"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(header().string("Cache-Control", containsString("max-age=604800")))
                .andExpect(header().string("Cache-Control", containsString("public")))
                .andExpect(header().string("Cache-Control", not(containsString("no-store"))))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(servi).isEqualTo(original);

        mockMvc.perform(get("/api/public/medias/" + id + "/miniature"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"));

        mockMvc.perform(delete("/api/medias/" + id).cookie(agent)).andExpect(status().isOk());
        mockMvc.perform(get("/api/public/medias/" + id + "/fichier")).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/public/medias/999999/fichier")).andExpect(status().isNotFound());
    }

    @Test
    void bienService_accepteDesMedias_servisPubliquement_etSupprimesEnCascade() throws Exception {
        int idBien = jdbc.queryForObject("SELECT min(id_bien_service) FROM bien_service", Integer.class);
        int id = uploaderPour("BIEN_SERVICE", idBien);

        mockMvc.perform(get("/api/public/medias").param("entiteType", "BIEN_SERVICE").param("entiteId", String.valueOf(idBien)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.idMedia == %d)].url".formatted(id))
                        .value("https://api.test/api/public/medias/%d/fichier".formatted(id)));
        mockMvc.perform(get("/api/public/medias/" + id + "/fichier"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("public")));

        // Suppression logique du bien : ses medias suivent (trigger), puis restauration
        jdbc.update("UPDATE bien_service SET is_deleted = true WHERE id_bien_service = ?", idBien);
        assertThat(jdbc.queryForObject("SELECT is_deleted FROM medias WHERE id_media = ?", Boolean.class, id)).isTrue();
        jdbc.update("UPDATE bien_service SET is_deleted = false WHERE id_bien_service = ?", idBien);
        assertThat(jdbc.queryForObject("SELECT is_deleted FROM medias WHERE id_media = ?", Boolean.class, id)).isFalse();
    }

    @Test
    void offre_accepteDesMedias_prives_lisiblesParLAgenceSeulement() throws Exception {
        mockMvc.perform(post("/api/public/offres").contentType(MediaType.APPLICATION_JSON).content("""
                        {"nomComplet":"Proprio Medias","email":"proprio.medias@test.com","typeOffre":"MAISON",
                         "titre":"Cour avec photos","adresse":"Secteur 9"}"""))
                .andExpect(status().isCreated());
        int idOffre = jdbc.queryForObject("SELECT id_offre FROM offre WHERE email = 'proprio.medias@test.com'", Integer.class);
        int id = uploaderPour("OFFRE", idOffre);
        byte[] original = new ClassPathResource("image/etoile.png").getContentAsByteArray();

        // Stocke dans le bucket prive, URL pointant vers la route authentifiee
        mockMvc.perform(get("/api/medias").cookie(agent).param("entiteType", "OFFRE").param("entiteId", String.valueOf(idOffre)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.idMedia == %d)].url".formatted(id))
                        .value("https://api.test/api/medias/%d/fichier".formatted(id)))
                .andExpect(jsonPath("$.data[?(@.idMedia == %d)].urlThumbnail".formatted(id))
                        .value("https://api.test/api/medias/%d/miniature".formatted(id)));

        // Public : liste refusee, fichier et miniature introuvables
        mockMvc.perform(get("/api/public/medias").param("entiteType", "OFFRE").param("entiteId", String.valueOf(idOffre)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/public/medias/" + id + "/fichier")).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/public/medias/" + id + "/miniature")).andExpect(status().isNotFound());

        // Route authentifiee : agent oui (sans cache partage), anonyme 401, client 403
        byte[] servi = mockMvc.perform(get("/api/medias/" + id + "/fichier").cookie(agent))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(servi).isEqualTo(original);
        mockMvc.perform(get("/api/medias/" + id + "/miniature").cookie(agent)).andExpect(status().isOk());
        mockMvc.perform(get("/api/medias/" + id + "/fichier")).andExpect(status().isUnauthorized());
        Cookie client = mockMvc.perform(post("/api/auth/signin").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"client@gestimmo.test\",\"password\":\"%s\"}".formatted(MOT_DE_PASSE_SEED)))
                .andExpect(status().isOk()).andReturn().getResponse().getCookie("access_token");
        mockMvc.perform(get("/api/medias/" + id + "/fichier").cookie(client)).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/medias").cookie(client).param("entiteType", "OFFRE").param("entiteId", String.valueOf(idOffre)))
                .andExpect(status().isForbidden());

        // Suppression logique de l'offre : ses medias suivent (trigger)
        jdbc.update("UPDATE offre SET is_deleted = true WHERE id_offre = ?", idOffre);
        assertThat(jdbc.queryForObject("SELECT is_deleted FROM medias WHERE id_media = ?", Boolean.class, id)).isTrue();
    }

    private int uploader(int idMaison) throws Exception {
        return uploaderPour("MAISON", idMaison);
    }

    private int uploaderPour(String entiteType, int entiteId) throws Exception {
        MockMultipartFile image = new MockMultipartFile("fichier", "etoile.png", "image/png",
                new ClassPathResource("image/etoile.png").getInputStream());

        String body = mockMvc.perform(multipart("/api/medias").file(image).cookie(agent)
                        .param("entiteType", entiteType)
                        .param("entiteId", String.valueOf(entiteId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.data.idMedia");
    }
}
