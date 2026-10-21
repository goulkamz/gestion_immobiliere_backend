package com.immobilier.gestionImmobiliere.modules.statistiques;

import com.immobilier.gestionImmobiliere.modules.statistiques.dto.FinancesPeriodeDTO;
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
 * Barres de recouvrement et de part reversée « de la période » : elles comptent les loyers et les reversements dont la
 * DATE tombe dans la période (déjà échus), quelle que soit la date du paiement. La période de test est en 2020 : aucune
 * donnée du jeu de départ n'y figure.
 */
class FinancesPeriodeIntegrationTest extends AbstractIntegrationTest {

    private static final int COUR_3 = 3;
    private static final LocalDate DEBUT = LocalDate.of(2020, 1, 1);
    private static final LocalDate FIN = LocalDate.of(2020, 1, 31);

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
    void loyersEtReversementsDeLaPeriode_comptentSeulementCeuxDontLaDateTombeDansLaPeriode() {
        int idAgent = jdbc.queryForObject("SELECT id_user FROM users WHERE email = 'agent@gestimmo.test'", Integer.class);
        idMandat = jdbc.queryForObject(
                "INSERT INTO contrat_mandat (id_cour, id_user, date_debut, type_mandat, commission, mode_facturation, statut) "
                        + "VALUES (?, ?, '2019-06-01', 'GESTION', 10, 'MENSUEL', 'ACTIF') RETURNING id_mandat",
                Integer.class, COUR_3, idAgent);
        int idMaison = jdbc.queryForObject("SELECT MIN(id_maison) FROM maison WHERE id_cour = ?", Integer.class, COUR_3);
        int idLocataire = jdbc.queryForObject("SELECT id_user FROM users WHERE email = 'client2@gestimmo.test'", Integer.class);
        idContrat = jdbc.queryForObject(
                "INSERT INTO contra_location (id_user, id_maison, date_entree, montant_loyer, statut) "
                        + "VALUES (?, ?, '2019-06-01', 100000, 'TERMINE') RETURNING id_contra_location",
                Integer.class, idLocataire, idMaison);

        // Loyers de la période : 100 000 payés à 60 000, 50 000 payés en entier. Un loyer de décembre 2019 reste hors période.
        loyer(DEBUT, 100000, 60000, "EN_RETARD");
        loyer(DEBUT, 50000, 50000, "PAYE");
        loyer(LocalDate.of(2019, 12, 1), 70000, 0, "EN_RETARD");
        // Reversements de la période : 90 000 net payé, 45 000 net à payer. Un reversement de décembre 2019 reste hors période.
        reversement(DEBUT, 90000, "PAYE");
        reversement(DEBUT, 45000, "EN_ATTENTE");
        reversement(LocalDate.of(2019, 12, 1), 30000, "EN_ATTENTE");

        FinancesPeriodeDTO fin = queries.finances(DEBUT, FIN);

        FinancesPeriodeDTO.LoyersPeriodeDTO l = fin.getLoyersPeriode();
        assertThat(l.getDusPeriode()).isEqualByComparingTo(new BigDecimal("150000"));
        assertThat(l.getEncaissePeriode()).isEqualByComparingTo(new BigDecimal("110000"));
        assertThat(l.getResteAEncaisser()).isEqualByComparingTo(new BigDecimal("40000"));
        assertThat(l.getNombreImpayes()).isEqualTo(1);
        assertThat(l.getTauxRecouvrement()).isEqualByComparingTo(new BigDecimal("73.3"));

        FinancesPeriodeDTO.ReversementsPeriodeDTO r = fin.getReversementsPeriode();
        assertThat(r.getTotalPeriode()).isEqualByComparingTo(new BigDecimal("135000"));
        assertThat(r.getEffectue()).isEqualByComparingTo(new BigDecimal("90000"));
        assertThat(r.getAReverser()).isEqualByComparingTo(new BigDecimal("45000"));
        assertThat(r.getTauxReverse()).isEqualByComparingTo(new BigDecimal("66.7"));
    }

    @Test
    void periodeEntierementDansLeFutur_neCompteRien() {
        LocalDate moisProchain = LocalDate.now().withDayOfMonth(1).plusMonths(1);
        FinancesPeriodeDTO fin = queries.finances(moisProchain, moisProchain.plusMonths(1).minusDays(1));

        assertThat(fin.getLoyersPeriode().getDusPeriode()).as("loyers pas encore exigibles").isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(fin.getLoyersPeriode().getTauxRecouvrement()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(fin.getReversementsPeriode().getTotalPeriode()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    private void loyer(LocalDate date, int du, int paye, String statut) {
        jdbc.update("INSERT INTO echeance_loyer (entite_echeance_type, entite_echeance_id, date_echeance, montant_du, montant_paye, "
                        + "commission_deduite, penalite, statut) VALUES ('LOCATION', ?, ?, ?, ?, 0, 0, ?)",
                idContrat, Date.valueOf(date), du, paye, statut);
    }

    private void reversement(LocalDate date, int net, String statut) {
        jdbc.update("INSERT INTO echeance_loyer (entite_echeance_type, entite_echeance_id, date_echeance, montant_du, montant_paye, "
                        + "commission_deduite, penalite, statut) VALUES ('MANDAT', ?, ?, ?, ?, 0, 0, ?)",
                idMandat, Date.valueOf(date), net, "PAYE".equals(statut) ? net : 0, statut);
    }
}
