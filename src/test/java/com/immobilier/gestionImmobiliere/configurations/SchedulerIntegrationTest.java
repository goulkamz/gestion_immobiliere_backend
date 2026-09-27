package com.immobilier.gestionImmobiliere.configurations;

import com.immobilier.gestionImmobiliere.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Les taches planifiees ne tournent que la nuit : on les declenche directement pour
 * verifier que leurs requetes (souvent des UPDATE/DELETE en masse) passent toujours.
 */
class SchedulerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private Scheduler scheduler;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void toutesLesTachesPlanifiees_sExecutentSansErreur() {
        assertThatCode(() -> {
            scheduler.cleanExpiredPendingRegistrations();
            scheduler.cleanExpiredPasswordResetToken();
            scheduler.cleanExpiredToken();
            scheduler.nettoyerFichiersOrphelins();
            scheduler.purgerMediasSupprimes();
            scheduler.nettoyerAnnonce();
            scheduler.marquerChantierExpireEnRetard();
            scheduler.regenererEcheancesFinAnnee();
            scheduler.genererRapportDuMois();
        }).doesNotThrowAnyException();

        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM document WHERE type_document = 'RAPPORT_MENSUEL' AND is_deleted = FALSE",
                Integer.class)).as("rapport mensuel genere et enregistre").isPositive();
    }

    @Test
    void echeancesEchuesNonPayees_passentEnRetard() {
        Integer id = jdbc.queryForObject("""
                INSERT INTO echeance_loyer (entite_echeance_type, entite_echeance_id, date_echeance,
                    montant_du, montant_paye, statut, created_at)
                VALUES ('LOCATION', 1, CURRENT_DATE - 400, 150000, 0, 'EN_ATTENTE', NOW())
                RETURNING id_echeance""", Integer.class);

        scheduler.marquerChantierExpireEnRetard();

        assertThat(jdbc.queryForObject("SELECT statut FROM echeance_loyer WHERE id_echeance = ?", String.class, id))
                .isEqualTo("EN_RETARD");
    }
}
