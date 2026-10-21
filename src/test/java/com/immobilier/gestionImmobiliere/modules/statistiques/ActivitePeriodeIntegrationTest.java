package com.immobilier.gestionImmobiliere.modules.statistiques;

import com.immobilier.gestionImmobiliere.modules.statistiques.dto.ActivitePeriodeDTO;
import com.immobilier.gestionImmobiliere.modules.statistiques.services.StatsActiviteQueries;
import com.immobilier.gestionImmobiliere.support.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Éléments créés pendant une période. La période de test est mars 2020 : le jeu de départ n'y a rien, et on y crée
 * un mandat et un contrat de location datés de cette période, plus un mandat daté d'avril (hors période).
 */
class ActivitePeriodeIntegrationTest extends AbstractIntegrationTest {

    private static final LocalDate DEBUT = LocalDate.of(2020, 3, 1);
    private static final LocalDate FIN = LocalDate.of(2020, 3, 31);

    @Autowired
    private StatsActiviteQueries queries;

    @Autowired
    private JdbcTemplate jdbc;

    @AfterEach
    void nettoyer() {
        jdbc.update("DELETE FROM contrat_mandat WHERE created_at >= '2020-01-01' AND created_at < '2020-12-31'");
        jdbc.update("DELETE FROM contra_location WHERE created_at >= '2020-01-01' AND created_at < '2020-12-31'");
    }

    @Test
    void compteSeulementCeQuiEstCreeDansLaPeriode() {
        ActivitePeriodeDTO vide = queries.activite(DEBUT, FIN);
        assertThat(vide.getMandats()).isZero();
        assertThat(vide.getContratsLocation()).isZero();
        assertThat(vide.getLocationsBienService()).isZero();
        assertThat(vide.getAnnonces()).isZero();
        assertThat(vide.getDemandes()).isZero();

        int idAgent = jdbc.queryForObject("SELECT id_user FROM users WHERE email = 'agent@gestimmo.test'", Integer.class);
        int idMaison = jdbc.queryForObject("SELECT MIN(id_maison) FROM maison WHERE id_cour = 3", Integer.class);
        int idLocataire = jdbc.queryForObject("SELECT id_user FROM users WHERE email = 'client2@gestimmo.test'", Integer.class);
        jdbc.update("INSERT INTO contrat_mandat (id_cour, id_user, date_debut, type_mandat, commission, mode_facturation, statut, created_at) "
                + "VALUES (3, ?, '2020-03-10', 'GESTION', 10, 'MENSUEL', 'RESILIE', '2020-03-15 10:00:00')", idAgent);
        jdbc.update("INSERT INTO contrat_mandat (id_cour, id_user, date_debut, type_mandat, commission, mode_facturation, statut, created_at) "
                + "VALUES (3, ?, '2020-04-10', 'GESTION', 10, 'MENSUEL', 'RESILIE', '2020-04-05 10:00:00')", idAgent);
        jdbc.update("INSERT INTO contra_location (id_user, id_maison, date_entree, montant_loyer, statut, created_at) "
                + "VALUES (?, ?, '2020-03-20', 100000, 'TERMINE', '2020-03-31 23:30:00')", idLocataire, idMaison);

        ActivitePeriodeDTO avec = queries.activite(DEBUT, FIN);
        assertThat(avec.getMandats()).as("le mandat d'avril est hors période").isEqualTo(1);
        assertThat(avec.getContratsLocation()).as("dernier jour de la période, 23 h 30 : compris").isEqualTo(1);
    }
}
