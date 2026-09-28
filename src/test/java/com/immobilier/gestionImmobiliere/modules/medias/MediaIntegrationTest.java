package com.immobilier.gestionImmobiliere.modules.medias;

import com.immobilier.gestionImmobiliere.support.AbstractIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
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

    private int uploader(int idMaison) throws Exception {
        MockMultipartFile image = new MockMultipartFile("fichier", "etoile.png", "image/png",
                new ClassPathResource("image/etoile.png").getInputStream());

        String body = mockMvc.perform(multipart("/api/medias").file(image).cookie(agent)
                        .param("entiteType", "MAISON")
                        .param("entiteId", String.valueOf(idMaison)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.data.idMedia");
    }
}
