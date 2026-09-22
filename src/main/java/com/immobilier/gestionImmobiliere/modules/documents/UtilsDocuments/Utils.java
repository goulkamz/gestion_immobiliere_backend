package com.immobilier.gestionImmobiliere.modules.documents.UtilsDocuments;


import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.immobilier.gestionImmobiliere.donnees.contrats.model.ContratLocation;
import org.openpdf.text.*;
import org.openpdf.text.Font;
import org.openpdf.text.Image;
import org.openpdf.text.Rectangle;
import org.openpdf.text.pdf.*;
import java.awt.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

@Component
public class Utils {
    @Value("${documents.secret}")
    private String secretDocuments;

    private static final Logger log = LoggerFactory.getLogger(Utils.class);
    private static final DateTimeFormatter FMT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ── Couleurs institutionnelles PISTE ─────────────────────────────────────
    private static final Color COULEUR_ENTETE    = new Color(31, 56, 100);
    private static final Color COULEUR_TEXTE     = new Color(33, 33, 33);
    private static final Color COULEUR_ACCENT    = new Color(206, 17, 38);
    private static final Color COULEUR_VERT      = new Color(0, 122, 61);
    private static final Color COULEUR_FOND_HASH = new Color(242, 242, 242);
    private static final Color COULEUR_BORDURE   = new Color(220, 220, 220);
    private static final Color COULEUR_FOND_LIGNE = new Color(248, 249, 250);
    private static final Color COULEUR_GRIS      = new Color(128, 128, 128);

    // ── Ressources graphiques ────────────────────────────────────────────────
    private static final String LOGO_PATH      = "/image/axios-logo.png";
    private static final String ARMOIRIE_PATH  = "/image/armoiries.png";
    private static final String ETOILE_PATH    = "/image/etoile.png";

    // ═════════════════════════════════════════════════════════════════════════
    //  CHARGEMENT D'IMAGES
    // ═════════════════════════════════════════════════════════════════════════

    public Image chargerLogo(String chemin) throws DocumentException, IOException {
        var url = getClass().getResource(chemin);
        if (url == null) {
            log.warn("Image introuvable au chemin {}", chemin);
            return null;
        }
        Image img = Image.getInstance(url);
        img.scaleToFit(65f, 65f);
        return img;
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  BANDEAU NATIONAL
    // ═════════════════════════════════════════════════════════════════════════

    public void ajouterBandeauNational(Document document, PdfWriter writer)
            throws DocumentException, IOException {

        PdfPTable bandeau = new PdfPTable(1);
        bandeau.setWidthPercentage(100);
        bandeau.setWidths(new float[]{100});

        PdfPCell rouge = new PdfPCell(new Phrase(" "));
        rouge.setBackgroundColor(COULEUR_ACCENT);
        rouge.setFixedHeight(4f);
        rouge.setBorder(Rectangle.NO_BORDER);
        bandeau.addCell(rouge);

        PdfPCell vert = new PdfPCell(new Phrase(" "));
        vert.setBackgroundColor(COULEUR_VERT);
        vert.setFixedHeight(4f);
        vert.setBorder(Rectangle.NO_BORDER);
        bandeau.addCell(vert);

        float yHautBandeau = writer.getVerticalPosition(true);
        document.add(bandeau);

        float yFrontiere = yHautBandeau - 4f;

        Image etoile = chargerLogo(ETOILE_PATH);
        if (etoile == null) return;

        float tailleEtoile = 10f;
        etoile.scaleToFit(tailleEtoile, tailleEtoile);

        float pageWidth = document.getPageSize().getWidth();
        float xEtoile = (pageWidth - etoile.getScaledWidth()) / 2f;
        float yEtoile = yFrontiere - (etoile.getScaledHeight() / 2f);

        etoile.setAbsolutePosition(xEtoile, yEtoile);
        PdfContentByte cb = writer.getDirectContent();
        cb.addImage(etoile);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  EN-TÊTE INSTITUTIONNEL
    // ═════════════════════════════════════════════════════════════════════════

    public void ajouterEntete(Document document) throws DocumentException, IOException {

        PdfPTable entete = new PdfPTable(3);
        entete.setWidthPercentage(100);
        entete.setWidths(new float[]{18f, 62f, 25f});
        entete.setSpacingAfter(3f);

        // Colonne 1 : Logo
        PdfPCell cellLogo = new PdfPCell();
        cellLogo.setBorder(Rectangle.NO_BORDER);
        cellLogo.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cellLogo.setHorizontalAlignment(Element.ALIGN_LEFT);
        Image logo = chargerLogo(LOGO_PATH);
        if (logo != null) {
            logo.setAlignment(Image.ALIGN_LEFT);
            cellLogo.addElement(logo);
        }
        entete.addCell(cellLogo);

        // Colonne 2 : Institution
        Font fontInstitution = FontFactory.getFont(FontFactory.HELVETICA, 9, Font.BOLD, COULEUR_ENTETE);
        Font fontMinistere   = FontFactory.getFont(FontFactory.HELVETICA, 9, Font.NORMAL, COULEUR_TEXTE);

        PdfPCell cellCentre = new PdfPCell();
        cellCentre.setBorder(Rectangle.NO_BORDER);
        cellCentre.setVerticalAlignment(Element.ALIGN_TOP);
        cellCentre.setHorizontalAlignment(Element.ALIGN_CENTER);

        cellCentre.addElement(new Paragraph("AGENCE IMMOBILIERE", fontInstitution));
        cellCentre.addElement(new Paragraph("            -------------   ", fontMinistere));
        cellCentre.addElement(new Paragraph("GENERALE IMMOBILIER", fontInstitution));
        cellCentre.addElement(new Paragraph("            -------------    ", fontMinistere));
        cellCentre.addElement(new Paragraph("  GI.BF ", fontInstitution));
        entete.addCell(cellCentre);

        // Colonne 3 : Armoirie
        PdfPCell cellArmoirie = new PdfPCell();
        cellArmoirie.setBorder(Rectangle.NO_BORDER);
        cellArmoirie.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cellArmoirie.setHorizontalAlignment(Element.ALIGN_CENTER);
        Image armoirie = chargerLogo(ARMOIRIE_PATH);
        if (armoirie != null) {
            armoirie.setAlignment(Image.ALIGN_CENTER);
            cellArmoirie.addElement(armoirie);
        }
        entete.addCell(cellArmoirie);

        // Ligne 2 : devise
        entete.addCell(celluleVide());
        entete.addCell(celluleVide());

        Font fontDevise = FontFactory.getFont(FontFactory.HELVETICA, 9, Font.ITALIC, COULEUR_ACCENT);
        PdfPCell cellDroite = new PdfPCell();
        cellDroite.setBorder(Rectangle.NO_BORDER);
        cellDroite.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cellDroite.setHorizontalAlignment(Element.ALIGN_CENTER);
        cellDroite.setPaddingTop(4f);
        cellDroite.addElement(creerParagrapheAligne(
                "La Patrie ou la Mort, nous Vaincrons", fontDevise, Element.ALIGN_CENTER));
        entete.addCell(cellDroite);

        document.add(entete);
    }

    private PdfPCell celluleVide() {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        return cell;
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  TITRE
    // ═════════════════════════════════════════════════════════════════════════

    public void ajouterTitre(Document document, String titre, String sousTitre)
            throws DocumentException {

        Font fontTitre = FontFactory.getFont(FontFactory.TIMES_ROMAN, 14, Font.BOLD, COULEUR_ENTETE);
        Paragraph pTitre = new Paragraph("****** " + titre + " ******", fontTitre);
        pTitre.setAlignment(Element.ALIGN_CENTER);
        pTitre.setSpacingBefore(5f);
        pTitre.setSpacingAfter(2f);
        document.add(pTitre);

        if (sousTitre != null && !sousTitre.isBlank()) {
            Font fontSousTitre = FontFactory.getFont(FontFactory.HELVETICA, 11, Font.ITALIC, COULEUR_ACCENT);
            Paragraph pSousTitre = new Paragraph(sousTitre, fontSousTitre);
            pSousTitre.setAlignment(Element.ALIGN_CENTER);
            pSousTitre.setSpacingAfter(10f);
            document.add(pSousTitre);
        }

        ajouterSeparateur(document, COULEUR_ENTETE);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  SÉPARATEUR
    // ═════════════════════════════════════════════════════════════════════════

    public void ajouterSeparateur(Document document, Color couleur) throws DocumentException {
        Paragraph sep = new Paragraph(" ");
        sep.setSpacingBefore(1f);
        sep.setSpacingAfter(1f);
        document.add(sep);

        PdfPTable ligne = new PdfPTable(1);
        ligne.setWidthPercentage(100);
        PdfPCell cell = new PdfPCell(new Phrase(" "));
        cell.setBackgroundColor(couleur);
        cell.setFixedHeight(2f);
        cell.setBorder(Rectangle.NO_BORDER);
        ligne.addCell(cell);
        document.add(ligne);

        Paragraph apres = new Paragraph(" ");
        apres.setSpacingAfter(1f);
        document.add(apres);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  QR CODE
    // ═════════════════════════════════════════════════════════════════════════


    public Image genererQrCode(String url, float width, float height) throws Exception {
        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix matrix = writer.encode(url, BarcodeFormat.QR_CODE, 120, 120);
        int w = matrix.getWidth();
        int h = matrix.getHeight();

        // Pixel manuel noir/blanc → pas de PixelGrabber (non fiable en headless),
        // pas de PNG (bug de filtre scanline) : contrôle total du buffer
        byte[] rgb = new byte[w * h * 3];
        int idx = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                byte v = matrix.get(x, y) ? (byte) 0x00 : (byte) 0xFF;
                rgb[idx++] = v;
                rgb[idx++] = v;
                rgb[idx++] = v;
            }
        }

        Image qr = Image.getInstance(w, h, 3, 8, rgb);
        qr.scaleToFit(width, height);
        return qr;
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  FILIGRANE
    // ═════════════════════════════════════════════════════════════════════════

    public static class FiligraneLogo extends PdfPageEventHelper {
        private final Image logoWatermark;

        public FiligraneLogo(Image logo, float fitWidth, float fitHeight) {
            this.logoWatermark = logo;
            if (logoWatermark != null) {
                logoWatermark.scaleToFit(fitWidth, fitHeight);
            }
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            if (logoWatermark == null) return;
            PdfContentByte canvas = writer.getDirectContentUnder();
            PdfGState gs = new PdfGState();
            gs.setFillOpacity(0.06f);
            canvas.saveState();
            canvas.setGState(gs);
            float x = (document.getPageSize().getWidth() - logoWatermark.getScaledWidth()) / 2;
            float y = (document.getPageSize().getHeight() - logoWatermark.getScaledHeight()) / 2;
            try {
                canvas.addImage(logoWatermark, logoWatermark.getScaledWidth(), 0, 0,
                        logoWatermark.getScaledHeight(), x, y);
            } catch (DocumentException e) {
                throw new RuntimeException(e);
            }
            canvas.restoreState();
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  SIGNATURE
    // ═════════════════════════════════════════════════════════════════════════

    public void ajouterSignature(Document document, String lieu, String leSignataire)
            throws DocumentException {

        Font fontDate = FontFactory.getFont(FontFactory.HELVETICA, 10, Font.NORMAL, COULEUR_TEXTE);

        PdfPTable tableSignature = new PdfPTable(2);
        tableSignature.setWidthPercentage(100);
        tableSignature.setWidths(new float[]{50f, 50f});
        tableSignature.setSpacingBefore(30f);
        tableSignature.setSpacingAfter(10f);

        PdfPCell cellDate = new PdfPCell();
        cellDate.setBorder(Rectangle.NO_BORDER);
        cellDate.addElement(new Paragraph(
                lieu + ", le " + LocalDate.now().format(FMT_DATE), fontDate));
        tableSignature.addCell(cellDate);

        PdfPCell cellSignature = new PdfPCell();
        cellSignature.setBorder(Rectangle.NO_BORDER);
        cellSignature.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cellSignature.addElement(creerParagrapheAligne(
                leSignataire, fontDate, Element.ALIGN_CENTER));
        for (int i = 0; i < 4; i++) {
            cellSignature.addElement(creerParagrapheAligne(" ", fontDate, Element.ALIGN_CENTER));
        }
        tableSignature.addCell(cellSignature);

        document.add(tableSignature);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  TABLEAU CLÉ / VALEUR
    // ═════════════════════════════════════════════════════════════════════════

    public PdfPTable creerTableauInfos() {
        PdfPTable tableau = new PdfPTable(2);
        tableau.setWidthPercentage(100);
        try {
            tableau.setWidths(new float[]{35f, 65f});
        } catch (DocumentException e) {
            throw new RuntimeException(e);
        }
        return tableau;
    }

    public void ajouterLigneTableau(PdfPTable tableau, String label, String valeur,
                                    Font fontLabel, Font fontValeur) {

        PdfPCell cellLabel = new PdfPCell(new Phrase(label, fontLabel));
        cellLabel.setBorder(Rectangle.BOTTOM);
        cellLabel.setBorderColor(COULEUR_BORDURE);
        cellLabel.setPadding(6f);
        cellLabel.setBackgroundColor(COULEUR_FOND_LIGNE);
        tableau.addCell(cellLabel);

        PdfPCell cellValeur = new PdfPCell(new Phrase(valeur, fontValeur));
        cellValeur.setBorder(Rectangle.BOTTOM);
        cellValeur.setBorderColor(COULEUR_BORDURE);
        cellValeur.setPadding(6f);
        tableau.addCell(cellValeur);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  PIED DE PAGE
    // ═════════════════════════════════════════════════════════════════════════

    public void ajouterPied(Document document, String numero, String donnees,
                            String urlVerificationBase, String typeAttestation)
            throws Exception {

        ajouterSeparateur(document, COULEUR_FOND_HASH);

        Font fontPied   = FontFactory.getFont(FontFactory.HELVETICA, 8, Font.NORMAL, COULEUR_GRIS);
        Font fontNumero = FontFactory.getFont(FontFactory.HELVETICA, 8, Font.BOLD, COULEUR_ENTETE);


        PdfPTable tablePied = new PdfPTable(2);
        tablePied.setWidthPercentage(100);
        tablePied.setWidths(new float[]{78f, 22f});

        PdfPCell cellTexte = new PdfPCell();
        cellTexte.setPadding(8f);
        cellTexte.setBorder(Rectangle.NO_BORDER);
        cellTexte.addElement(new Paragraph( numero, fontNumero));
        cellTexte.addElement(new Paragraph(
                "Généré automatiquement par la plateforme de gestion immobilière GI.BF - Burkina Faso", fontPied));
        tablePied.addCell(cellTexte);

        PdfPCell cellQr = new PdfPCell();
        cellQr.setBorder(Rectangle.NO_BORDER);
        cellQr.setHorizontalAlignment(Element.ALIGN_CENTER);
        cellQr.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cellQr.addElement(genererQrCode(donnees, 70, 70));
        tablePied.addCell(cellQr);

        document.add(tablePied);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  HELPERS
    // ═════════════════════════════════════════════════════════════════════════

    public Paragraph creerParagrapheAligne(String texte, Font font, int alignement) {
        Paragraph p = new Paragraph(texte, font);
        p.setAlignment(alignement);
        return p;
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  HASH SHA-256
    // ═════════════════════════════════════════════════════════════════════════

    public String signer(String donnees) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secretDocuments.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] sig = mac.doFinal(donnees.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(sig).substring(0, 12);
        } catch (Exception e) {
            throw new IllegalStateException("Erreur signature HMAC", e);
        }
    }

}