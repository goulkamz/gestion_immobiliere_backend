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
    private final java.util.List<Integer> paiementsCrees = new java.util.ArrayList<>();

    @AfterEach
    void nettoyer() {
        for (Integer idPaiement : paiementsCrees) {
            jdbc.update("DELETE FROM paiement_echeance WHERE id_paiement = ?", idPaiement);
            jdbc.update("DELETE FROM paiement WHERE id_paiement = ?", idPaiement);
        }
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

    @Test
    void periodeFuture_montreCePayeAvanceSansLeCompterCommeDuNiCommeImpaye() {
        LocalDate debut = LocalDate.of(2099, 1, 1);
        LocalDate fin = LocalDate.of(2099, 1, 31);
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

        // Janvier 2099 : un loyer payé d'avance en entier, un loyer pas encore payé, un reversement déjà fait
        loyer(debut, 100000, 100000, "PAYE");
        loyer(debut, 60000, 0, "EN_ATTENTE");
        reversement(debut, 80000, "PAYE");

        FinancesPeriodeDTO resultat = queries.finances(debut, fin);

        FinancesPeriodeDTO.LoyersPeriodeDTO l = resultat.getLoyersPeriode();
        assertThat(l.getDusPeriode()).as("rien n'est échu").isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(l.getResteAEncaisser()).as("un loyer à venir n'est pas un impayé").isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(l.getAVenirPeriode()).isEqualByComparingTo(new BigDecimal("160000"));
        assertThat(l.getPayeAvancePeriode()).isEqualByComparingTo(new BigDecimal("100000"));
        assertThat(l.getNombreAVenir()).isEqualTo(2);

        FinancesPeriodeDTO.ReversementsPeriodeDTO r = resultat.getReversementsPeriode();
        assertThat(r.getTotalPeriode()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(r.getAVenirTotal()).isEqualByComparingTo(new BigDecimal("80000"));
        assertThat(r.getAVenirEffectue()).isEqualByComparingTo(new BigDecimal("80000"));
        assertThat(r.getNombreAVenir()).isEqualTo(1);
    }

    @Test
    void loyersPayesAvantLaPeriode_sontSepares_avecLesDatesReellesDesPaiements() {
        LocalDate debut = LocalDate.of(2099, 1, 1);
        LocalDate fin = LocalDate.of(2099, 1, 31);
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

        // Deux loyers de janvier 2099 payés avant : l'un le 15 novembre 2098, l'autre le 20 décembre 2098 ; un troisième payé en janvier
        paiementDeLoyer(loyer(debut, 40000, 40000, "PAYE"), "2098-11-15 10:00:00", 40000);
        paiementDeLoyer(loyer(debut, 25000, 25000, "PAYE"), "2098-12-20 10:00:00", 25000);
        paiementDeLoyer(loyer(debut, 10000, 10000, "PAYE"), "2099-01-10 10:00:00", 10000);

        FinancesPeriodeDTO.LoyersPeriodeDTO l = queries.finances(debut, fin).getLoyersPeriode();
        assertThat(l.getPayeAvantPeriode()).as("seuls les paiements antérieurs au 1er janvier").isEqualByComparingTo(new BigDecimal("65000"));
        assertThat(l.getPremierPaiementAvance()).isEqualTo(LocalDate.of(2098, 11, 15));
        assertThat(l.getDernierPaiementAvance()).isEqualTo(LocalDate.of(2098, 12, 20));

        // Une période sans paiement anticipé ne produit rien : la mention disparaît d'elle-même
        FinancesPeriodeDTO.LoyersPeriodeDTO vide = queries.finances(LocalDate.of(2099, 3, 1), LocalDate.of(2099, 3, 31)).getLoyersPeriode();
        assertThat(vide.getPayeAvantPeriode()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(vide.getPremierPaiementAvance()).isNull();
    }

    private int loyer(LocalDate date, int du, int paye, String statut) {
        return jdbc.queryForObject("INSERT INTO echeance_loyer (entite_echeance_type, entite_echeance_id, date_echeance, montant_du, montant_paye, "
                        + "commission_deduite, penalite, statut) VALUES ('LOCATION', ?, ?, ?, ?, 0, 0, ?) RETURNING id_echeance",
                Integer.class, idContrat, Date.valueOf(date), du, paye, statut);
    }

    private void paiementDeLoyer(int idEcheance, String datePaiement, int montant) {
        int idPaiement = jdbc.queryForObject("INSERT INTO paiement (date_paiement, montant_paiement, mode_paiement, reference_paiement, sens) "
                        + "VALUES (CAST(? AS timestamp), ?, 'ESPECES', 'TEST-AVANCE-' || ?, 'ENTREE') RETURNING id_paiement",
                Integer.class, datePaiement, montant, idEcheance);
        jdbc.update("INSERT INTO paiement_echeance (id_echeance, id_paiement) VALUES (?, ?)", idEcheance, idPaiement);
        paiementsCrees.add(idPaiement);
    }

    private void reversement(LocalDate date, int net, String statut) {
        jdbc.update("INSERT INTO echeance_loyer (entite_echeance_type, entite_echeance_id, date_echeance, montant_du, montant_paye, "
                        + "commission_deduite, penalite, statut) VALUES ('MANDAT', ?, ?, ?, ?, 0, 0, ?)",
                idMandat, Date.valueOf(date), net, "PAYE".equals(statut) ? net : 0, statut);
    }
}
