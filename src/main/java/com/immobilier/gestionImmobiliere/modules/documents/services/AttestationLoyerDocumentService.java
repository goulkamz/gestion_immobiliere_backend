package com.immobilier.gestionImmobiliere.modules.documents.services;

import com.immobilier.gestionImmobiliere.donnees.contrats.model.ContratLocation;
import com.immobilier.gestionImmobiliere.donnees.contrats.repository.ContratLocationRepository;
import com.immobilier.gestionImmobiliere.donnees.paiements.repository.EcheanceLoyerRepository;
import com.immobilier.gestionImmobiliere.exceptions.ResourceNotFoundException;
import com.immobilier.gestionImmobiliere.modules.documents.UtilsDocuments.Utils;

import org.openpdf.text.Font;
import org.openpdf.text.Image;
import org.openpdf.text.Paragraph;
import org.openpdf.text.*;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class AttestationLoyerDocumentService {

    private static final Logger log = LoggerFactory.getLogger(AttestationLoyerDocumentService.class);
    private static final DateTimeFormatter FMT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ContratLocationRepository contratLocationRepository;
    private final EcheanceLoyerRepository echeanceLoyerRepository;
    private final Utils utils;

    // ── Constantes SPÉCIFIQUES à ce document (le reste vient de Utils) ──────
    private static final String URL_VERIFICATION_BASE = "https://gi.bf/verifier-attestation";
    private static final String TYPE_ATTESTATION      = "ATTESTATION_LOYER";
    private static final String PREFIXE_NUMERO        = "GI.BF-ATT";
    private static final String LOGO_PATH      = "/image/axios-logo.png";
    private static final Color COULEUR_TEXTE     = new Color(33, 33, 33);
    private static final Color COULEUR_ENTETE    = new Color(31, 56, 100);


    // Couleurs locales très spécifiques au statut de paiement
    // (elles pourraient aussi aller dans Utils si réutilisées ailleurs)
    private static final Color COULEUR_RETARD = new Color(206, 17, 38);
    private static final Color COULEUR_OK     = new Color(0, 122, 61);


    public AttestationLoyerDocumentService(ContratLocationRepository contratLocationRepository,
                                           EcheanceLoyerRepository echeanceLoyerRepository, Utils utils) {
        this.contratLocationRepository = contratLocationRepository;
        this.echeanceLoyerRepository = echeanceLoyerRepository;
        this.utils = utils;
    }


    /**
     * Toujours régénérée à la demande, jamais stockée : reflète la situation
     * de paiement ACTUELLE, pas un instant figé dans le passé. Aucune écriture
     * en base, aucun envoi à MinIO — retourne directement les octets du PDF.
     */
    @PreAuthorize(
            "hasAnyRole('ADMIN','AGENT') " +
                    "or @contratLocationSecurity.isAccessible(#idContrat, authentication.principal.idUser)"
    )
    public byte[] genererAttestation(Integer idContrat) {
        ContratLocation contrat = contratLocationRepository.findById(idContrat)
                .orElseThrow(() -> new ResourceNotFoundException("contrat de location", idContrat));

        BigDecimal arrieres = echeanceLoyerRepository.sumArrieresParContrat(idContrat);
        if (arrieres == null) arrieres = BigDecimal.ZERO;

        return genererPdf(contrat, arrieres);
    }

    // ── Génération PDF ────────────────────────────────────────────────────────

    private byte[] genererPdf(ContratLocation contrat, BigDecimal arrieres) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 50, 50, 60, 60);

        String numero = genererNumero(contrat);
        String hash   = utils.calculerHashLoyer(numero, contrat, arrieres);

        try {
            PdfWriter writer = PdfWriter.getInstance(document, out);
            writer.setEncryption(null, null,
                    PdfWriter.ALLOW_PRINTING | PdfWriter.ALLOW_COPY,
                    PdfWriter.ENCRYPTION_AES_128);

            // Filigrane (logo Utils)
            Image logoFiligrane = utils.chargerLogo(LOGO_PATH);
            writer.setPageEvent(new Utils.FiligraneLogo(logoFiligrane, 380, 380));

            document.open();

            // 1) Bandeau national (rouge/vert + étoile)
            utils.ajouterBandeauNational(document, writer);

            // 2) En-tête institutionnel GI.BF
            utils.ajouterEntete(document);

            // 3) Titre + sous-titre + séparateur
            utils.ajouterTitre(document,
                    "ATTESTATION DE LOYER",
                    "Agence Générale Immobilière GI.BF");

            // 4) Corps
            ajouterCorps(document, contrat, arrieres);

            // 5) Signature
            utils.ajouterSignature(document, "Ouagadougou",
                    "Le responsable de l'agence GI.BF");

            // 6) Pied avec QR + numéro + hash
            utils.ajouterPied(document, numero, hash,
                    URL_VERIFICATION_BASE, TYPE_ATTESTATION);

        } catch (Exception e) {
            log.error("Erreur génération PDF attestation de loyer", e);
            throw new RuntimeException("Échec génération attestation loyer PDF", e);
        } finally {
            if (document.isOpen()) document.close();
        }

        return out.toByteArray();
    }

    // ── Corps du document ─────────────────────────────────────────────────────

    private void ajouterCorps(Document document, ContratLocation contrat,
                              BigDecimal arrieres) throws DocumentException {

        // Polices : on réutilise les couleurs de Utils
        Font fontNormal = FontFactory.getFont(FontFactory.HELVETICA, 11,
                Font.NORMAL, COULEUR_TEXTE);
        Font fontGras   = FontFactory.getFont(FontFactory.HELVETICA, 11,
                Font.BOLD, COULEUR_TEXTE);
        Font fontLabel  = FontFactory.getFont(FontFactory.HELVETICA, 10,
                Font.BOLD, COULEUR_ENTETE);

        var maison    = contrat.getMaison();
        var cour      = maison.getCour();
        var locataire = contrat.getLocataire();
        var bailleur  = cour.getProprietaire();

        // Intro
        Paragraph intro = new Paragraph(
                "Je soussigné(e) " + bailleur.getNom() + " " + bailleur.getPrenom()
                        + ", bailleur, atteste que :", fontNormal);
        intro.setSpacingBefore(15f);
        intro.setSpacingAfter(20f);
        document.add(intro);

        // Tableau infos
        PdfPTable tableau = utils.creerTableauInfos();
        tableau.setSpacingAfter(20f);

        String periode = contrat.getStatut().name().equals("ACTIF")
                ? "depuis le " + contrat.getDateEntree().format(FMT_DATE)
                : "du " + contrat.getDateEntree().format(FMT_DATE)
                + " au " + (contrat.getDateSortie() != null
                ? contrat.getDateSortie().format(FMT_DATE) : "—");

        utils.ajouterLigneTableau(tableau, "Locataire :",
                locataire.getNom() + " " + locataire.getPrenom(), fontLabel, fontGras);
        utils.ajouterLigneTableau(tableau, "Logement :",
                maison.getNomCommunMaison(), fontLabel, fontNormal);
        utils.ajouterLigneTableau(tableau, "Cour :",
                cour.getReferenceCour(), fontLabel, fontNormal);
        utils.ajouterLigneTableau(tableau, "Période :", periode, fontLabel, fontNormal);
        utils.ajouterLigneTableau(tableau, "Loyer mensuel :",
                contrat.getMontantLoyer() + " FCFA", fontLabel, fontGras);
        document.add(tableau);

        // Statut paiement (coloré selon situation)
        boolean enRetard = arrieres.compareTo(BigDecimal.ZERO) > 0;
        Font fontStatut = FontFactory.getFont(FontFactory.HELVETICA, 11, Font.BOLD,
                enRetard ? COULEUR_RETARD : COULEUR_OK);

        Paragraph statut = new Paragraph(
                enRetard
                        ? "À la date de ce document, le/la locataire présente un solde impayé de "
                        + arrieres + " FCFA."
                        : "À la date de ce document, le/la locataire est à jour de ses paiements.",
                fontStatut);
        statut.setSpacingAfter(15f);
        document.add(statut);

        // Conclusion
        Paragraph conclusion = new Paragraph(
                "Cette attestation est délivrée à la demande de l'intéressé(e) pour servir "
                        + "et valoir ce que de droit, à la date du "
                        + LocalDate.now().format(FMT_DATE) + ".", fontNormal);
        conclusion.setAlignment(Element.ALIGN_JUSTIFIED);
        document.add(conclusion);
    }

    // ── Numérotation ──────────────────────────────────────────────────────────

    private String genererNumero(ContratLocation contrat) {
        return String.format("%s-%s-%s",
                PREFIXE_NUMERO,
                LocalDate.now().getYear(),
                UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    }

//    private byte[] genererPdf(ContratLocation contrat, BigDecimal arrieres) {
//        PdfBuilder builder = new PdfBuilder("ATTESTATION DE LOYER");
//
//        var maison = contrat.getMaison();
//        var cour = maison.getCour();
//        var locataire = contrat.getLocataire();
//        var bailleur = cour.getProprietaire();
//        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
//
//        builder.document.add(new Paragraph(
//                "Je soussigné(e) " + bailleur.getNom() + " " + bailleur.getPrenom() +
//                        ", bailleur, atteste que :")
//                .setMarginTop(10));
//
//        builder.document.add(new Paragraph(
//                locataire.getNom() + " " + locataire.getPrenom() +
//                        (contrat.getStatut().name().equals("ACTIF")
//                                ? " est locataire depuis le " + contrat.getDateEntree().format(fmt)
//                                : " a été locataire du " + contrat.getDateEntree().format(fmt) +
//                                " au " + (contrat.getDateSortie() != null ? contrat.getDateSortie().format(fmt) : "—")) +
//                        " du logement suivant : " + maison.getNomCommunMaison() +
//                        ", situé dans la cour " + cour.getReferenceCour() +
//                        ", moyennant un loyer mensuel de " + contrat.getMontantLoyer() + " FCFA.")
//                .setMarginTop(15));
//
//        String statutPaiement = arrieres.compareTo(BigDecimal.ZERO) > 0
//                ? "présente à ce jour un solde impayé de " + arrieres + " FCFA."
//                : "est à jour de ses paiements de loyer à la date de ce jour.";
//
//        builder.document.add(new Paragraph("À la date de ce document, le/la locataire " + statutPaiement)
//                .setFontColor(arrieres.compareTo(BigDecimal.ZERO) > 0 ? ColorConstants.RED : ColorConstants.DARK_GRAY)
//                .setMarginTop(15));
//
//        builder.document.add(new Paragraph(
//                "Cette attestation est délivrée à la demande de l'intéressé(e) pour servir et valoir ce que de droit, " +
//                        "à la date du " + LocalDate.now().format(fmt) + ".")
//                .setFontSize(9).setMarginTop(25));
//
//        return builder.genererEtFermer();
//    }


}