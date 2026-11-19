package com.immobilier.gestionImmobiliere.modules.depenses;

import com.immobilier.gestionImmobiliere.modules.depenses.services.DevisService;
import com.immobilier.gestionImmobiliere.modules.paiements.services.EcheanceService;
import com.immobilier.gestionImmobiliere.modules.statistiques.dto.ReversementsStatsDTO;
import com.immobilier.gestionImmobiliere.modules.statistiques.services.StatsFinanceQueries;
import com.immobilier.gestionImmobiliere.support.AbstractIntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cas limites des dépenses à la charge du bailleur et des devis : résiliation du mandat avec un solde
 * (retenue de tout le solde, puis règlement direct), rééchelonnement, expiration des devis sans réponse,
 * statistiques. Même montage que {@link DepensesIntegrationTest} : mandat actif à 10 % sur la cour 3 et
 * loyer de 100 000, soit un net de 90 000 par mois avant retenue.
 */
class DepensesCasLimitesIntegrationTest extends AbstractIntegrationTest {

    private static final int COUR = 3;
    private static final String AGENT = "agent@gestimmo.test";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private EcheanceService echeanceService;

    @Autowired
    private DevisService devisService;

    @Autowired
    private StatsFinanceQueries statsFinanceQueries;

    private final List<Integer> mandatsCrees = new ArrayList<>();
    private final List<Integer> contratsCrees = new ArrayList<>();
    private int derniereNotification;
    private final LocalDate moisCourant = LocalDate.now().withDayOfMonth(1);

    @BeforeEach
    void memoriserEtat() {
        derniereNotification = jdbc.queryForObject("SELECT COALESCE(MAX(id_notification), 0) FROM notification", Integer.class);
    }

    @AfterEach
    void nettoyer() {
        for (int idMandat : mandatsCrees) {
            jdbc.update("DELETE FROM depense_imputation WHERE id_depense IN (SELECT id_depense FROM depense_bien WHERE id_mandat = ?)", idMandat);
            jdbc.update("DELETE FROM depense_reglement WHERE id_depense IN (SELECT id_depense FROM depense_bien WHERE id_mandat = ?)", idMandat);
            jdbc.update("DELETE FROM depense_bien WHERE id_mandat = ?", idMandat);
            jdbc.update("DELETE FROM devis_travaux WHERE id_mandat = ?", idMandat);
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

    // ---------- Résiliation avec un solde ----------

    @Test
    void resiliation_toutLeSoldeEstRetenuSurLesReversementsPuisLeResteEstRegleDirectement() throws Exception {
        int idMandat = preparer(2);
        Cookie agent = connexion(AGENT);
        int idDepense = creerDepense(agent, "300000", 6);

        echeanceService.calculerReversementMandat(idMandat, moisCourant, idAgent());
        assertThat(retenue(idMandat, moisCourant)).as("étalement normal avant la résiliation").isEqualByComparingTo("50000");
        assertThat(net(idMandat, moisCourant)).isEqualByComparingTo("40000");

        // Un règlement direct n'a pas de sens tant que le mandat retient sur les reversements
        mockMvc.perform(post("/api/depenses/{id}/reglements", idDepense).cookie(agent).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"montant":10000}"""))
                .andExpect(status().isConflict());

        resilier(agent, idMandat);

        // Le reversement déjà calculé et pas encore viré absorbe ce qu'il peut : tout son net
        assertThat(retenue(idMandat, moisCourant)).isEqualByComparingTo("90000");
        assertThat(net(idMandat, moisCourant)).isEqualByComparingTo("0");
        assertThat(statut(idDepense)).isEqualTo("A_REGLER");
        assertThat(impute(idDepense)).isEqualByComparingTo("90000");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM depense_imputation WHERE id_depense = ? AND is_deleted = false",
                Integer.class, idDepense)).as("la retenue du reversement est complétée, pas doublée").isEqualTo(1);
        assertThat(nombreNotifications("DEPENSES_RESILIATION:" + idMandat)).isEqualTo(1);
        assertThat(nombreNotifications("DEPENSES_DUES:" + idMandat)).as("l'agent du mandat est prévenu du solde dû").isEqualTo(1);

        // Le dernier reversement dû, calculé après la résiliation, retient lui aussi tout le solde possible
        echeanceService.calculerReversementMandat(idMandat, moisCourant.plusMonths(1), idAgent());
        assertThat(retenue(idMandat, moisCourant.plusMonths(1))).isEqualByComparingTo("90000");
        assertThat(net(idMandat, moisCourant.plusMonths(1))).isEqualByComparingTo("0");
        assertThat(impute(idDepense)).isEqualByComparingTo("180000");
        assertThat(statut(idDepense)).isEqualTo("A_REGLER");

        // Reste 120 000 : réglé directement par le bailleur, en deux fois
        mockMvc.perform(post("/api/depenses/{id}/reglements", idDepense).cookie(agent).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"montant":200000}"""))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/depenses/{id}/reglements", idDepense).cookie(connexion(emailProprietaire()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"montant":100000}"""))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/depenses/{id}/reglements", idDepense).cookie(agent).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"montant":100000,"modeReglement":"Espèces","reference":"Reçu 12"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.montantRestant").value(20000))
                .andExpect(jsonPath("$.data.reglements.length()").value(1));
        assertThat(statut(idDepense)).isEqualTo("A_REGLER");
        mockMvc.perform(post("/api/depenses/{id}/reglements", idDepense).cookie(agent).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"montant":20000}"""))
                .andExpect(status().isCreated());
        assertThat(statut(idDepense)).isEqualTo("SOLDEE");
        mockMvc.perform(post("/api/depenses/{id}/reglements", idDepense).cookie(agent).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"montant":1}"""))
                .andExpect(status().isConflict());
    }

    @Test
    void resiliation_unReversementDejaVireNEstPasModifie() throws Exception {
        int idMandat = preparer(1);
        Cookie agent = connexion(AGENT);
        int idDepense = creerDepense(agent, "300000", 6);
        echeanceService.calculerReversementMandat(idMandat, moisCourant, idAgent());
        jdbc.update("UPDATE echeance_loyer SET statut = 'PAYE', montant_paye = montant_du WHERE entite_echeance_type = 'MANDAT' "
                + "AND entite_echeance_id = ?", idMandat);

        resilier(agent, idMandat);

        assertThat(retenue(idMandat, moisCourant)).isEqualByComparingTo("50000");
        assertThat(net(idMandat, moisCourant)).isEqualByComparingTo("40000");
        assertThat(impute(idDepense)).isEqualByComparingTo("50000");
        assertThat(statut(idDepense)).as("le solde de 250 000 est dû par le bailleur").isEqualTo("A_REGLER");
    }

    @Test
    void resiliation_lesDevisEncoreOuvertsSontRetires() throws Exception {
        int idMandat = preparer(1);
        Cookie agent = connexion(AGENT);
        int idDevis = creerDevis(agent, "80000", 2);

        resilier(agent, idMandat);

        assertThat(statutDevis(idDevis)).isEqualTo("ANNULE");
        assertThat(jdbc.queryForObject("SELECT motif_annulation FROM devis_travaux WHERE id_devis = ?", String.class, idDevis))
                .isEqualTo("Mandat résilié");
    }

    // ---------- Rééchelonnement ----------

    @Test
    void reechelonnement_rallongerEstLibreEtLeSoldeEstRetenuSurLesNouveauxMois() throws Exception {
        int idMandat = preparer(4);
        Cookie agent = connexion(AGENT);
        int idDepense = creerDepense(agent, "120000", 2);
        echeanceService.calculerReversementMandat(idMandat, moisCourant, idAgent());
        assertThat(retenue(idMandat, moisCourant)).isEqualByComparingTo("60000");

        // Même nombre de mois restants : rien à changer ; motif obligatoire ; le bailleur ne rééchelonne pas lui-même
        mockMvc.perform(patch("/api/depenses/{id}/reechelonner", idDepense).cookie(agent).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombreMoisRestants":1,"motif":"demande du bailleur"}"""))
                .andExpect(status().isBadRequest());
        mockMvc.perform(patch("/api/depenses/{id}/reechelonner", idDepense).cookie(agent).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombreMoisRestants":3}"""))
                .andExpect(status().isBadRequest());
        mockMvc.perform(patch("/api/depenses/{id}/reechelonner", idDepense).cookie(connexion(emailProprietaire()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombreMoisRestants":3,"motif":"je préfère"}"""))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/depenses/{id}/reechelonner", idDepense).cookie(agent).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombreMoisRestants":3,"motif":"demande du bailleur"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mensualite").value(20000))
                .andExpect(jsonPath("$.data.moisRestants").value(3))
                .andExpect(jsonPath("$.data.nombreMoisInitial").value(2));
        assertThat(nombreNotificationsCommencantPar("DEPENSE_REECHELONNEE:" + idDepense)).isEqualTo(1);

        echeanceService.calculerReversementMandat(idMandat, moisCourant.plusMonths(1), idAgent());
        assertThat(retenue(idMandat, moisCourant.plusMonths(1))).isEqualByComparingTo("20000");
        echeanceService.calculerReversementMandat(idMandat, moisCourant.plusMonths(2), idAgent());
        assertThat(retenue(idMandat, moisCourant.plusMonths(2))).isEqualByComparingTo("20000");
        assertThat(statut(idDepense)).isEqualTo("EN_COURS");
        echeanceService.calculerReversementMandat(idMandat, moisCourant.plusMonths(3), idAgent());
        assertThat(retenue(idMandat, moisCourant.plusMonths(3))).isEqualByComparingTo("20000");
        assertThat(statut(idDepense)).isEqualTo("SOLDEE");

        mockMvc.perform(patch("/api/depenses/{id}/reechelonner", idDepense).cookie(agent).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombreMoisRestants":2,"motif":"trop tard"}"""))
                .andExpect(status().isConflict());
    }

    @Test
    void reechelonnement_raccourcirDemandeLAccordDuBailleur() throws Exception {
        int idMandat = preparer(2);
        Cookie agent = connexion(AGENT);
        int idDepense = creerDepense(agent, "90000", 3);
        echeanceService.calculerReversementMandat(idMandat, moisCourant, idAgent());
        assertThat(retenue(idMandat, moisCourant)).isEqualByComparingTo("30000");

        mockMvc.perform(patch("/api/depenses/{id}/reechelonner", idDepense).cookie(agent).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombreMoisRestants":1,"motif":"le bailleur veut solder vite"}"""))
                .andExpect(status().isConflict());
        mockMvc.perform(patch("/api/depenses/{id}/reechelonner", idDepense).cookie(agent).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombreMoisRestants":1,"motif":"le bailleur veut solder vite","accordBailleur":true}"""))
                .andExpect(status().isOk());

        echeanceService.calculerReversementMandat(idMandat, moisCourant.plusMonths(1), idAgent());
        assertThat(retenue(idMandat, moisCourant.plusMonths(1))).isEqualByComparingTo("60000");
        assertThat(statut(idDepense)).isEqualTo("SOLDEE");
    }

    // ---------- Expiration des devis ----------

    @Test
    void devisSansReponse_estRelancePuisExpireEtPeutEtreSoumisANouveau() throws Exception {
        preparer(1);
        Cookie agent = connexion(AGENT);
        Cookie bailleur = connexion(emailProprietaire());
        int idDevis = creerDevis(agent, "80000", 2);

        // Récent : ni rappel ni expiration
        devisService.expirerDevisSansReponse();
        assertThat(statutDevis(idDevis)).isEqualTo("EN_ATTENTE");
        assertThat(nombreNotifications("DEVIS_RAPPEL:" + idDevis + ":1")).isZero();

        // À 5 jours de l'expiration (délai de 30 jours) : un seul rappel, même si le job repasse
        vieillirSoumission(idDevis, 25);
        devisService.expirerDevisSansReponse();
        devisService.expirerDevisSansReponse();
        assertThat(statutDevis(idDevis)).isEqualTo("EN_ATTENTE");
        assertThat(nombreNotifications("DEVIS_RAPPEL:" + idDevis + ":1")).isEqualTo(1);

        // Délai dépassé : expiré, bailleur et agent prévenus, le bailleur ne peut plus répondre
        vieillirSoumission(idDevis, 31);
        assertThat(devisService.expirerDevisSansReponse()).isEqualTo(1);
        assertThat(statutDevis(idDevis)).isEqualTo("EXPIRE");
        assertThat(nombreNotifications("DEVIS_EXPIRE:" + idDevis + ":1")).isEqualTo(2);
        mockMvc.perform(patch("/api/devis/{id}/accepter", idDevis).cookie(bailleur).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"montant":80000,"nombreMois":2}"""))
                .andExpect(status().isConflict());

        // L'agent le modifie : il repart en attente avec un nouveau délai
        mockMvc.perform(patch("/api/devis/{id}", idDevis).cookie(agent).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"montant":75000}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.statut").value("EN_ATTENTE"))
                .andExpect(jsonPath("$.data.nombreSoumissions").value(2))
                .andExpect(jsonPath("$.data.dateLimiteReponse").exists());
        assertThat(jdbc.queryForObject("SELECT date_soumission FROM devis_travaux WHERE id_devis = ?", Timestamp.class, idDevis)
                .toLocalDateTime()).isAfter(LocalDateTime.now().minusMinutes(5));
        assertThat(devisService.expirerDevisSansReponse()).isZero();
        mockMvc.perform(patch("/api/devis/{id}/accepter", idDevis).cookie(bailleur).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"montant":75000,"nombreMois":2}"""))
                .andExpect(status().isOk());
    }

    // ---------- Statistiques ----------

    @Test
    void statistiques_depensesEnCoursEtDuesCoteAdminEtCoteBailleur() throws Exception {
        ReversementsStatsDTO avant = statsFinanceQueries.reversements();
        int idMandat = preparer(1);
        Cookie agent = connexion(AGENT);
        Cookie bailleur = connexion(emailProprietaire());
        creerDepense(agent, "300000", 6);
        creerDevis(agent, "80000", 2);
        echeanceService.calculerReversementMandat(idMandat, moisCourant, idAgent());

        ReversementsStatsDTO enCours = statsFinanceQueries.reversements();
        assertThat(enCours.getDepensesResteARetenir().subtract(avant.getDepensesResteARetenir())).isEqualByComparingTo("250000");
        assertThat(enCours.getNombreDepensesEnCours() - avant.getNombreDepensesEnCours()).isEqualTo(1);
        mockMvc.perform(get("/api/stats/bailleur/depenses").cookie(bailleur))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resteARetenir").value(250000))
                .andExpect(jsonPath("$.data.nombreEnCours").value(1))
                .andExpect(jsonPath("$.data.duDirectement").value(0))
                .andExpect(jsonPath("$.data.devisEnAttente").value(1));

        resilier(agent, idMandat);

        // Le reversement en attente absorbe 40 000 de plus : restent 210 000 dus directement
        ReversementsStatsDTO apres = statsFinanceQueries.reversements();
        assertThat(apres.getDepensesResteARetenir()).isEqualByComparingTo(avant.getDepensesResteARetenir());
        assertThat(apres.getDepensesDuesParBailleurs().subtract(avant.getDepensesDuesParBailleurs())).isEqualByComparingTo("210000");
        assertThat(apres.getNombreDepensesDues() - avant.getNombreDepensesDues()).isEqualTo(1);
        mockMvc.perform(get("/api/stats/bailleur/depenses").cookie(bailleur))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resteARetenir").value(0))
                .andExpect(jsonPath("$.data.duDirectement").value(210000))
                .andExpect(jsonPath("$.data.nombreDues").value(1))
                .andExpect(jsonPath("$.data.devisEnAttente").value(0));
        mockMvc.perform(get("/api/stats/bailleur/depenses").cookie(connexion("client2@gestimmo.test"))).andExpect(status().isForbidden());
    }

    @Test
    void avanceSansLoyer_tientCompteDesDepensesRetenuesDansLesLoyersBruts() throws Exception {
        BigDecimal avant = statsFinanceQueries.reversements().getAvanceSansLoyer();
        int idMandat = preparer(1);
        creerDepense(connexion(AGENT), "30000", 1);
        echeanceService.calculerReversementMandat(idMandat, moisCourant, idAgent());
        // Reversement viré (60 000 nets) alors que le loyer de 100 000 n'est pas encaissé : l'agence a avancé tout le net
        jdbc.update("UPDATE echeance_loyer SET statut = 'PAYE', montant_paye = montant_du WHERE entite_echeance_type = 'MANDAT' "
                + "AND entite_echeance_id = ?", idMandat);

        assertThat(statsFinanceQueries.reversements().getAvanceSansLoyer().subtract(avant)).isEqualByComparingTo("60000");
    }

    // ---------- Outils ----------

    /** Mandat de gestion mensuel actif sur la cour 3 et un loyer de 100 000 pour chacun des {@code nombreMois} mois à partir du mois courant. */
    private int preparer(int nombreMois) {
        int idMandat = jdbc.queryForObject(
                "INSERT INTO contrat_mandat (id_cour, id_user, date_debut, type_mandat, commission, mode_facturation, statut) "
                        + "VALUES (?, ?, ?, 'GESTION', 10, 'MENSUEL', 'ACTIF') RETURNING id_mandat",
                Integer.class, COUR, idAgent(), Date.valueOf(moisCourant));
        mandatsCrees.add(idMandat);

        int idMaison = jdbc.queryForObject("SELECT MIN(id_maison) FROM maison WHERE id_cour = ?", Integer.class, COUR);
        int idLocataire = jdbc.queryForObject("SELECT id_user FROM users WHERE email = 'client2@gestimmo.test'", Integer.class);
        int idContrat = jdbc.queryForObject(
                "INSERT INTO contra_location (id_user, id_maison, date_entree, montant_loyer, statut) "
                        + "VALUES (?, ?, ?, 100000, 'TERMINE') RETURNING id_contra_location",
                Integer.class, idLocataire, idMaison, Date.valueOf(LocalDate.now().minusMonths(6)));
        contratsCrees.add(idContrat);
        for (int decalage = 0; decalage < nombreMois; decalage++) {
            jdbc.update("INSERT INTO echeance_loyer (entite_echeance_type, entite_echeance_id, date_echeance, montant_du, montant_paye, "
                            + "commission_deduite, penalite, statut) VALUES ('LOCATION', ?, ?, 100000, 0, 0, 0, 'EN_ATTENTE')",
                    idContrat, Date.valueOf(moisCourant.plusMonths(decalage)));
        }
        return idMandat;
    }

    private int creerDepense(Cookie agent, String montant, int nombreMois) throws Exception {
        mockMvc.perform(post("/api/depenses").cookie(agent).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idCour":%d,"libelle":"Reprise de la toiture","categorie":"ETANCHEITE","montant":%s,"nombreMois":%d}"""
                                .formatted(COUR, montant, nombreMois)))
                .andExpect(status().isCreated());
        return jdbc.queryForObject("SELECT MAX(id_depense) FROM depense_bien", Integer.class);
    }

    private int creerDevis(Cookie agent, String montant, int nombreMois) throws Exception {
        mockMvc.perform(post("/api/devis").cookie(agent).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idCour":%d,"libelle":"Remplacement du portail","categorie":"ETANCHEITE","montant":%s,"nombreMois":%d}"""
                                .formatted(COUR, montant, nombreMois)))
                .andExpect(status().isCreated());
        return jdbc.queryForObject("SELECT MAX(id_devis) FROM devis_travaux", Integer.class);
    }

    private void resilier(Cookie agent, int idMandat) throws Exception {
        mockMvc.perform(patch("/api/contrats-mandat/{id}/resilierContratLocation", idMandat).cookie(agent)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"motifResiliation":"test automatique"}"""))
                .andExpect(status().isOk());
    }

    private void vieillirSoumission(int idDevis, int jours) {
        jdbc.update("UPDATE devis_travaux SET date_soumission = ? WHERE id_devis = ?",
                Timestamp.valueOf(LocalDateTime.now().minusDays(jours)), idDevis);
    }

    private int idAgent() {
        return jdbc.queryForObject("SELECT id_user FROM users WHERE email = ?", Integer.class, AGENT);
    }

    private String emailProprietaire() {
        return jdbc.queryForObject("SELECT u.email FROM cour c JOIN users u ON u.id_user = c.id_user WHERE c.id_cour = ?", String.class, COUR);
    }

    private BigDecimal colonneReversement(String colonne, int idMandat, LocalDate mois) {
        return jdbc.queryForObject("SELECT " + colonne + " FROM echeance_loyer WHERE entite_echeance_type = 'MANDAT' "
                + "AND entite_echeance_id = ? AND date_echeance = ? AND is_deleted = false", BigDecimal.class, idMandat, Date.valueOf(mois));
    }

    private BigDecimal net(int idMandat, LocalDate mois) {
        return colonneReversement("montant_du", idMandat, mois);
    }

    private BigDecimal retenue(int idMandat, LocalDate mois) {
        return colonneReversement("retenue_depenses", idMandat, mois);
    }

    private String statut(int idDepense) {
        return jdbc.queryForObject("SELECT statut FROM depense_bien WHERE id_depense = ?", String.class, idDepense);
    }

    private String statutDevis(int idDevis) {
        return jdbc.queryForObject("SELECT statut FROM devis_travaux WHERE id_devis = ?", String.class, idDevis);
    }

    private BigDecimal impute(int idDepense) {
        return jdbc.queryForObject("SELECT montant_impute FROM depense_bien WHERE id_depense = ?", BigDecimal.class, idDepense);
    }

    private int nombreNotifications(String cle) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM notification WHERE id_notification > ? AND cle_alerte = ?",
                Integer.class, derniereNotification, cle);
    }

    private int nombreNotificationsCommencantPar(String prefixe) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM notification WHERE id_notification > ? AND cle_alerte LIKE ?",
                Integer.class, derniereNotification, prefixe + "%");
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
