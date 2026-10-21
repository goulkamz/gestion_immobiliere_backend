package com.immobilier.gestionImmobiliere.modules.statistiques;

import com.immobilier.gestionImmobiliere.modules.statistiques.dto.ReversementsStatsDTO;
import com.immobilier.gestionImmobiliere.modules.statistiques.services.StatsFinanceQueries;
import com.immobilier.gestionImmobiliere.support.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * « Avance de l'agence » : un reversement PAYÉ dont les loyers du même mois (même cour) n'ont pas été encaissés.
 * Les chiffres du jeu de départ servent de base : on vérifie la variation apportée par le scénario du test.
 */
class AvanceSansLoyerIntegrationTest extends AbstractIntegrationTest {

    private static final int COUR_3 = 3;

    @Autowired
    private StatsFinanceQueries queries;

    @Autowired
    private JdbcTemplate jdbc;

    private Integer idMandat;
    private Integer idContrat;

    @AfterEach
    void nettoyer() {
        if (idMandat != null) {
            jdbc.update("DELETE FROM echeance_loyer WHERE entite_echeance_type = 'MANDAT' AND entite_echeance_id = ?", idMandat);
            jdbc.update("DELETE FROM contrat_mandat WHERE id_mandat = ?", idMandat);
        }
        if (idContrat != null) {
            jdbc.update("DELETE FROM echeance_loyer WHERE entite_echeance_type = 'LOCATION' AND entite_echeance_id = ?", idContrat);
            jdbc.update("DELETE FROM contra_location WHERE id_contra_location = ?", idContrat);
        }
    }

    @Test
    void reversementPayeSansLoyerEncaisse_estUneAvance_puisDisparaitQuandLeLoyerEstEncaisse() {
        ReversementsStatsDTO avant = queries.reversements();

        LocalDate mois = LocalDate.now().minusMonths(1).withDayOfMonth(1);
        int idAgent = jdbc.queryForObject("SELECT id_user FROM users WHERE email = 'agent@gestimmo.test'", Integer.class);
        idMandat = jdbc.queryForObject(
                "INSERT INTO contrat_mandat (id_cour, id_user, date_debut, type_mandat, commission, mode_facturation, statut) "
                        + "VALUES (?, ?, ?, 'GESTION', 10, 'MENSUEL', 'ACTIF') RETURNING id_mandat",
                Integer.class, COUR_3, idAgent, Date.valueOf(mois.minusMonths(3)));
        int idMaison = jdbc.queryForObject("SELECT MIN(id_maison) FROM maison WHERE id_cour = ?", Integer.class, COUR_3);
        int idLocataire = jdbc.queryForObject("SELECT id_user FROM users WHERE email = 'client2@gestimmo.test'", Integer.class);
        idContrat = jdbc.queryForObject(
                "INSERT INTO contra_location (id_user, id_maison, date_entree, montant_loyer, statut) "
                        + "VALUES (?, ?, ?, 100000, 'TERMINE') RETURNING id_contra_location",
                Integer.class, idLocataire, idMaison, Date.valueOf(mois.minusMonths(3)));

        // Loyer de 100 000 impayé, et reversement payé de 90 000 net (10 % de commission)
        long idLoyer = jdbc.queryForObject(
                "INSERT INTO echeance_loyer (entite_echeance_type, entite_echeance_id, date_echeance, montant_du, montant_paye, "
                        + "commission_deduite, penalite, statut) VALUES ('LOCATION', ?, ?, 100000, 0, 0, 0, 'EN_RETARD') RETURNING id_echeance",
                Long.class, idContrat, Date.valueOf(mois));
        jdbc.update("INSERT INTO echeance_loyer (entite_echeance_type, entite_echeance_id, date_echeance, montant_du, montant_paye, "
                        + "commission_deduite, penalite, statut) VALUES ('MANDAT', ?, ?, 90000, 90000, 10000, 0, 'PAYE')",
                idMandat, Date.valueOf(mois));

        ReversementsStatsDTO sansLoyer = queries.reversements();
        assertThat(sansLoyer.getAvanceSansLoyer().subtract(avant.getAvanceSansLoyer()))
                .as("loyer non encaissé : tout le net reversé est une avance").isEqualByComparingTo(new BigDecimal("90000"));
        assertThat(sansLoyer.getNombreAvances() - avant.getNombreAvances()).isEqualTo(1);

        // Le locataire paie la moitié : la moitié du net reste une avance
        jdbc.update("UPDATE echeance_loyer SET montant_paye = 50000 WHERE id_echeance = ?", idLoyer);
        ReversementsStatsDTO moitie = queries.reversements();
        assertThat(moitie.getAvanceSansLoyer().subtract(avant.getAvanceSansLoyer()))
                .as("loyer payé à moitié : moitié du net en avance").isEqualByComparingTo(new BigDecimal("45000"));

        // Loyer encaissé en entier : plus d'avance
        jdbc.update("UPDATE echeance_loyer SET montant_paye = 100000, statut = 'PAYE' WHERE id_echeance = ?", idLoyer);
        ReversementsStatsDTO complet = queries.reversements();
        assertThat(complet.getAvanceSansLoyer()).isEqualByComparingTo(avant.getAvanceSansLoyer());
        assertThat(complet.getNombreAvances()).isEqualTo(avant.getNombreAvances());
    }
}
