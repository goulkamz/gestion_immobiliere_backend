package com.immobilier.gestionImmobiliere.modules.contrats;

import com.immobilier.gestionImmobiliere.modules.paiements.services.EcheanceService;
import com.immobilier.gestionImmobiliere.support.AbstractIntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Résiliation d'un mandat : un loyer est daté du 1er du mois suivant son mois d'occupation, donc le reversement daté du
 * mois qui suit la résiliation paie le mois de résiliation et est conservé. Les reversements datés encore après sont
 * supprimés, sauf s'ils ont reçu un paiement (même partiel) : la trace d'un virement n'est jamais supprimée.
 * La cour 3 du seed n'a ni mandat ni location active : on y crée un mandat actif dédié au test.
 */
class ResiliationMandatIntegrationTest extends AbstractIntegrationTest {

    private static final int COUR_SANS_LOCATION_ACTIVE = 3;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private EcheanceService echeanceService;

    // La base de test est partagée entre les classes : ce que ces tests créent est supprimé après chacun d'eux
    // (un mandat actif oublié sur la cour 3 ferait échouer « cour sans mandat actif : réservation refusée »).
    private final List<Integer> mandatsCrees = new ArrayList<>();
    private final List<Integer> contratsCrees = new ArrayList<>();
    private int derniereNotification;

    @BeforeEach
    void memoriserEtat() {
        derniereNotification = jdbc.queryForObject("SELECT COALESCE(MAX(id_notification), 0) FROM notification", Integer.class);
    }

    @AfterEach
    void nettoyer() {
        for (int idMandat : mandatsCrees) {
            jdbc.update("DELETE FROM echeance_loyer WHERE entite_echeance_type = 'MANDAT' AND entite_echeance_id = ?", idMandat);
            jdbc.update("DELETE FROM contrat_mandat WHERE id_mandat = ?", idMandat);
        }
        for (int idContrat : contratsCrees) {
            jdbc.update("DELETE FROM echeance_loyer WHERE entite_echeance_type = 'LOCATION' AND entite_echeance_id = ?", idContrat);
            jdbc.update("DELETE FROM contra_location WHERE id_contra_location = ?", idContrat);
        }
        jdbc.update("DELETE FROM notification WHERE id_notification > ?", derniereNotification);
        mandatsCrees.clear();
        contratsCrees.clear();
    }

    @Test
    void resiliation_supprimeLesReversementsDesMoisSuivantsEtGardeCeluiDuMoisDeResiliationEtLesPayes() throws Exception {
        Cookie agent = connexion("agent@gestimmo.test");
        int idAgent = jdbc.queryForObject("SELECT id_user FROM users WHERE email = 'agent@gestimmo.test'", Integer.class);
        int idMandat = creerMandatActif("MENSUEL", idAgent);

        LocalDate moisCourant = LocalDate.now().withDayOfMonth(1);
        creerReversement(idMandat, moisCourant.minusMonths(1), "EN_ATTENTE", "0");
        creerReversement(idMandat, moisCourant, "EN_ATTENTE", "0");
        creerReversement(idMandat, moisCourant.plusMonths(1), "EN_ATTENTE", "0");   // paie le mois de résiliation
        creerReversement(idMandat, moisCourant.plusMonths(2), "EN_ATTENTE", "0");
        creerReversement(idMandat, moisCourant.plusMonths(3), "EN_ATTENTE", "50");  // paiement partiel
        creerReversement(idMandat, moisCourant.plusMonths(4), "PAYE", "90000");

        mockMvc.perform(patch("/api/contrats-mandat/{id}/resilierContratLocation", idMandat)
                        .cookie(agent)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"motifResiliation":"test automatique"}"""))
                .andExpect(status().isOk());

        assertThat(supprime(idMandat, moisCourant.minusMonths(1))).as("mois passé").isFalse();
        assertThat(supprime(idMandat, moisCourant)).as("reversement daté du mois de résiliation").isFalse();
        assertThat(supprime(idMandat, moisCourant.plusMonths(1))).as("reversement qui paie le mois de résiliation : conservé").isFalse();
        assertThat(supprime(idMandat, moisCourant.plusMonths(2))).as("au-delà, sans paiement : supprimé").isTrue();
        assertThat(supprime(idMandat, moisCourant.plusMonths(3))).as("au-delà, avec paiement partiel : conservé").isFalse();
        assertThat(supprime(idMandat, moisCourant.plusMonths(4))).as("au-delà, payé : conservé").isFalse();
    }


    // ---------- Calcul mensuel, calcul cumulé et date de fin : même lecture du dernier mois ----------

    @Test
    void calculMensuel_resteAutoriseJusquAuReversementQuiPaieLeMoisDeResiliation() throws Exception {
        Cookie agent = connexion("agent@gestimmo.test");
        int idAgent = idAgent();
        LocalDate moisCourant = LocalDate.now().withDayOfMonth(1);
        int idMandat = creerMandatActif("MENSUEL", idAgent);
        int idContrat = creerContratTermine();
        for (int decalage = 0; decalage <= 2; decalage++) {
            creerLoyer(idContrat, moisCourant.plusMonths(decalage));
        }
        resilier(agent, idMandat);

        // Mandat résilié ce mois-ci : le reversement daté du mois suivant paie le mois de résiliation, il reste calculable
        echeanceService.calculerReversementMandat(idMandat, moisCourant.plusMonths(1), idAgent);
        assertThat(nombreReversements(idMandat, moisCourant.plusMonths(1))).isEqualTo(1);

        // Au-delà, plus rien n'est dû
        assertThatThrownBy(() -> echeanceService.calculerReversementMandat(idMandat, moisCourant.plusMonths(2), idAgent))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("postérieure à la résiliation");
    }

    @Test
    void calculCumule_vaJusquAuReversementQuiPaieLeMoisDeResiliationEtPasAuDela() throws Exception {
        Cookie agent = connexion("agent@gestimmo.test");
        int idAgent = idAgent();
        LocalDate moisCourant = LocalDate.now().withDayOfMonth(1);
        int idMandat = creerMandatActif("CUMUL", idAgent);
        int idContrat = creerContratTermine();
        for (int decalage = 0; decalage <= 3; decalage++) {
            creerLoyer(idContrat, moisCourant.plusMonths(decalage));
        }
        resilier(agent, idMandat);

        echeanceService.calculerReversementCumulMandat(idMandat, moisCourant.plusMonths(3), idAgent);

        assertThat(nombreReversements(idMandat, moisCourant)).as("mois de résiliation").isEqualTo(1);
        assertThat(nombreReversements(idMandat, moisCourant.plusMonths(1))).as("paie le mois de résiliation").isEqualTo(1);
        assertThat(nombreReversements(idMandat, moisCourant.plusMonths(2))).as("au-delà de la résiliation").isZero();
    }

    @Test
    void dateDeFin_unReversementQuiPaieLeDernierMoisNeBloquePas_unReversementAuDelaBloque() {
        int idMandat = creerMandatActif("MENSUEL", idAgent());
        LocalDate moisCourant = LocalDate.now().withDayOfMonth(1);
        LocalDate finDuMois = moisCourant.plusMonths(1).minusDays(1);

        creerReversement(idMandat, moisCourant.plusMonths(1), "EN_ATTENTE", "0");
        assertThat(echeanceService.reversementApres(idMandat, finDuMois))
                .as("reversement du mois suivant = dernier mois couvert : n'empêche pas une fin au dernier jour du mois").isFalse();

        creerReversement(idMandat, moisCourant.plusMonths(2), "EN_ATTENTE", "0");
        assertThat(echeanceService.reversementApres(idMandat, finDuMois))
                .as("reversement deux mois plus tard : couvre une période après la fin").isTrue();
    }

    @Test
    void calculMensuel_mandatArriveASaDateDeFinSansResiliation_memeRegle() {
        int idAgent = idAgent();
        LocalDate moisCourant = LocalDate.now().withDayOfMonth(1);
        int idMandat = creerMandatActif("MENSUEL", idAgent);
        // le mandat se termine le dernier jour du mois courant, sans être résilié
        jdbc.update("UPDATE contrat_mandat SET date_fin = ? WHERE id_mandat = ?",
                Date.valueOf(moisCourant.plusMonths(1).minusDays(1)), idMandat);
        int idContrat = creerContratTermine();
        for (int decalage = 0; decalage <= 2; decalage++) {
            creerLoyer(idContrat, moisCourant.plusMonths(decalage));
        }

        echeanceService.calculerReversementMandat(idMandat, moisCourant.plusMonths(1), idAgent);
        assertThat(nombreReversements(idMandat, moisCourant.plusMonths(1))).as("paie le dernier mois du mandat").isEqualTo(1);

        assertThatThrownBy(() -> echeanceService.calculerReversementMandat(idMandat, moisCourant.plusMonths(2), idAgent))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("date de fin");
    }

    private int idAgent() {
        return jdbc.queryForObject("SELECT id_user FROM users WHERE email = 'agent@gestimmo.test'", Integer.class);
    }

    private int creerMandatActif(String modeFacturation, int idAgent) {
        int id = jdbc.queryForObject(
                "INSERT INTO contrat_mandat (id_cour, id_user, date_debut, type_mandat, commission, mode_facturation, statut) "
                        + "VALUES (?, ?, ?, 'GESTION', 10, ?, 'ACTIF') RETURNING id_mandat",
                Integer.class, COUR_SANS_LOCATION_ACTIVE, idAgent, Date.valueOf(LocalDate.now().minusMonths(6)), modeFacturation);
        mandatsCrees.add(id);
        return id;
    }

    /** Contrat de location terminé (il ne bloque pas la résiliation) sur une maison de la cour, loyer 100 000. */
    private int creerContratTermine() {
        int idMaison = jdbc.queryForObject("SELECT MIN(id_maison) FROM maison WHERE id_cour = ?", Integer.class, COUR_SANS_LOCATION_ACTIVE);
        int idLocataire = jdbc.queryForObject("SELECT id_user FROM users WHERE email = 'client2@gestimmo.test'", Integer.class);
        int id = jdbc.queryForObject(
                "INSERT INTO contra_location (id_user, id_maison, date_entree, montant_loyer, statut) "
                        + "VALUES (?, ?, ?, 100000, 'TERMINE') RETURNING id_contra_location",
                Integer.class, idLocataire, idMaison, Date.valueOf(LocalDate.now().minusMonths(6)));
        contratsCrees.add(id);
        return id;
    }

    private void creerLoyer(int idContrat, LocalDate dateEcheance) {
        jdbc.update("INSERT INTO echeance_loyer (entite_echeance_type, entite_echeance_id, date_echeance, montant_du, montant_paye, "
                        + "commission_deduite, penalite, statut) VALUES ('LOCATION', ?, ?, 100000, 0, 0, 0, 'EN_ATTENTE')",
                idContrat, Date.valueOf(dateEcheance));
    }

    private void resilier(Cookie agent, int idMandat) throws Exception {
        mockMvc.perform(patch("/api/contrats-mandat/{id}/resilierContratLocation", idMandat)
                        .cookie(agent)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"motifResiliation":"test automatique"}"""))
                .andExpect(status().isOk());
    }

    private int nombreReversements(int idMandat, LocalDate dateEcheance) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM echeance_loyer WHERE entite_echeance_type = 'MANDAT' AND entite_echeance_id = ? "
                        + "AND date_echeance = ? AND is_deleted = false",
                Integer.class, idMandat, Date.valueOf(dateEcheance));
    }

    private void creerReversement(int idMandat, LocalDate dateEcheance, String statut, String montantPaye) {
        jdbc.update("INSERT INTO echeance_loyer (entite_echeance_type, entite_echeance_id, date_echeance, montant_du, montant_paye, "
                        + "commission_deduite, penalite, statut) VALUES ('MANDAT', ?, ?, 90000, ?::numeric, 10000, 0, ?)",
                idMandat, Date.valueOf(dateEcheance), montantPaye, statut);
    }

    private boolean supprime(int idMandat, LocalDate dateEcheance) {
        return jdbc.queryForObject(
                "SELECT is_deleted FROM echeance_loyer WHERE entite_echeance_type = 'MANDAT' AND entite_echeance_id = ? AND date_echeance = ?",
                Boolean.class, idMandat, Date.valueOf(dateEcheance));
    }

    private Cookie connexion(String email) throws Exception {
        return mockMvc.perform(post("/api/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}""".formatted(email, MOT_DE_PASSE_SEED)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getCookie("access_token");
    }
}
