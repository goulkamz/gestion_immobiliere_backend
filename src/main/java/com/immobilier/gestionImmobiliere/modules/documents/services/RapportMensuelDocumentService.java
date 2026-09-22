package com.immobilier.gestionImmobiliere.modules.documents.services;

import com.immobilier.gestionImmobiliere.donnees.contrats.repository.DecompteSortieRepository;
import com.immobilier.gestionImmobiliere.donnees.documents.model.Document;
import com.immobilier.gestionImmobiliere.donnees.documents.model.TypeDocument;
import com.immobilier.gestionImmobiliere.donnees.documents.repository.DocumentRepository;
import com.immobilier.gestionImmobiliere.donnees.paiements.repository.EcheanceLoyerRepository;
import com.immobilier.gestionImmobiliere.donnees.paiements.repository.PaiementLocationBienServiceRepository;
import com.immobilier.gestionImmobiliere.donnees.paiements.repository.RemboursementRepository;
import com.immobilier.gestionImmobiliere.modules.documents.UtilsDocuments.Utils;
import com.immobilier.gestionImmobiliere.modules.statistiques.projection.SumRetard;
import com.immobilier.gestionImmobiliere.utils.DateUtils;
import org.openpdf.text.*;
import org.openpdf.text.pdf.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;


import static com.immobilier.gestionImmobiliere.utils.BuildSuccessResponse.buildSuccessResponse;

@Service
public class RapportMensuelDocumentService {

    private static final Logger log = LoggerFactory.getLogger(RapportMensuelDocumentService.class);

    private static final Color COULEUR_ENTETE = new Color(31, 56, 100);
    private static final Color COULEUR_ACCENT = new Color(206, 17, 38);
    private static final String LOGO_PATH     = "/image/axios-logo.png";
    private static final DecimalFormat FMT_MONTANT = new DecimalFormat("#,##0");

    private final EcheanceLoyerRepository echeanceLoyerRepository;
    private final PaiementLocationBienServiceRepository paiementLocationBienServiceRepository;
    private final RemboursementRepository remboursementRepository;
    private final DecompteSortieRepository decompteSortieRepository;
    private final DocumentRepository documentRepository;
    private final DocumentStorageService documentStorageService;
    private final Utils utils;

    public RapportMensuelDocumentService(EcheanceLoyerRepository echeanceLoyerRepository,
                                         PaiementLocationBienServiceRepository paiementLocationBienServiceRepository,
                                         RemboursementRepository remboursementRepository,
                                         DecompteSortieRepository decompteSortieRepository,
                                         DocumentRepository documentRepository,
                                         DocumentStorageService documentStorageService, Utils utils) {
        this.echeanceLoyerRepository = echeanceLoyerRepository;
        this.paiementLocationBienServiceRepository = paiementLocationBienServiceRepository;
        this.remboursementRepository = remboursementRepository;
        this.decompteSortieRepository = decompteSortieRepository;
        this.documentRepository = documentRepository;
        this.documentStorageService = documentStorageService;
        this.utils = utils;
    }


    /**
     * Réservé Admin. Un seul rapport par mois (immuable une fois généré) —
     * appelable à tout moment (avant ou après le 15), mais le job planifié
     * s'assure qu'il existe automatiquement chaque 15 du mois.
     */
    @Transactional
    public ResponseEntity<?> genererOuRecuperer(LocalDate periode, Integer currentUserId) {
        LocalDate debutMois = periode.withDayOfMonth(1);

        var existant = documentRepository.findByTypeDocumentAndPeriodeMois(TypeDocument.RAPPORT_MENSUEL, debutMois);

        Document document = existant.orElseGet(() -> {
            byte[] pdf = genererPdf(debutMois);
            String cle = documentStorageService.store(pdf, "rapports-mensuels");

            Document nouveau = Document.builder()
                    .typeDocument(TypeDocument.RAPPORT_MENSUEL)
                    .periodeMois(debutMois)
                    .cheminFichier(cle)
                    .createdAt(LocalDateTime.now())
                    .userCreate(currentUserId)
                    .build();
            return documentRepository.save(nouveau);
        });

        String url = documentStorageService.genererUrlPresignee(document.getCheminFichier());
        return buildSuccessResponse(HttpStatus.OK, "Rapport mensuel disponible", "RAPPORT_MENSUEL_GENERATED", url);
    }

    // ── Génération PDF (OpenPDF) ─────────────────────────────────────────────

    private byte[] genererPdf(LocalDate periode) {
        String libellePeriode = DateUtils.nomMoisFrancais(periode) + " " + periode.getYear();
        String numero = "GI.BF-RAP-" + periode.getYear() + "-" + String.format("%02d", periode.getMonthValue());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        org.openpdf.text.Document document = new org.openpdf.text.Document(PageSize.A4, 50, 50, 60, 60);

        try {
            PdfWriter writer = PdfWriter.getInstance(document, out);
            org.openpdf.text.Image logoFiligrane = utils.chargerLogo(LOGO_PATH);
            writer.setPageEvent(new Utils.FiligraneLogo(logoFiligrane, 380, 380));

            document.open();

            utils.ajouterBandeauNational(document, writer);
            utils.ajouterEntete(document);
            utils.ajouterTitre(document, "RAPPORT MENSUEL DE GESTION", libellePeriode);

            ajouterSectionLoyers(document, periode);
            ajouterSectionRetards(document);
            ajouterSectionBiensServices(document, periode);
            ajouterSectionDecomptesSortie(document, periode);

            Font fontNote = FontFactory.getFont(FontFactory.HELVETICA, 8, Font.ITALIC, Color.GRAY);
            Paragraph note = new Paragraph(
                    "Rapport généré le 15 du mois : on suppose qu'à cette date tous les paiements " +
                            "et virements du mois sont effectués.", fontNote);
            note.setSpacingBefore(15f);
            document.add(note);

            String donnees = numero + "|" + periode;
            String payloadQr = donnees + "|" + utils.signer(donnees);
            utils.ajouterPied(document, "N° Rapport : " + numero, payloadQr,"","RAPPORT_MENSUEL");

        } catch (Exception e) {
            log.error("Erreur génération PDF rapport mensuel", e);
            throw new RuntimeException("Échec génération rapport mensuel PDF", e);
        } finally {
            if (document.isOpen()) document.close();
        }

        return out.toByteArray();
    }

    private void ajouterSectionLoyers(org.openpdf.text.Document document, LocalDate periode) throws DocumentException {
        ajouterTitreSection(document, "LOYERS & COMMISSIONS");

        PdfPTable tableau = utils.creerTableauInfos();
        Font fontLabel  = FontFactory.getFont(FontFactory.HELVETICA, 9, Font.BOLD, COULEUR_ENTETE);
        Font fontValeur = FontFactory.getFont(FontFactory.HELVETICA, 9, Font.NORMAL, Color.DARK_GRAY);

        ajouterLigneMontant(tableau, "Loyers encaissés", echeanceLoyerRepository.sumLoyersEncaissesDuMois(periode), fontLabel, fontValeur);
        ajouterLigneMontant(tableau, "Pénalités de retard encaissées", echeanceLoyerRepository.sumPenalitesEncaisseesDuMois(periode), fontLabel, fontValeur);
        ajouterLigneMontant(tableau, "Commission de l'agence", echeanceLoyerRepository.sumCommissionAgenceDuMois(periode), fontLabel, fontValeur);
        ajouterLigneMontant(tableau, "Montant dû aux bailleurs", echeanceLoyerRepository.sumMontantDuAuxBailleursDuMois(periode), fontLabel, fontValeur);

        document.add(tableau);
    }

    private void ajouterSectionRetards(org.openpdf.text.Document document) throws DocumentException {
        ajouterTitreSection(document, "ÉCHÉANCES EN RETARD (situation actuelle)");

        SumRetard retard = echeanceLoyerRepository.sumEnRetard();
        PdfPTable tableau = utils.creerTableauInfos();
        Font fontLabel  = FontFactory.getFont(FontFactory.HELVETICA, 9, Font.BOLD, COULEUR_ENTETE);
        Font fontValeur = FontFactory.getFont(FontFactory.HELVETICA, 9, Font.NORMAL, Color.DARK_GRAY);

        utils.ajouterLigneTableau(tableau, "Nombre d'échéances en retard", String.valueOf(retard.getNombre()), fontLabel, fontValeur);
        ajouterLigneMontant(tableau, "Montant total en retard", retard.getMontant(), fontLabel, fontValeur);

        document.add(tableau);
    }

    private void ajouterSectionBiensServices(org.openpdf.text.Document document, LocalDate periode) throws DocumentException {
        ajouterTitreSection(document, "LOCATIONS BIENS & SERVICES");

        PdfPTable tableau = utils.creerTableauInfos();
        Font fontLabel  = FontFactory.getFont(FontFactory.HELVETICA, 9, Font.BOLD, COULEUR_ENTETE);
        Font fontValeur = FontFactory.getFont(FontFactory.HELVETICA, 9, Font.NORMAL, Color.DARK_GRAY);

        ajouterLigneMontant(tableau, "Total encaissé", paiementLocationBienServiceRepository.sumEncaisseBienServiceDuMois(periode), fontLabel, fontValeur);
        ajouterLigneMontant(tableau, "Total remboursé", remboursementRepository.sumRembourseDuMois(periode), fontLabel, fontValeur);

        document.add(tableau);
    }

    private void ajouterSectionDecomptesSortie(org.openpdf.text.Document document, LocalDate periode) throws DocumentException {
        ajouterTitreSection(document, "DÉCOMPTES DE SORTIE RÉGLÉS CE MOIS");

        var decomptesRegles = decompteSortieRepository.findReglesDuMois(periode);

        Font fontTexte = FontFactory.getFont(FontFactory.HELVETICA, 9, Font.NORMAL, Color.DARK_GRAY);
        Paragraph resume = new Paragraph(decomptesRegles.size() + " décompte(s) réglé(s) durant cette période.", fontTexte);
        resume.setSpacingAfter(4f);
        document.add(resume);

        BigDecimal totalRembourseLocataires = decomptesRegles.stream()
                .map(d -> d.getMontantARembourser() != null ? d.getMontantARembourser() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalManquantsLocataires = decomptesRegles.stream()
                .map(d -> d.getMontantManquant() != null ? d.getMontantManquant() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        PdfPTable tableau = utils.creerTableauInfos();
        Font fontLabel = FontFactory.getFont(FontFactory.HELVETICA, 9, Font.BOLD, COULEUR_ENTETE);
        ajouterLigneMontant(tableau, "Total remboursé aux locataires", totalRembourseLocataires, fontLabel, fontTexte);
        ajouterLigneMontant(tableau, "Total manquants facturés", totalManquantsLocataires, fontLabel, fontTexte);

        document.add(tableau);
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private void ajouterTitreSection(org.openpdf.text.Document document, String titre) throws DocumentException {
        Font fontSection = FontFactory.getFont(FontFactory.HELVETICA, 11, Font.BOLD, COULEUR_ACCENT);
        Paragraph p = new Paragraph(titre, fontSection);
        p.setSpacingBefore(14f);
        p.setSpacingAfter(4f);
        document.add(p);
    }

    private void ajouterLigneMontant(PdfPTable tableau, String libelle, BigDecimal montant, Font fontLabel, Font fontValeur) {
        String valeur = FMT_MONTANT.format(montant != null ? montant : BigDecimal.ZERO) + " FCFA";
        utils.ajouterLigneTableau(tableau, libelle, valeur, fontLabel, fontValeur);
    }

}
