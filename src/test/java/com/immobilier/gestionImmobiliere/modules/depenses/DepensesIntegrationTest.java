package com.immobilier.gestionImmobiliere.modules.depenses;

import com.immobilier.gestionImmobiliere.donnees.depenses.repository.DepenseBienRepository;
import com.immobilier.gestionImmobiliere.donnees.depenses.repository.DepenseImputationRepository;
import com.immobilier.gestionImmobiliere.modules.depenses.services.DepenseService;
import com.immobilier.gestionImmobiliere.modules.documents.services.RapportMensuelDocumentService;
import com.immobilier.gestionImmobiliere.modules.paiements.services.EcheanceService;
import com.immobilier.gestionImmobiliere.support.AbstractIntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Dépenses à la charge du bailleur : retenue sur les reversements du mandat de gestion, d'un coup ou étalée,
 * plafonnée au net disponible avec report sur le reversement suivant.
 * La cour 3 du seed n'a ni mandat ni location active : on y crée un mandat actif (commission 10 %) et un
 * contrat de location terminé à 100 000 de loyer, soit un net de 90 000 par mois avant retenue.
 */
class DepensesIntegrationTest extends AbstractIntegrationTest {

    private static final int COUR = 3;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private EcheanceService echeanceService;

    @Autowired
    private DepenseService depenseService;

    @Autowired
    private DepenseBienRepository depenseRepository;

    @Autowired
    private DepenseImputationRepository imputationRepository;

    @Autowired
    private RapportMensuelDocumentService rapportMensuelDocumentService;

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
            jdbc.update("DELETE FROM depense_bien WHERE id_mandat = ?", idMandat);
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
    void depenseEnUneFois_estRetenueSurLeReversementEtSoldee() throws Exception {
        int idMandat = preparer("MENSUEL", 1);
        int idDepense = creerDepense(connexion("agent@gestimmo.test"), "30000", 1, null);

        echeanceService.calculerReversementMandat(idMandat, moisCourant, idAgent());

        assertThat(net(idMandat, moisCourant)).isEqualByComparingTo("60000");
        assertThat(retenue(idMandat, moisCourant)).isEqualByComparingTo("30000");
        assertThat(commission(idMandat, moisCourant)).as("la commission reste calculée sur les loyers").isEqualByComparingTo("10000");
        assertThat(statut(idDepense)).isEqualTo("SOLDEE");
        assertThat(impute(idDepense)).isEqualByComparingTo("30000");
    }

    @Test
    void depenseEtalee_estRetenueParTiersEtLeDernierPrendLeResteDesArrondis() throws Exception {
        int idMandat = preparer("MENSUEL", 3);
        int idDepense = creerDepense(connexion("agent@gestimmo.test"), "100000", 3, null);

        echeanceService.calculerReversementMandat(idMandat, moisCourant, idAgent());
        assertThat(retenue(idMandat, moisCourant)).isEqualByComparingTo("33333.33");
        assertThat(statut(idDepense)).isEqualTo("EN_COURS");

        echeanceService.calculerReversementMandat(idMandat, moisCourant.plusMonths(1), idAgent());
        assertThat(retenue(idMandat, moisCourant.plusMonths(1))).isEqualByComparingTo("33333.34");

        echeanceService.calculerReversementMandat(idMandat, moisCourant.plusMonths(2), idAgent());
        assertThat(retenue(idMandat, moisCourant.plusMonths(2))).isEqualByComparingTo("33333.33");
        assertThat(statut(idDepense)).isEqualTo("SOLDEE");
        assertThat(impute(idDepense)).isEqualByComparingTo("100000");
    }

    @Test
    void depenseSuperieureAuNet_estPlafonneeAuNetEtLeResteEstReporte() throws Exception {
        int idMandat = preparer("MENSUEL", 2);
        int idDepense = creerDepense(connexion("agent@gestimmo.test"), "150000", 1, null);

        echeanceService.calculerReversementMandat(idMandat, moisCourant, idAgent());
        assertThat(net(idMandat, moisCourant)).as("jamais négatif").isEqualByComparingTo("0");
        assertThat(retenue(idMandat, moisCourant)).isEqualByComparingTo("90000");
        assertThat(statut(idDepense)).isEqualTo("EN_COURS");

        echeanceService.calculerReversementMandat(idMandat, moisCourant.plusMonths(1), idAgent());
        assertThat(retenue(idMandat, moisCourant.plusMonths(1))).isEqualByComparingTo("60000");
        assertThat(net(idMandat, moisCourant.plusMonths(1))).isEqualByComparingTo("30000");
        assertThat(statut(idDepense)).isEqualTo("SOLDEE");
    }

    @Test
    void calculCumule_retientLesDepensesMoisParMois() throws Exception {
        int idMandat = preparer("CUMUL", 3);
        int idDepense = creerDepense(connexion("agent@gestimmo.test"), "60000", 2, null);

        echeanceService.calculerReversementCumulMandat(idMandat, moisCourant.plusMonths(2), idAgent());

        assertThat(retenue(idMandat, moisCourant)).isEqualByComparingTo("30000");
        assertThat(retenue(idMandat, moisCourant.plusMonths(1))).isEqualByComparingTo("30000");
        assertThat(retenue(idMandat, moisCourant.plusMonths(2))).isEqualByComparingTo("0");
        assertThat(statut(idDepense)).isEqualTo("SOLDEE");
    }

    @Test
    void reversementSupprime_laRetenueRedevientARetenir() throws Exception {
        int idMandat = preparer("MENSUEL", 3);
        int idDepense = creerDepense(connexion("agent@gestimmo.test"), "30000", 1, null);
        echeanceService.calculerReversementMandat(idMandat, moisCourant.plusMonths(2), idAgent());
        assertThat(statut(idDepense)).isEqualTo("SOLDEE");

        // Résiliation aujourd'hui : le reversement daté de dans deux mois n'est plus dû, il disparaît
        echeanceService.supprimerReversementsFuturs(idMandat, LocalDate.now());

        assertThat(statut(idDepense)).isEqualTo("EN_COURS");
        assertThat(impute(idDepense)).isEqualByComparingTo("0");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM depense_imputation WHERE id_depense = ? AND is_deleted = false",
                Integer.class, idDepense)).isZero();
    }

    @Test
    void plafondDuMandat_auDela_ilFautLAccordDuBailleur() throws Exception {
        int idMandat = preparer("MENSUEL", 1);
        jdbc.update("UPDATE contrat_mandat SET plafond_depense_sans_accord = 20000 WHERE id_mandat = ?", idMandat);
        Cookie agent = connexion("agent@gestimmo.test");

        mockMvc.perform(post("/api/depenses").cookie(agent).contentType(MediaType.APPLICATION_JSON).content(corps("30000", 1, null)))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/depenses").cookie(agent).contentType(MediaType.APPLICATION_JSON).content(corps("20000", 1, null)))
                .andExpect(status().isCreated());
        // L'accord attesté sans préciser qui l'a donné ne suffit plus : le motif est obligatoire
        mockMvc.perform(post("/api/depenses").cookie(agent).contentType(MediaType.APPLICATION_JSON).content(corps("30000", 1, true)))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/depenses").cookie(agent).contentType(MediaType.APPLICATION_JSON)
                        .content(corps("30000", 1, true).replace("}", ",\"accordMotif\":\"Le bailleur, par téléphone\"}")))
                .andExpect(status().isCreated());
    }

    @Test
    void sansMandatDeGestionActif_laDepenseEstRefusee() throws Exception {
        mockMvc.perform(post("/api/depenses").cookie(connexion("agent@gestimmo.test"))
                        .contentType(MediaType.APPLICATION_JSON).content(corps("30000", 1, null)))
                .andExpect(status().isConflict());
    }

    @Test
    void depenseEntamee_nePeutPlusEtreModifieeNiAnnulee() throws Exception {
        int idMandat = preparer("MENSUEL", 1);
        Cookie agent = connexion("agent@gestimmo.test");
        int idDepense = creerDepense(agent, "60000", 2, null);

        mockMvc.perform(patch("/api/depenses/{id}", idDepense).cookie(agent).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombreMois":3}"""))
                .andExpect(status().isOk());

        echeanceService.calculerReversementMandat(idMandat, moisCourant, idAgent());
        assertThat(retenue(idMandat, moisCourant)).isEqualByComparingTo("20000");

        mockMvc.perform(patch("/api/depenses/{id}", idDepense).cookie(agent).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"montant":10000}"""))
                .andExpect(status().isConflict());
        mockMvc.perform(patch("/api/depenses/{id}/annuler", idDepense).cookie(agent).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"motif":"erreur de saisie"}"""))
                .andExpect(status().isConflict());
    }

    @Test
    void depenseAnnulee_nEstPasRetenue() throws Exception {
        int idMandat = preparer("MENSUEL", 1);
        Cookie agent = connexion("agent@gestimmo.test");
        int idDepense = creerDepense(agent, "30000", 1, null);

        mockMvc.perform(patch("/api/depenses/{id}/annuler", idDepense).cookie(agent).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"motif":"erreur de saisie"}"""))
                .andExpect(status().isOk());
        echeanceService.calculerReversementMandat(idMandat, moisCourant, idAgent());

        assertThat(retenue(idMandat, moisCourant)).isEqualByComparingTo("0");
        assertThat(net(idMandat, moisCourant)).isEqualByComparingTo("90000");
    }

    @Test
    void acces_leBailleurLitSesDepensesMaisNEcritPas_leClientNAAccesARien() throws Exception {
        preparer("MENSUEL", 1);
        int idDepense = creerDepense(connexion("agent@gestimmo.test"), "30000", 1, null);
        String emailProprietaire = jdbc.queryForObject(
                "SELECT u.email FROM cour c JOIN users u ON u.id_user = c.id_user WHERE c.id_cour = ?", String.class, COUR);
        Cookie proprietaire = connexion(emailProprietaire);

        mockMvc.perform(get("/api/depenses/{id}", idDepense).cookie(proprietaire)).andExpect(status().isOk());
        mockMvc.perform(get("/api/depenses").cookie(proprietaire)).andExpect(status().isOk());
        mockMvc.perform(post("/api/depenses").cookie(proprietaire).contentType(MediaType.APPLICATION_JSON).content(corps("1000", 1, null)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/depenses").cookie(connexion("client2@gestimmo.test"))).andExpect(status().isForbidden());

        // Un utilisateur non propriétaire de la cour (hors admin/agent) n'accède pas au détail
        assertThatThrownBy(() -> depenseService.getByIdForCurrentUser(idDepense, -1, false))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void notificationDuBailleur_pointeVersSaPageDepensesSurLaLigneConcernee() throws Exception {
        preparer("MENSUEL", 1);
        int idDepense = creerDepense(connexion("agent@gestimmo.test"), "30000", 1, null);

        String lien = jdbc.queryForObject("SELECT lien FROM notification WHERE id_notification > ? AND cle_alerte = ?",
                String.class, derniereNotification, "DEPENSE:" + idDepense);
        assertThat(lien).isEqualTo("/espace-bailleur/depenses?cible=" + idDepense);
    }

    @Test
    void rapportMensuel_compteLesDepensesEngageesRetenuesEtRestantes() throws Exception {
        LocalDate finMois = moisCourant.withDayOfMonth(moisCourant.lengthOfMonth());
        long nombreAvant = depenseRepository.countEngageesEntre(moisCourant, finMois);
        BigDecimal engageAvant = depenseRepository.sumEngageesEntre(moisCourant, finMois);
        BigDecimal retenuAvant = imputationRepository.sumRetenuesEntre(moisCourant, finMois);
        BigDecimal resteAvant = depenseRepository.sumResteARetenir();

        int idMandat = preparer("MENSUEL", 1);
        Cookie agent = connexion("agent@gestimmo.test");
        creerDepense(agent, "60000", 2, null);
        int idAnnulee = creerDepense(agent, "5000", 1, null);
        mockMvc.perform(patch("/api/depenses/{id}/annuler", idAnnulee).cookie(agent).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"motif":"erreur de saisie"}"""))
                .andExpect(status().isOk());
        echeanceService.calculerReversementMandat(idMandat, moisCourant, idAgent());

        assertThat(depenseRepository.countEngageesEntre(moisCourant, finMois) - nombreAvant).as("la dépense annulée n'est pas comptée").isEqualTo(1);
        assertThat(depenseRepository.sumEngageesEntre(moisCourant, finMois).subtract(engageAvant)).isEqualByComparingTo("60000");
        assertThat(imputationRepository.sumRetenuesEntre(moisCourant, finMois).subtract(retenuAvant)).isEqualByComparingTo("30000");
        assertThat(depenseRepository.sumResteARetenir().subtract(resteAvant)).isEqualByComparingTo("30000");

        // Le rapport du mois en cours est provisoire (jamais enregistré) : il doit se générer avec la section dépenses
        byte[] pdf = rapportMensuelDocumentService.genererOuRecuperer(moisCourant, idAgent());
        assertThat(new String(pdf, 0, 4, java.nio.charset.StandardCharsets.US_ASCII)).isEqualTo("%PDF");
    }

    // ---------- Outils ----------

    /** Mandat de gestion actif sur la cour 3 et un loyer de 100 000 pour chacun des {@code nombreMois} mois à partir du mois courant. */
    private int preparer(String modeFacturation, int nombreMois) {
        int idMandat = jdbc.queryForObject(
                "INSERT INTO contrat_mandat (id_cour, id_user, date_debut, type_mandat, commission, mode_facturation, statut) "
                        + "VALUES (?, ?, ?, 'GESTION', 10, ?, 'ACTIF') RETURNING id_mandat",
                Integer.class, COUR, idAgent(), Date.valueOf(moisCourant), modeFacturation);
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

    private String corps(String montant, int nombreMois, Boolean accordBailleur) {
        return """
                {"idCour":%d,"libelle":"Vidange de la latrine","categorie":"VIDANGE","montant":%s,"nombreMois":%d%s}"""
                .formatted(COUR, montant, nombreMois, accordBailleur == null ? "" : ",\"accordBailleur\":" + accordBailleur);
    }

    private int creerDepense(Cookie agent, String montant, int nombreMois, Boolean accordBailleur) throws Exception {
        mockMvc.perform(post("/api/depenses").cookie(agent).contentType(MediaType.APPLICATION_JSON)
                        .content(corps(montant, nombreMois, accordBailleur)))
                .andExpect(status().isCreated());
        return jdbc.queryForObject("SELECT MAX(id_depense) FROM depense_bien", Integer.class);
    }

    private int idAgent() {
        return jdbc.queryForObject("SELECT id_user FROM users WHERE email = 'agent@gestimmo.test'", Integer.class);
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

    private BigDecimal commission(int idMandat, LocalDate mois) {
        return colonneReversement("commission_deduite", idMandat, mois);
    }

    private String statut(int idDepense) {
        return jdbc.queryForObject("SELECT statut FROM depense_bien WHERE id_depense = ?", String.class, idDepense);
    }

    private BigDecimal impute(int idDepense) {
        return jdbc.queryForObject("SELECT montant_impute FROM depense_bien WHERE id_depense = ?", BigDecimal.class, idDepense);
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
