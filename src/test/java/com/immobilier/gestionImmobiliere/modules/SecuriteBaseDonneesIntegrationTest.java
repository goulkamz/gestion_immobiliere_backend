package com.immobilier.gestionImmobiliere.modules;

import com.immobilier.gestionImmobiliere.support.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Securite de la base. L'application tourne avec le role restreint gestimmo_app (V4) : elle lit et ecrit les donnees
 * mais ne peut rien detruire ni administrer. Les preuves financieres et le journal sont immuables (V5) : une erreur
 * se corrige par une nouvelle ligne, jamais en reecrivant l'historique.
 */
class SecuriteBaseDonneesIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private DataSource dataSource;

    private final List<Integer> paiementsCrees = new ArrayList<>();

    @AfterEach
    void nettoyer() throws Exception {
        // Seule une maintenance explicite (reglage de transaction) peut effacer une preuve : c'est ce que fait ce nettoyage
        try (Connection c = dataSource.getConnection(); Statement s = c.createStatement()) {
            c.setAutoCommit(false);
            s.execute("SET LOCAL gestimmo.maintenance = 'on'");
            for (Integer id : paiementsCrees) {
                s.execute("DELETE FROM paiement_echeance WHERE id_paiement = " + id);
                s.execute("DELETE FROM paiement WHERE id_paiement = " + id);
            }
            c.commit();
        }
    }

    private int nouveauPaiement() {
        int id = jdbc.queryForObject("INSERT INTO paiement (date_paiement, montant_paiement, mode_paiement, reference_paiement, sens) "
                + "VALUES (NOW(), 12345, 'ESPECES', 'TEST-SECURITE', 'ENTREE') RETURNING id_paiement", Integer.class);
        paiementsCrees.add(id);
        return id;
    }

    // ---------- Role restreint ----------

    @Test
    void lApplicationNeTournePasAvecLeSuperUtilisateur() {
        assertThat(jdbc.queryForObject("SELECT current_user", String.class)).isEqualTo(ROLE_APPLICATIF);
        assertThat(jdbc.queryForObject("SELECT rolsuper FROM pg_roles WHERE rolname = current_user", Boolean.class)).isFalse();
        assertThat(jdbc.queryForObject("SELECT rolcreaterole FROM pg_roles WHERE rolname = current_user", Boolean.class)).isFalse();
    }

    @Test
    void leRoleApplicatifNePeutNiCreerNiSupprimerNiAdministrer() {
        assertThatThrownBy(() -> jdbc.execute("CREATE TABLE piege (id INT)")).isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.execute("DROP TABLE contrat_mandat")).isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.execute("TRUNCATE TABLE users CASCADE")).isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.execute("CREATE ROLE intrus LOGIN")).isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.queryForObject("SELECT count(*) FROM flyway_schema_history", Integer.class))
                .as("l'historique des migrations n'est pas lisible par l'application").isInstanceOf(DataAccessException.class);
    }

    @Test
    void leRoleApplicatifLitEtEcritLesDonnees() {
        assertThat(jdbc.queryForObject("SELECT count(*) FROM users", Integer.class)).isPositive();
        nouveauPaiement(); // insertion et usage des sequences
    }

    // ---------- Immutabilite des preuves ----------

    @Test
    void unPaiementNePeutPasEtreSupprime() {
        int id = nouveauPaiement();
        assertThatThrownBy(() -> jdbc.update("DELETE FROM paiement WHERE id_paiement = ?", id))
                .isInstanceOf(DataAccessException.class).hasMessageContaining("Suppression interdite");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM paiement WHERE id_paiement = ?", Integer.class, id)).isEqualTo(1);
    }

    @Test
    void unPaiementNePeutPasEtreSupprimeLogiquementNiRecopie() {
        int id = nouveauPaiement();
        assertThatThrownBy(() -> jdbc.update("UPDATE paiement SET is_deleted = true WHERE id_paiement = ?", id))
                .isInstanceOf(DataAccessException.class).hasMessageContaining("Modification interdite");
        assertThatThrownBy(() -> jdbc.update("UPDATE paiement SET montant_paiement = 1 WHERE id_paiement = ?", id))
                .isInstanceOf(DataAccessException.class).hasMessageContaining("Modification interdite");
        assertThat(jdbc.queryForObject("SELECT montant_paiement FROM paiement WHERE id_paiement = ?", Integer.class, id)).isEqualTo(12345);
    }

    @Test
    void lHorodatageDeMiseAJourReste_neanmoins_autorise() {
        int id = nouveauPaiement();
        assertThat(jdbc.update("UPDATE paiement SET updated_at = NOW() WHERE id_paiement = ?", id)).isEqualTo(1);
    }

    @Test
    void leJournalDAuditEstImmuable() {
        Integer idJournal = jdbc.queryForObject("SELECT MIN(id_journal) FROM journal_operation", Integer.class);
        if (idJournal == null) {
            idJournal = jdbc.queryForObject("INSERT INTO journal_operation (action, entite, date_action) VALUES ('TEST', 'TEST', NOW()) RETURNING id_journal", Integer.class);
        }
        int id = idJournal;
        assertThatThrownBy(() -> jdbc.update("DELETE FROM journal_operation WHERE id_journal = ?", id))
                .isInstanceOf(DataAccessException.class).hasMessageContaining("Suppression interdite");
        assertThatThrownBy(() -> jdbc.update("UPDATE journal_operation SET description = 'reecrit' WHERE id_journal = ?", id))
                .isInstanceOf(DataAccessException.class).hasMessageContaining("Modification interdite");
    }

    @Test
    void lesLiensEtRemboursementsSontAussiProteges() {
        for (String table : List.of("paiement_echeance", "paiement_location_bien_service", "remboursement")) {
            assertThatThrownBy(() -> jdbc.update("DELETE FROM " + table))
                    .as("suppression sur " + table).isInstanceOf(DataAccessException.class);
        }
    }

    @Test
    void uneMaintenanceExplicitePeutCorrigerUneEcriture() throws Exception {
        int id = nouveauPaiement();
        try (Connection c = dataSource.getConnection(); Statement s = c.createStatement()) {
            c.setAutoCommit(false);
            s.execute("SET LOCAL gestimmo.maintenance = 'on'");
            assertThat(s.executeUpdate("UPDATE paiement SET montant_paiement = 99 WHERE id_paiement = " + id)).isEqualTo(1);
            c.commit();
        }
        assertThat(jdbc.queryForObject("SELECT montant_paiement FROM paiement WHERE id_paiement = ?", Integer.class, id)).isEqualTo(99);
    }
}
