package com.immobilier.gestionImmobiliere.modules.documents.services;

import com.immobilier.gestionImmobiliere.donnees.biens.model.Cour;
import com.immobilier.gestionImmobiliere.donnees.depenses.model.DepenseBien;
import com.immobilier.gestionImmobiliere.donnees.depenses.model.DepenseReglement;
import com.immobilier.gestionImmobiliere.donnees.depenses.repository.DepenseReglementRepository;
import com.immobilier.gestionImmobiliere.donnees.documents.model.Document;
import com.immobilier.gestionImmobiliere.donnees.documents.model.TypeDocument;
import com.immobilier.gestionImmobiliere.donnees.documents.model.TypeEntiteDocument;
import com.immobilier.gestionImmobiliere.donnees.documents.repository.DocumentRepository;
import com.immobilier.gestionImmobiliere.exceptions.ResourceNotFoundException;
import com.immobilier.gestionImmobiliere.modules.documents.UtilsDocuments.Utils;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.FontFactory;
import org.openpdf.text.Image;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Rectangle;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Reçu remis au bailleur quand l'agent enregistre son règlement direct d'une dépense (mandat résilié). Généré une seule
 * fois et conservé tel quel ; il n'est plus servi si le règlement est annulé par la suite (saisi par erreur).
 */
@Service
public class ReglementDepenseRecuDocumentService {

    private static final Logger log = LoggerFactory.getLogger(ReglementDepenseRecuDocumentService.class);

    private static final Color COULEUR_ENTETE = new Color(31, 56, 100);
    private static final Color COULEUR_TEXTE = new Color(33, 33, 33);
    private static final Color COULEUR_ACCENT = new Color(55, 86, 35);
    private static final Color COULEUR_FOND_MONTANT = new Color(230, 245, 230);
    private static final String LOGO_PATH = "/image/axios-logo.png";
    private static final DecimalFormat FMT_MONTANT = new DecimalFormat("#,##0");
    private static final DateTimeFormatter FMT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final DepenseReglementRepository reglementRepository;
    private final DocumentRepository documentRepository;
    private final DocumentRegistrar documentRegistrar;
    private final DocumentStorageService documentStorageService;
    private final Utils utils;

    public ReglementDepenseRecuDocumentService(DepenseReglementRepository reglementRepository, DocumentRepository documentRepository,
                                               DocumentRegistrar documentRegistrar, DocumentStorageService documentStorageService, Utils utils) {
        this.reglementRepository = reglementRepository;
        this.documentRepository = documentRepository;
        this.documentRegistrar = documentRegistrar;
        this.documentStorageService = documentStorageService;
        this.utils = utils;
    }

    /** Admin et agent : tous les règlements. Bailleur : ceux des dépenses de ses cours uniquement. */
    @Transactional
    public byte[] genererOuRecuperer(Integer idReglement, Integer currentUserId, boolean isAdminOrAgent) {
        DepenseReglement reglement = reglementRepository.findById(idReglement)
                .orElseThrow(() -> new ResourceNotFoundException("reglement", idReglement));
        DepenseBien depense = reglement.getDepense();
        Cour cour = depense.getCour();
        if (!isAdminOrAgent && (cour.getProprietaire() == null || !cour.getProprietaire().getIdUser().equals(currentUserId))) {
            throw new AccessDeniedException("Vous n'avez pas accès à ce règlement");
        }
        if (Boolean.TRUE.equals(reglement.getAnnule())) {
            throw new IllegalStateException("Ce règlement a été annulé : aucun reçu n'est délivré");
        }

        var existant = documentRepository.findByEntiteTypeAndEntiteIdOrderByCreatedAtDesc(TypeEntiteDocument.REGLEMENT_DEPENSE, idReglement);
        if (!existant.isEmpty()) {
            return documentStorageService.telecharger(existant.getFirst().getCheminFichier());
        }

        byte[] pdf = genererPdf(reglement, depense);
        String cle = documentStorageService.store(pdf, "recus-reglements");
        Document document = Document.builder()
                .typeDocument(TypeDocument.RECU_REGLEMENT_DEPENSE)
                .entiteType(TypeEntiteDocument.REGLEMENT_DEPENSE)
                .entiteId(idReglement)
                .cheminFichier(cle)
                .createdAt(LocalDateTime.now())
                .userCreate(currentUserId)
                .build();
        return documentRegistrar.enregistrer(document, pdf);
    }

    private byte[] genererPdf(DepenseReglement reglement, DepenseBien depense) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        org.openpdf.text.Document document = new org.openpdf.text.Document(PageSize.A4, 30, 30, 45, 40);
        String numero = String.format("GI.BF-REG-%s-%s", LocalDate.now().getYear(), UUID.randomUUID().toString().substring(0, 8).toUpperCase());

        // Ce qui était dû juste après ce règlement : les règlements annulés ou postérieurs ne comptent pas
        BigDecimal dejaRegle = reglementRepository.findByDepense_IdDepenseAndAnnuleFalseOrderByIdReglementAsc(depense.getIdDepense()).stream()
                .filter(r -> r.getIdReglement() <= reglement.getIdReglement())
                .map(DepenseReglement::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal resteDu = depense.getMontant().subtract(depense.getMontantImpute()).subtract(dejaRegle).max(BigDecimal.ZERO);

        try {
            PdfWriter writer = PdfWriter.getInstance(document, out);
            Image logoFiligrane = utils.chargerLogo(LOGO_PATH);
            writer.setPageEvent(new Utils.FiligraneLogo(logoFiligrane, 280, 280));
            document.open();

            utils.ajouterBandeauNational(document, writer);
            utils.ajouterEntete(document);
            utils.ajouterTitre(document, "REÇU DE RÈGLEMENT N° " + numero, "Dépense avancée par l'agence - Gestion Immobilière GI.BF");

            Font fontLabel = FontFactory.getFont(FontFactory.HELVETICA, 8, Font.BOLD, COULEUR_ENTETE);
            Font fontValeur = FontFactory.getFont(FontFactory.HELVETICA, 8, Font.NORMAL, COULEUR_TEXTE);
            PdfPTable tableau = utils.creerTableauInfos();
            tableau.setSpacingBefore(4f);
            tableau.setSpacingAfter(4f);
            Cour cour = depense.getCour();
            utils.ajouterLigneTableau(tableau, "Date du règlement", reglement.getDateReglement().format(FMT_DATE), fontLabel, fontValeur);
            utils.ajouterLigneTableau(tableau, "Reçu de", cour.getProprietaire().getPrenom() + " " + cour.getProprietaire().getNom(), fontLabel, fontValeur);
            utils.ajouterLigneTableau(tableau, "Cour", cour.getReferenceCour(), fontLabel, fontValeur);
            if (depense.getMaison() != null) {
                utils.ajouterLigneTableau(tableau, "Maison", depense.getMaison().getNomCommunMaison(), fontLabel, fontValeur);
            }
            utils.ajouterLigneTableau(tableau, "Dépense", depense.getLibelle() + " (" + depense.getDateDepense().format(FMT_DATE) + ")", fontLabel, fontValeur);
            utils.ajouterLigneTableau(tableau, "Montant de la dépense", FMT_MONTANT.format(depense.getMontant()) + " FCFA", fontLabel, fontValeur);
            utils.ajouterLigneTableau(tableau, "Retenu sur les reversements", FMT_MONTANT.format(depense.getMontantImpute()) + " FCFA", fontLabel, fontValeur);
            utils.ajouterLigneTableau(tableau, "Total réglé directement (ce reçu inclus)", FMT_MONTANT.format(dejaRegle) + " FCFA", fontLabel, fontValeur);
            utils.ajouterLigneTableau(tableau, "Reste dû après ce règlement", FMT_MONTANT.format(resteDu) + " FCFA", fontLabel, fontValeur);
            utils.ajouterLigneTableau(tableau, "Mode de règlement", reglement.getModeReglement() != null ? reglement.getModeReglement() : "-", fontLabel, fontValeur);
            utils.ajouterLigneTableau(tableau, "Référence", reglement.getReference() != null ? reglement.getReference() : "-", fontLabel, fontValeur);
            document.add(tableau);

            ajouterCadreMontant(document, reglement, numero);
            utils.ajouterSignature(document, "Ouagadougou", "L'Agence GI.BF");
        } catch (Exception e) {
            log.error("Erreur génération du reçu de règlement", e);
            throw new RuntimeException("Échec génération du reçu de règlement", e);
        } finally {
            if (document.isOpen()) document.close();
        }
        return out.toByteArray();
    }

    private void ajouterCadreMontant(org.openpdf.text.Document document, DepenseReglement reglement, String numero) throws Exception {
        String donnees = numero + "|reglement:" + reglement.getIdReglement() + "|montant:" + reglement.getMontant()
                + "|date:" + reglement.getDateReglement();
        String payloadQr = donnees + "|" + utils.signer(donnees);

        PdfPTable cadre = new PdfPTable(2);
        cadre.setWidthPercentage(100);
        cadre.setWidths(new float[]{55f, 45f});
        cadre.setSpacingBefore(4f);
        cadre.setSpacingAfter(4f);

        Font fontTexte = FontFactory.getFont(FontFactory.HELVETICA, 8, Font.NORMAL, COULEUR_TEXTE);
        PdfPCell cellQr = new PdfPCell();
        cellQr.setBorder(Rectangle.NO_BORDER);
        PdfPTable sousQr = new PdfPTable(2);
        sousQr.setWidthPercentage(100);
        sousQr.setWidths(new float[]{40f, 60f});
        PdfPCell qr = new PdfPCell(utils.genererQrCode(payloadQr, 70, 70));
        qr.setBorder(Rectangle.NO_BORDER);
        qr.setVerticalAlignment(Element.ALIGN_MIDDLE);
        qr.setPadding(2f);
        sousQr.addCell(qr);
        PdfPCell texte = new PdfPCell();
        texte.setBorder(Rectangle.NO_BORDER);
        texte.setVerticalAlignment(Element.ALIGN_MIDDLE);
        texte.addElement(new Paragraph("N° " + numero, FontFactory.getFont(FontFactory.HELVETICA, 8, Font.BOLD, COULEUR_ENTETE)));
        texte.addElement(new Paragraph("Scannant le QR code.", fontTexte));
        sousQr.addCell(texte);
        cellQr.addElement(sousQr);
        cadre.addCell(cellQr);

        PdfPCell montant = new PdfPCell();
        montant.setBackgroundColor(COULEUR_FOND_MONTANT);
        montant.setPadding(4f);
        Paragraph label = new Paragraph("MONTANT RÉGLÉ", FontFactory.getFont(FontFactory.HELVETICA, 9, Font.BOLD, COULEUR_ACCENT));
        label.setAlignment(Element.ALIGN_CENTER);
        montant.addElement(label);
        Paragraph valeur = new Paragraph(FMT_MONTANT.format(reglement.getMontant()) + " FCFA",
                FontFactory.getFont(FontFactory.HELVETICA, 12, Font.BOLD, COULEUR_ACCENT));
        valeur.setAlignment(Element.ALIGN_CENTER);
        montant.addElement(valeur);
        cadre.addCell(montant);

        document.add(cadre);
    }
}
