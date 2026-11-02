package com.immobilier.gestionImmobiliere.modules.notifications;

import com.immobilier.gestionImmobiliere.modules.notifications.services.AlerteSauvegardeService;
import com.immobilier.gestionImmobiliere.support.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/** Supervision des sauvegardes : les administrateurs sont prevenus une fois par jour et par probleme, jamais pour un etat sain. */
class AlerteSauvegardeIntegrationTest extends AbstractIntegrationTest {

    private static final Path DOSSIER = creerDossier();

    private static Path creerDossier() {
        try {
            return Files.createTempDirectory("sauvegardes-test");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @DynamicPropertySource
    static void surveillance(DynamicPropertyRegistry registry) {
        registry.add("app.sauvegardes.dossier", DOSSIER::toString);
        registry.add("app.sauvegardes.delai-heures", () -> "26");
    }

    @Autowired
    private AlerteSauvegardeService service;

    @Autowired
    private JdbcTemplate jdbc;

    @AfterEach
    void nettoyer() {
        jdbc.update("DELETE FROM notification WHERE cle_alerte LIKE 'SAUVEGARDE:%'");
    }

    private void ecrireStatut(String statut, OffsetDateTime date) throws IOException {
        Files.writeString(DOSSIER.resolve("dernier-statut.txt"), statut + " " + date + " /backups/postgres/x.dump");
    }

    private int nombreAdmins() {
        return jdbc.queryForObject("SELECT count(*) FROM users u JOIN role r ON r.id_role = u.id_role "
                + "WHERE r.libelle_role = 'ROLE_ADMIN' AND u.is_deleted = false", Integer.class);
    }

    private int notificationsSauvegarde() {
        return jdbc.queryForObject("SELECT count(*) FROM notification WHERE cle_alerte LIKE 'SAUVEGARDE:%'", Integer.class);
    }

    @Test
    void sauvegardeSaine_neNotifiePersonne() throws IOException {
        ecrireStatut("OK", OffsetDateTime.now().minusHours(3));
        assertThat(service.verifier()).isZero();
        assertThat(notificationsSauvegarde()).isZero();
    }

    @Test
    void sauvegardeEnEchec_previentChaqueAdministrateur_uneSeuleFoisParJour() throws IOException {
        int admins = nombreAdmins();
        assertThat(admins).isPositive();
        ecrireStatut("ECHEC", OffsetDateTime.now().minusHours(1));

        assertThat(service.verifier()).as("1er passage : tous les administrateurs sont prevenus").isEqualTo(admins);
        assertThat(notificationsSauvegarde()).isEqualTo(admins);
        assertThat(service.verifier()).as("passage suivant le meme jour : pas de doublon").isZero();
        assertThat(notificationsSauvegarde()).isEqualTo(admins);
    }

    @Test
    void sauvegardeTropAncienne_estSignalee() throws IOException {
        ecrireStatut("OK", OffsetDateTime.now().minusHours(40));
        assertThat(service.verifier()).isEqualTo(nombreAdmins());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM notification WHERE cle_alerte LIKE 'SAUVEGARDE:RETARD:%'", Integer.class))
                .isEqualTo(nombreAdmins());
    }
}
