package com.immobilier.gestionImmobiliere.modules.depenses;

import com.immobilier.gestionImmobiliere.modules.depenses.services.DevisService;
import com.immobilier.gestionImmobiliere.modules.notifications.services.AlerteAgentService;
import com.immobilier.gestionImmobiliere.modules.statistiques.services.StatsFinanceQueries;
import com.immobilier.gestionImmobiliere.support.AbstractIntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Compléments des dépenses et des devis : annulation d'un règlement et reçu, abandon de créance, accord du bailleur
 * attesté (motif obligatoire), accord de devis donné hors plateforme et contesté, devis PDF joint, rappel d'un devis accepté
 * non réalisé, alertes de fin de mandat, plafond retirable, recherche texte. Même montage que
 * {@link DepensesCasLimitesIntegrationTest} : mandat actif à 10 % sur la cour 3.
 */
class DepensesComplementsIntegrationTest extends AbstractIntegrationTest {

    private static final int COUR = 3;
    private static final String AGENT = "agent@gestimmo.test";
    private static final String ADMIN = "admin@gestimmo.test";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private DevisService devisService;

    @Autowired
    private AlerteAgentService alerteAgentService;

    @Autowired
    private StatsFinanceQueries statsFinanceQueries;

    private final List<Integer> mandatsCrees = new ArrayList<>();
    private int derniereNotification;
    private final LocalDate moisCourant = LocalDate.now().withDayOfMonth(1);

    @BeforeEach
    void memoriserEtat() {
        derniereNotification = jdbc.queryForObject("SELECT COALESCE(MAX(id_notification), 0) FROM notification", Integer.class);
    }

    @AfterEach
    void nettoyer() {
        for (int idMandat : mandatsCrees) {
            jdbc.update("DELETE FROM document WHERE entite_type = 'DEVIS_TRAVAUX' AND entite_id IN (SELECT id_devis FROM devis_travaux WHERE id_mandat = ?)", idMandat);
            jdbc.update("DELETE FROM document WHERE entite_type = 'REGLEMENT_DEPENSE' AND entite_id IN "
                    + "(SELECT id_reglement FROM depense_reglement WHERE id_depense IN (SELECT id_depense FROM depense_bien WHERE id_mandat = ?))", idMandat);
            jdbc.update("DELETE FROM depense_imputation WHERE id_depense IN (SELECT id_depense FROM depense_bien WHERE id_mandat = ?)", idMandat);
            jdbc.update("DELETE FROM depense_reglement WHERE id_depense IN (SELECT id_depense FROM depense_bien WHERE id_mandat = ?)", idMandat);
            jdbc.update("DELETE FROM depense_bien WHERE id_mandat = ?", idMandat);
            jdbc.update("DELETE FROM devis_travaux WHERE id_mandat = ?", idMandat);
            jdbc.update("DELETE FROM echeance_loyer WHERE entite_echeance_type = 'MANDAT' AND entite_echeance_id = ?", idMandat);
            jdbc.update("DELETE FROM document WHERE entite_type = 'AVENANT' AND entite_id IN (SELECT id_avenant FROM avenant WHERE id_mandat = ?)", idMandat);
            jdbc.update("DELETE FROM avenant WHERE id_mandat = ?", idMandat);
            jdbc.update("DELETE FROM document WHERE entite_type = 'CONTRAT_MANDAT' AND entite_id = ?", idMandat);
            jdbc.update("DELETE FROM contrat_mandat WHERE id_mandat = ?", idMandat);
        }
        jdbc.update("DELETE FROM notification WHERE id_notification > ?", derniereNotification);
        mandatsCrees.clear();
    }

    // ---------- Règlement annulé, reçu, abandon ----------

    @Test
    void reglement_annule_laDetteReapparaitEtLeRecuNEstPlusServi() throws Exception {
        int idMandat = preparer(null);
        Cookie agent = connexion(AGENT);
        int idDepense = creerDepense(agent, "30000", 1, null, null);
        resilier(agent, idMandat);
        assertThat(statut(idDepense)).isEqualTo("A_REGLER");

        mockMvc.perform(post("/api/depenses/{id}/reglements", idDepense).cookie(agent).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"montant":30000,"modeReglement":"Espèces"}"""))
                .andExpect(status().isCreated());
        int idReglement = jdbc.queryForObject("SELECT MAX(id_reglement) FROM depense_reglement WHERE id_depense = ?", Integer.class, idDepense);
        assertThat(statut(idDepense)).isEqualTo("SOLDEE");

        // Reçu : généré une fois, servi à l'agence et au bailleur de la cour, refusé à un tiers
        byte[] recu = mockMvc.perform(get("/api/documents/recus-reglements/{id}", idReglement).cookie(agent))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        assertThat(new String(recu, 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
        mockMvc.perform(get("/api/documents/recus-reglements/{id}", idReglement).cookie(connexion(emailProprietaire())))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/documents/recus-reglements/{id}", idReglement).cookie(connexion("client2@gestimmo.test")))
                .andExpect(status().isForbidden());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM document WHERE entite_type = 'REGLEMENT_DEPENSE' AND entite_id = ?",
                Integer.class, idReglement)).isEqualTo(1);

        // Annulation : motif obligatoire, la dette réapparaît, le règlement reste en base marqué annulé
        mockMvc.perform(patch("/api/depenses/{id}/reglements/{r}/annuler", idDepense, idReglement).cookie(agent)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"motif":" "}"""))
                .andExpect(status().isBadRequest());
        mockMvc.perform(patch("/api/depenses/{id}/reglements/{r}/annuler", idDepense, idReglement).cookie(agent)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"motif":"Saisi sur la mauvaise dépense"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.montantRestant").value(30000))
                .andExpect(jsonPath("$.data.reglements[0].annule").value(true));
        assertThat(statut(idDepense)).isEqualTo("A_REGLER");
        assertThat(jdbc.queryForObject("SELECT montant_regle FROM depense_bien WHERE id_depense = ?", BigDecimal.class, idDepense)).isEqualByComparingTo("0");
        assertThat(jdbc.queryForObject("SELECT annule FROM depense_reglement WHERE id_reglement = ?", Boolean.class, idReglement)).isTrue();
        assertThat(nombreNotifications("DEPENSE_REGLEMENT_ANNULE:" + idReglement)).as("le bailleur est prévenu").isEqualTo(1);

        mockMvc.perform(patch("/api/depenses/{id}/reglements/{r}/annuler", idDepense, idReglement).cookie(agent)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"motif":"Encore"}"""))
                .andExpect(status().isConflict());
        mockMvc.perform(get("/api/documents/recus-reglements/{id}", idReglement).cookie(agent)).andExpect(status().isConflict());

        // Le règlement corrigé peut être ressaisi
        mockMvc.perform(post("/api/depenses/{id}/reglements", idDepense).cookie(agent).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"montant":30000}"""))
                .andExpect(status().isCreated());
        assertThat(statut(idDepense)).isEqualTo("SOLDEE");
    }

    @Test
    void abandonDeCreance_estReserveALAdminEtSoldeLaDepenseSansLaConfondreAvecUnReglement() throws Exception {
        int idMandat = preparer(null);
        Cookie agent = connexion(AGENT);
        Cookie admin = connexion(ADMIN);
        BigDecimal avant = statsFinanceQueries.reversements().getDepensesDuesParBailleurs();

        int idDepense = creerDepense(agent, "30000", 1, null, null);
        // Rien à abandonner tant que la dépense se retient sur les reversements
        mockMvc.perform(patch("/api/depenses/{id}/abandon", idDepense).cookie(admin).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"motif":"Trop tôt"}"""))
                .andExpect(status().isConflict());

        resilier(agent, idMandat);
        assertThat(statsFinanceQueries.reversements().getDepensesDuesParBailleurs().subtract(avant)).isEqualByComparingTo("30000");

        mockMvc.perform(patch("/api/depenses/{id}/abandon", idDepense).cookie(agent).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"motif":"Bailleur introuvable"}"""))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/depenses/{id}/abandon", idDepense).cookie(admin).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"motif":" "}"""))
                .andExpect(status().isBadRequest());
        mockMvc.perform(patch("/api/depenses/{id}/abandon", idDepense).cookie(admin).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"motif":"Bailleur introuvable"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.statut").value("SOLDEE"))
                .andExpect(jsonPath("$.data.montantAbandonne").value(30000))
                .andExpect(jsonPath("$.data.montantRestant").value(0))
                .andExpect(jsonPath("$.data.reglements.length()").value(0));

        assertThat(jdbc.queryForObject("SELECT montant_regle FROM depense_bien WHERE id_depense = ?", BigDecimal.class, idDepense)).isEqualByComparingTo("0");
        assertThat(statsFinanceQueries.reversements().getDepensesDuesParBailleurs()).as("la dette n'est plus due").isEqualByComparingTo(avant);
        assertThat(nombreNotifications("DEPENSE_ABANDON:" + idDepense)).isEqualTo(1);
        mockMvc.perform(patch("/api/depenses/{id}/abandon", idDepense).cookie(admin).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"motif":"Deux fois"}"""))
                .andExpect(status().isConflict());
    }

    // ---------- Accord du bailleur attesté ----------

    @Test
    void accordAtteste_auDessusDuPlafondLeMotifEstObligatoireEtLeBailleurEnEstInforme() throws Exception {
        preparer(new BigDecimal("100000"));
        Cookie agent = connexion(AGENT);

        mockMvc.perform(post("/api/depenses").cookie(agent).contentType(MediaType.APPLICATION_JSON)
                        .content(corpsDepense("200000", true, null)))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/depenses").cookie(agent).contentType(MediaType.APPLICATION_JSON)
                        .content(corpsDepense("200000", null, "Au téléphone")))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/depenses").cookie(agent).contentType(MediaType.APPLICATION_JSON)
                        .content(corpsDepense("200000", true, "M. Ouedraogo, par téléphone, le 12/11")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.accordMotif").value("M. Ouedraogo, par téléphone, le 12/11"));
        int idDepense = jdbc.queryForObject("SELECT MAX(id_depense) FROM depense_bien", Integer.class);

        assertThat(jdbc.queryForObject("SELECT message FROM notification WHERE cle_alerte = ? AND id_notification > ?",
                String.class, "DEPENSE:" + idDepense, derniereNotification)).contains("M. Ouedraogo, par téléphone");

        // Sous le plafond : aucune attestation nécessaire, un motif éventuel est ignoré
        mockMvc.perform(post("/api/depenses").cookie(agent).contentType(MediaType.APPLICATION_JSON)
                        .content(corpsDepense("50000", null, "Inutile")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.accordMotif").doesNotExist());
    }

    // ---------- Devis : accord hors plateforme, contestation, pièce jointe, rappel ----------

    @Test
    void accordHorsPlateforme_estEnregistreParLAgentPuisContestableParLeBailleurTantQueLaDepenseNExistePas() throws Exception {
        preparer(null);
        Cookie agent = connexion(AGENT);
        Cookie bailleur = connexion(emailProprietaire());
        int idDevis = creerDevis(agent, "50000", 1);
        String aujourdhui = LocalDate.now().toString();

        mockMvc.perform(patch("/api/devis/{id}/accord-hors-plateforme", idDevis).cookie(bailleur).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"motif":"Moi-même","dateAccord":"%s"}""".formatted(aujourdhui)))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/devis/{id}/accord-hors-plateforme", idDevis).cookie(agent).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"motif":"","dateAccord":"%s"}""".formatted(aujourdhui)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(patch("/api/devis/{id}/accord-hors-plateforme", idDevis).cookie(agent).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"motif":"Au téléphone","dateAccord":"%s"}""".formatted(LocalDate.now().plusDays(3))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(patch("/api/devis/{id}/accord-hors-plateforme", idDevis).cookie(agent).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"motif":"M. le bailleur, par téléphone","dateAccord":"%s"}""".formatted(aujourdhui)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.statut").value("ACCEPTE"))
                .andExpect(jsonPath("$.data.accordHorsPlateforme").value(true))
                .andExpect(jsonPath("$.data.accordMotif").value("M. le bailleur, par téléphone"));
        assertThat(nombreNotifications("DEVIS_ACCORD_HORS_PLATEFORME:" + idDevis + ":1")).as("le bailleur est prévenu").isEqualTo(1);

        // Un accord n'est pas enregistrable deux fois, et seul le bailleur propriétaire le conteste
        mockMvc.perform(patch("/api/devis/{id}/accord-hors-plateforme", idDevis).cookie(agent).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"motif":"Encore","dateAccord":"%s"}""".formatted(aujourdhui)))
                .andExpect(status().isConflict());
        mockMvc.perform(patch("/api/devis/{id}/contester-accord", idDevis).cookie(agent).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"commentaire":"Non"}"""))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/devis/{id}/contester-accord", idDevis).cookie(bailleur).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"commentaire":"Je n'ai rien accepté"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.statut").value("A_RENEGOCIER"))
                .andExpect(jsonPath("$.data.accordHorsPlateforme").value(false));
        assertThat(jdbc.queryForObject("SELECT commentaire_bailleur FROM devis_travaux WHERE id_devis = ?", String.class, idDevis))
                .contains("Je n'ai rien accepté");
        assertThat(nombreNotificationsCommencantPar("DEVIS_REPONSE:" + idDevis + ":1:A_RENEGOCIER")).as("l'agent est prévenu").isGreaterThanOrEqualTo(1);

        // Le devis est révisé puis un nouvel accord enregistré : la dépense qui en naît porte la mention de l'accord
        mockMvc.perform(patch("/api/devis/{id}", idDevis).cookie(agent).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"montant":48000}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.statut").value("EN_ATTENTE"));
        mockMvc.perform(patch("/api/devis/{id}/accord-hors-plateforme", idDevis).cookie(agent).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"motif":"Confirmé en agence","dateAccord":"%s"}""".formatted(aujourdhui)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/devis/{id}/depense", idDevis).cookie(agent).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isCreated());
        int idDepense = jdbc.queryForObject("SELECT MAX(id_depense) FROM depense_bien", Integer.class);
        assertThat(jdbc.queryForObject("SELECT accord_motif FROM depense_bien WHERE id_depense = ?", String.class, idDepense))
                .contains("hors plateforme").contains("Confirmé en agence");

        // Dépense enregistrée : le bailleur ne peut plus contester
        mockMvc.perform(patch("/api/devis/{id}/contester-accord", idDevis).cookie(bailleur).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"commentaire":"Trop tard"}"""))
                .andExpect(status().isConflict());
    }

    @Test
    void devisPdfJoint_seulUnPdfEstAccepteEtIlNeChangePlusUneFoisLeDevisAccepte() throws Exception {
        preparer(null);
        Cookie agent = connexion(AGENT);
        Cookie bailleur = connexion(emailProprietaire());
        int idDevis = creerDevis(agent, "50000", 1);
        byte[] pdf = "%PDF-1.4\n%contenu de test\n".getBytes(StandardCharsets.ISO_8859_1);

        mockMvc.perform(get("/api/devis/{id}/piece-jointe", idDevis).cookie(agent)).andExpect(status().isNotFound());
        mockMvc.perform(multipart("/api/devis/{id}/piece-jointe", idDevis)
                        .file(new MockMultipartFile("fichier", "devis.pdf", "application/pdf", "pas un pdf".getBytes(StandardCharsets.UTF_8)))
                        .cookie(agent))
                .andExpect(status().isBadRequest());
        mockMvc.perform(multipart("/api/devis/{id}/piece-jointe", idDevis)
                        .file(new MockMultipartFile("fichier", "devis.pdf", "application/pdf", pdf)).cookie(bailleur))
                .andExpect(status().isForbidden());
        mockMvc.perform(multipart("/api/devis/{id}/piece-jointe", idDevis)
                        .file(new MockMultipartFile("fichier", "devis.pdf", "application/pdf", pdf)).cookie(agent))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/devis/{id}", idDevis).cookie(bailleur)).andExpect(jsonPath("$.data.pieceJointe").value(true));
        assertThat(mockMvc.perform(get("/api/devis/{id}/piece-jointe", idDevis).cookie(bailleur))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray()).isEqualTo(pdf);
        mockMvc.perform(get("/api/devis/{id}/piece-jointe", idDevis).cookie(connexion("client2@gestimmo.test"))).andExpect(status().isForbidden());

        // Remplacer ne laisse qu'un seul PDF
        byte[] pdf2 = "%PDF-1.4\n%deuxieme version\n".getBytes(StandardCharsets.ISO_8859_1);
        mockMvc.perform(multipart("/api/devis/{id}/piece-jointe", idDevis)
                        .file(new MockMultipartFile("fichier", "devis2.pdf", "application/pdf", pdf2)).cookie(agent))
                .andExpect(status().isCreated());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM document WHERE entite_type = 'DEVIS_TRAVAUX' AND entite_id = ?", Integer.class, idDevis)).isEqualTo(1);
        assertThat(mockMvc.perform(get("/api/devis/{id}/piece-jointe", idDevis).cookie(agent))
                .andReturn().getResponse().getContentAsByteArray()).isEqualTo(pdf2);

        // Une fois le devis accepté, le document sur lequel porte l'accord est figé
        mockMvc.perform(patch("/api/devis/{id}/accord-hors-plateforme", idDevis).cookie(agent).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"motif":"Au téléphone","dateAccord":"%s"}""".formatted(LocalDate.now())))
                .andExpect(status().isOk());
        mockMvc.perform(multipart("/api/devis/{id}/piece-jointe", idDevis)
                        .file(new MockMultipartFile("fichier", "devis3.pdf", "application/pdf", pdf)).cookie(agent))
                .andExpect(status().isConflict());
        mockMvc.perform(delete("/api/devis/{id}/piece-jointe", idDevis).cookie(agent)).andExpect(status().isConflict());
    }

    @Test
    void devisRetire_lePdfJointEstSupprimeSaufSiLeDevisAvaitEteAccepte() throws Exception {
        preparer(null);
        Cookie agent = connexion(AGENT);
        byte[] pdf = "%PDF-1.4\n%contenu\n".getBytes(StandardCharsets.ISO_8859_1);

        int enAttente = creerDevis(agent, "50000", 1);
        mockMvc.perform(multipart("/api/devis/{id}/piece-jointe", enAttente).file(new MockMultipartFile("fichier", "a.pdf", "application/pdf", pdf)).cookie(agent))
                .andExpect(status().isCreated());
        mockMvc.perform(patch("/api/devis/{id}/annuler", enAttente).cookie(agent).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"commentaire":"Artisan indisponible"}"""))
                .andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM document WHERE entite_type = 'DEVIS_TRAVAUX' AND entite_id = ?", Integer.class, enAttente))
                .as("devis retiré avant tout accord : le PDF ne sert plus").isZero();

        int accepte = creerDevis(agent, "60000", 1);
        mockMvc.perform(multipart("/api/devis/{id}/piece-jointe", accepte).file(new MockMultipartFile("fichier", "b.pdf", "application/pdf", pdf)).cookie(agent))
                .andExpect(status().isCreated());
        jdbc.update("UPDATE devis_travaux SET statut = 'ACCEPTE' WHERE id_devis = ?", accepte);
        mockMvc.perform(patch("/api/devis/{id}/annuler", accepte).cookie(agent).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"commentaire":"Travaux abandonnés"}"""))
                .andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM document WHERE entite_type = 'DEVIS_TRAVAUX' AND entite_id = ?", Integer.class, accepte))
                .as("devis accepté : le PDF reste, il est la preuve de l'accord").isEqualTo(1);
    }

    @Test
    void devisAccepteNonRealise_l_agentEstRappeleSansExpiration() throws Exception {
        preparer(null);
        Cookie agent = connexion(AGENT);
        int idDevis = creerDevis(agent, "50000", 1);
        jdbc.update("UPDATE devis_travaux SET statut = 'ACCEPTE', date_reponse = ? WHERE id_devis = ?",
                Timestamp.valueOf(LocalDateTime.now().minusDays(20)), idDevis);

        devisService.rappelerDevisAcceptesNonRealises();
        assertThat(nombreNotificationsCommencantPar("DEVIS_ACCEPTE_RAPPEL:" + idDevis)).as("pas encore de rappel avant 30 jours").isZero();

        jdbc.update("UPDATE devis_travaux SET date_reponse = ? WHERE id_devis = ?", Timestamp.valueOf(LocalDateTime.now().minusDays(65)), idDevis);
        devisService.rappelerDevisAcceptesNonRealises();
        assertThat(nombreNotifications("DEVIS_ACCEPTE_RAPPEL:" + idDevis + ":2")).isGreaterThanOrEqualTo(1);
        int apres = nombreNotificationsCommencantPar("DEVIS_ACCEPTE_RAPPEL:" + idDevis);
        devisService.rappelerDevisAcceptesNonRealises();
        assertThat(nombreNotificationsCommencantPar("DEVIS_ACCEPTE_RAPPEL:" + idDevis)).as("un seul rappel par période").isEqualTo(apres);

        devisService.expirerDevisSansReponse();
        assertThat(jdbc.queryForObject("SELECT statut FROM devis_travaux WHERE id_devis = ?", String.class, idDevis))
                .as("l'accord du bailleur reste valable").isEqualTo("ACCEPTE");
    }

    // ---------- Mandat échu ----------

    @Test
    void mandatEchu_l_agentEstAlerteEtLesDepensesEnCoursSontSignalees() throws Exception {
        int idMandat = preparer(null);
        Cookie agent = connexion(AGENT);
        int idDepense = creerDepense(agent, "30000", 3, null, null);

        // À 20 jours de la fin : première alerte
        jdbc.update("UPDATE contrat_mandat SET date_fin = ? WHERE id_mandat = ?", Timestamp.valueOf(LocalDateTime.now().plusDays(20)), idMandat);
        alerteAgentService.alerterMandatsAEcheance();
        assertThat(nombreNotificationsCommencantPar("MANDAT_FIN_J30:" + idMandat)).isGreaterThanOrEqualTo(1);
        assertThat(nombreNotificationsCommencantPar("MANDAT_ECHU:" + idMandat)).isZero();
        mockMvc.perform(get("/api/depenses/mandats-echus").cookie(agent)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.idMandat == %d)]".formatted(idMandat)).isEmpty());

        // Mandat échu et toujours actif : alerte, puis une seule par semaine
        jdbc.update("UPDATE contrat_mandat SET date_fin = ? WHERE id_mandat = ?", Timestamp.valueOf(LocalDateTime.now().minusDays(2)), idMandat);
        alerteAgentService.alerterMandatsAEcheance();
        int premiere = nombreNotificationsCommencantPar("MANDAT_ECHU:" + idMandat);
        assertThat(premiere).isGreaterThanOrEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT message FROM notification WHERE cle_alerte LIKE ? AND id_notification > ? LIMIT 1", String.class,
                "MANDAT_ECHU:" + idMandat + "%", derniereNotification)).contains("1 dépense(s)").contains("30000");
        alerteAgentService.alerterMandatsAEcheance();
        assertThat(nombreNotificationsCommencantPar("MANDAT_ECHU:" + idMandat)).as("pas de doublon dans la même semaine").isEqualTo(premiere);

        mockMvc.perform(get("/api/depenses/mandats-echus").cookie(agent)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.idMandat == %d)].nombreDepenses".formatted(idMandat)).value(1))
                .andExpect(jsonPath("$.data[?(@.idMandat == %d)].resteARetenir".formatted(idMandat)).value(30000.0));
        mockMvc.perform(get("/api/depenses/{id}", idDepense).cookie(agent)).andExpect(jsonPath("$.data.mandatEchu").value(true));
        mockMvc.perform(get("/api/depenses/mandats-echus").cookie(connexion(emailProprietaire()))).andExpect(status().isForbidden());

        // Une fois résilié, plus d'alerte : le solde est traité
        resilier(agent, idMandat);
        mockMvc.perform(get("/api/depenses/{id}", idDepense).cookie(agent)).andExpect(jsonPath("$.data.mandatEchu").value(false));
    }

    // ---------- Plafond retirable ----------

    @Test
    void plafond_peutEtreRetireParAvenant() throws Exception {
        int idMandat = preparer(new BigDecimal("100000"));
        Cookie agent = connexion(AGENT);
        mockMvc.perform(post("/api/depenses").cookie(agent).contentType(MediaType.APPLICATION_JSON).content(corpsDepense("200000", null, null)))
                .andExpect(status().isConflict());

        mockMvc.perform(patch("/api/contrats-mandat/{id}/modifier", idMandat).cookie(agent).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"retirerPlafond":true,"plafondDepenseSansAccord":50000,"motif":"Confiance","accordClient":true}"""))
                .andExpect(status().isBadRequest());
        mockMvc.perform(patch("/api/contrats-mandat/{id}/modifier", idMandat).cookie(agent).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"retirerPlafond":true,"motif":"Le bailleur nous fait confiance","accordClient":true}"""))
                .andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT plafond_depense_sans_accord FROM contrat_mandat WHERE id_mandat = ?", BigDecimal.class, idMandat)).isNull();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM avenant_ligne WHERE champ = 'plafondDepenseSansAccord' AND nouvelle_valeur IS NULL "
                + "AND id_avenant IN (SELECT id_avenant FROM avenant WHERE id_mandat = ?)", Integer.class, idMandat)).isEqualTo(1);

        mockMvc.perform(post("/api/depenses").cookie(agent).contentType(MediaType.APPLICATION_JSON).content(corpsDepense("200000", null, null)))
                .andExpect(status().isCreated());

        // Plus de plafond à retirer
        mockMvc.perform(patch("/api/contrats-mandat/{id}/modifier", idMandat).cookie(agent).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"retirerPlafond":true,"motif":"Encore","accordClient":true}"""))
                .andExpect(status().isBadRequest());
    }

    // ---------- Recherche texte ----------

    @Test
    void recherche_parLibelleCourOuBailleurSansJokerInvolontaire() throws Exception {
        preparer(null);
        Cookie agent = connexion(AGENT);
        creerDepense(agent, "30000", 1, null, null);
        String reference = jdbc.queryForObject("SELECT reference_cour FROM cour WHERE id_cour = ?", String.class, COUR);
        String nomBailleur = jdbc.queryForObject("SELECT u.nom FROM cour c JOIN users u ON u.id_user = c.id_user WHERE c.id_cour = ?", String.class, COUR);

        for (String terme : List.of("toiture", "TOITURE", reference, nomBailleur)) {
            mockMvc.perform(get("/api/depenses").param("recherche", terme).param("idCour", String.valueOf(COUR)).cookie(agent))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
        }
        mockMvc.perform(get("/api/depenses").param("recherche", "zzz-introuvable").param("idCour", String.valueOf(COUR)).cookie(agent))
                .andExpect(jsonPath("$.data.content.length()").value(0));
        // Les jokers SQL saisis par l'utilisateur sont des caractères ordinaires
        mockMvc.perform(get("/api/depenses").param("recherche", "%").param("idCour", String.valueOf(COUR)).cookie(agent))
                .andExpect(jsonPath("$.data.content.length()").value(0));
        mockMvc.perform(get("/api/depenses").param("recherche", "_").param("idCour", String.valueOf(COUR)).cookie(agent))
                .andExpect(jsonPath("$.data.content.length()").value(0));
    }

    // ---------- Rétablissement, contestation, recherche des devis ----------

    @Test
    void creanceAbandonnee_peutEtreRetablieParLAdminEtLAbandonResteDansLHistorique() throws Exception {
        int idMandat = preparer(null);
        Cookie agent = connexion(AGENT);
        Cookie admin = connexion(ADMIN);
        int idDepense = creerDepense(agent, "30000", 1, null, null);
        resilier(agent, idMandat);
        mockMvc.perform(patch("/api/depenses/{id}/abandon/retablir", idDepense).cookie(admin).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"motif":"Rien à rétablir"}"""))
                .andExpect(status().isConflict());

        mockMvc.perform(patch("/api/depenses/{id}/abandon", idDepense).cookie(admin).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"motif":"Bailleur introuvable"}"""))
                .andExpect(status().isOk());
        BigDecimal avant = statsFinanceQueries.reversements().getDepensesDuesParBailleurs();

        mockMvc.perform(patch("/api/depenses/{id}/abandon/retablir", idDepense).cookie(agent).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"motif":"Le bailleur paie"}"""))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/depenses/{id}/abandon/retablir", idDepense).cookie(admin).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"motif":" "}"""))
                .andExpect(status().isBadRequest());
        mockMvc.perform(patch("/api/depenses/{id}/abandon/retablir", idDepense).cookie(admin).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"motif":"Le bailleur retrouvé accepte de payer"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.statut").value("A_REGLER"))
                .andExpect(jsonPath("$.data.montantAbandonne").value(0))
                .andExpect(jsonPath("$.data.montantRestant").value(30000))
                .andExpect(jsonPath("$.data.motifAbandon").value("Bailleur introuvable"))
                .andExpect(jsonPath("$.data.motifRetablissement").value("Le bailleur retrouvé accepte de payer"));
        assertThat(statsFinanceQueries.reversements().getDepensesDuesParBailleurs().subtract(avant)).isEqualByComparingTo("30000");
        assertThat(nombreNotificationsCommencantPar("DEPENSE_RETABLIE:" + idDepense)).isEqualTo(1);

        // La dette rétablie se règle de nouveau normalement
        mockMvc.perform(post("/api/depenses/{id}/reglements", idDepense).cookie(agent).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"montant":30000}"""))
                .andExpect(status().isCreated());
        assertThat(statut(idDepense)).isEqualTo("SOLDEE");
    }

    @Test
    void contestation_leBailleurSignaleL_agenceRepondEtRienNEstAnnuleAutomatiquement() throws Exception {
        preparer(null);
        Cookie agent = connexion(AGENT);
        Cookie bailleur = connexion(emailProprietaire());
        int idDepense = creerDepense(agent, "30000", 3, null, null);

        mockMvc.perform(patch("/api/depenses/{id}/contester", idDepense).cookie(agent).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"motif":"Non"}"""))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/depenses/{id}/contester", idDepense).cookie(bailleur).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"motif":""}"""))
                .andExpect(status().isBadRequest());
        mockMvc.perform(patch("/api/depenses/{id}/contester", idDepense).cookie(bailleur).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"motif":"Je n'ai jamais demandé ces travaux"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.contestationOuverte").value(true))
                .andExpect(jsonPath("$.data.motifContestation").value("Je n'ai jamais demandé ces travaux"));
        assertThat(statut(idDepense)).as("rien n'est annulé automatiquement").isEqualTo("EN_COURS");
        assertThat(nombreNotifications("DEPENSE_CONTESTEE:" + idDepense)).as("l'agent du mandat et les admins sont alertés").isGreaterThanOrEqualTo(2);

        mockMvc.perform(patch("/api/depenses/{id}/contester", idDepense).cookie(bailleur).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"motif":"Encore"}"""))
                .andExpect(status().isConflict());
        mockMvc.perform(patch("/api/depenses/{id}/contestation/repondre", idDepense).cookie(bailleur).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"motif":"Moi-même"}"""))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/depenses/{id}/contestation/repondre", idDepense).cookie(agent).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"motif":"Travaux urgents d'étanchéité, devis joint par e-mail"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.contestationOuverte").value(false))
                .andExpect(jsonPath("$.data.reponseContestation").value("Travaux urgents d'étanchéité, devis joint par e-mail"));
        assertThat(nombreNotifications("DEPENSE_CONTESTATION_REPONSE:" + idDepense)).as("le bailleur est prévenu de la réponse").isEqualTo(1);
        mockMvc.perform(patch("/api/depenses/{id}/contestation/repondre", idDepense).cookie(agent).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"motif":"Encore"}"""))
                .andExpect(status().isConflict());
    }

    @Test
    void contestation_uneDepenseIssueD_unDevisAccepteSurLaPlateformeNEstPasContestable() throws Exception {
        preparer(null);
        Cookie agent = connexion(AGENT);
        Cookie bailleur = connexion(emailProprietaire());
        int idDevis = creerDevis(agent, "50000", 1);
        mockMvc.perform(patch("/api/devis/{id}/accepter", idDevis).cookie(bailleur).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"montant":50000,"nombreMois":1}"""))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/devis/{id}/depense", idDevis).cookie(agent).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isCreated());
        int idDepense = jdbc.queryForObject("SELECT MAX(id_depense) FROM depense_bien", Integer.class);

        mockMvc.perform(patch("/api/depenses/{id}/contester", idDepense).cookie(bailleur).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"motif":"Finalement non"}"""))
                .andExpect(status().isConflict());
    }

    @Test
    void rechercheDevis_parLibelleCourOuBailleurSansJokerInvolontaire() throws Exception {
        preparer(null);
        Cookie agent = connexion(AGENT);
        creerDevis(agent, "50000", 1);
        String reference = jdbc.queryForObject("SELECT reference_cour FROM cour WHERE id_cour = ?", String.class, COUR);

        for (String terme : List.of("portail", "PORTAIL", reference)) {
            mockMvc.perform(get("/api/devis").param("recherche", terme).param("idCour", String.valueOf(COUR)).cookie(agent))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
        }
        mockMvc.perform(get("/api/devis").param("recherche", "zzz-introuvable").param("idCour", String.valueOf(COUR)).cookie(agent))
                .andExpect(jsonPath("$.data.content.length()").value(0));
        mockMvc.perform(get("/api/devis").param("recherche", "%").param("idCour", String.valueOf(COUR)).cookie(agent))
                .andExpect(jsonPath("$.data.content.length()").value(0));
    }

    // ---------- Outils ----------

    /** Mandat de gestion mensuel actif sur la cour 3, avec un plafond de dépenses sans accord facultatif. */
    private int preparer(BigDecimal plafond) {
        int idMandat = jdbc.queryForObject(
                "INSERT INTO contrat_mandat (id_cour, id_user, date_debut, type_mandat, commission, mode_facturation, statut, plafond_depense_sans_accord) "
                        + "VALUES (?, ?, ?, 'GESTION', 10, 'MENSUEL', 'ACTIF', ?) RETURNING id_mandat",
                Integer.class, COUR, idAgent(), Date.valueOf(moisCourant), plafond);
        mandatsCrees.add(idMandat);
        return idMandat;
    }

    private String corpsDepense(String montant, Boolean accord, String accordMotif) {
        return """
                {"idCour":%d,"libelle":"Reprise de la toiture","categorie":"ETANCHEITE","montant":%s,"nombreMois":1%s%s}"""
                .formatted(COUR, montant, accord != null ? ",\"accordBailleur\":" + accord : "",
                        accordMotif != null ? ",\"accordMotif\":\"" + accordMotif + "\"" : "");
    }

    private int creerDepense(Cookie agent, String montant, int nombreMois, Boolean accord, String accordMotif) throws Exception {
        mockMvc.perform(post("/api/depenses").cookie(agent).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idCour":%d,"libelle":"Reprise de la toiture","categorie":"ETANCHEITE","montant":%s,"nombreMois":%d%s%s}"""
                                .formatted(COUR, montant, nombreMois, accord != null ? ",\"accordBailleur\":" + accord : "",
                                        accordMotif != null ? ",\"accordMotif\":\"" + accordMotif + "\"" : "")))
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

    private int idAgent() {
        return jdbc.queryForObject("SELECT id_user FROM users WHERE email = ?", Integer.class, AGENT);
    }

    private String emailProprietaire() {
        return jdbc.queryForObject("SELECT u.email FROM cour c JOIN users u ON u.id_user = c.id_user WHERE c.id_cour = ?", String.class, COUR);
    }

    private String statut(int idDepense) {
        return jdbc.queryForObject("SELECT statut FROM depense_bien WHERE id_depense = ?", String.class, idDepense);
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
