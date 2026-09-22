package com.immobilier.gestionImmobiliere.modules.documents.services;

import com.immobilier.gestionImmobiliere.donnees.contrats.model.ContratLocation;
import com.immobilier.gestionImmobiliere.donnees.contrats.repository.ContratLocationRepository;
import com.immobilier.gestionImmobiliere.donnees.documents.model.Document;
import com.immobilier.gestionImmobiliere.donnees.documents.model.TypeDocument;
import com.immobilier.gestionImmobiliere.donnees.documents.model.TypeEntiteDocument;
import com.immobilier.gestionImmobiliere.donnees.documents.repository.DocumentRepository;
import com.immobilier.gestionImmobiliere.donnees.user.model.User;
import com.immobilier.gestionImmobiliere.exceptions.ResourceNotFoundException;
import com.immobilier.gestionImmobiliere.modules.documents.UtilsDocuments.Utils;
import org.openpdf.text.*;
import org.openpdf.text.pdf.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static com.immobilier.gestionImmobiliere.utils.BuildSuccessResponse.buildSuccessResponse;

@Service
public class ContratLocationDocumentService {

    private static final Logger log = LoggerFactory.getLogger(ContratLocationDocumentService.class);

    private static final Color COULEUR_ENTETE = new Color(31, 56, 100);
    private static final Color COULEUR_ACCENT = new Color(206, 17, 38);
    private static final String LOGO_PATH     = "/image/axios-logo.png";
    private static final DecimalFormat FMT_MONTANT = new DecimalFormat("#,##0");
    private static final DateTimeFormatter FMT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ContratLocationRepository contratLocationRepository;
    private final DocumentRepository documentRepository;
    private final DocumentStorageService documentStorageService;
    private final Utils utils;

    public ContratLocationDocumentService(ContratLocationRepository contratLocationRepository,
                                          DocumentRepository documentRepository,
                                          DocumentStorageService documentStorageService,
                                          Utils utils) {
        this.contratLocationRepository = contratLocationRepository;
        this.documentRepository = documentRepository;
        this.documentStorageService = documentStorageService;
        this.utils = utils;
    }

    /**
     * Génère le contrat une seule fois (immuable, document juridique) —
     * si déjà généré, renvoie le même document sans le recréer.
     */
    @PreAuthorize(
            "hasAnyRole('ADMIN','AGENT') " +
                    "or @contratLocationSecurity.isAccessible(#idContrat, authentication.principal.idUser)"
    )
    @Transactional
    public ResponseEntity<?> genererOuRecuperer(Integer idContrat, Integer currentUserId) {
        var existant = documentRepository.findByEntiteTypeAndEntiteIdOrderByCreatedAtDesc(
                TypeEntiteDocument.CONTRAT_LOCATION, idContrat);

        Document document;
        if (!existant.isEmpty()) {
            document = existant.getFirst();
        } else {
            ContratLocation contrat = contratLocationRepository.findById(idContrat)
                    .orElseThrow(() -> new ResourceNotFoundException("contrat de location", idContrat));

            byte[] pdf = genererPdf(contrat);
            String cle = documentStorageService.store(pdf, "contrats-location");

            document = Document.builder()
                    .typeDocument(TypeDocument.CONTRAT_LOCATION)
                    .entiteType(TypeEntiteDocument.CONTRAT_LOCATION)
                    .entiteId(idContrat)
                    .cheminFichier(cle)
                    .createdAt(LocalDateTime.now())
                    .userCreate(currentUserId)
                    .build();
            documentRepository.save(document);
        }

        String url = documentStorageService.genererUrlPresignee(document.getCheminFichier());
        return buildSuccessResponse(HttpStatus.OK, "Contrat de location disponible", "CONTRAT_LOCATION_GENERATED", url);
    }

    // ── Génération PDF (OpenPDF) ─────────────────────────────────────────────

    private byte[] genererPdf(ContratLocation contrat) {
        var maison = contrat.getMaison();
        var cour = maison.getCour();
        var bailleur = cour.getProprietaire();
        var locataire = contrat.getLocataire();

        String numero = genererNumero(contrat);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        org.openpdf.text.Document document = new org.openpdf.text.Document(PageSize.A4, 50, 50, 60, 60);

        try {
            PdfWriter writer = PdfWriter.getInstance(document, out);
            Image logoFiligrane = utils.chargerLogo(LOGO_PATH);
            writer.setPageEvent(new Utils.FiligraneLogo(logoFiligrane, 380, 380));

            document.open();

            utils.ajouterBandeauNational(document, writer);
            utils.ajouterEntete(document);
            utils.ajouterTitre(document, "CONTRAT DE LOCATION N° " + numero, "Agence Générale Immobilière GI.BF");

            ajouterParties(document, bailleur, locataire);
            ajouterObjetLocation(document, maison, cour);
            ajouterConditionsFinancieres(document, contrat, maison);
            ajouterClause(document);
            ajouterSignaturesBilaterales(document);

            String donnees = numero + "| contrat : " + contrat.getTypeContrat() + "|loyer : " + contrat.getMontantLoyer()
                    + "|date entree : " + contrat.getDateEntree();
            String payloadQr = donnees + "|" + utils.signer(donnees);
            utils.ajouterPied(document, "N° Contrat : "+ numero, payloadQr,"","CONTRAT_LOCATION");

        } catch (Exception e) {
            log.error("Erreur génération PDF contrat de location", e);
            throw new RuntimeException("Échec génération contrat de location PDF", e);
        } finally {
            if (document.isOpen()) document.close();
        }

        return out.toByteArray();
    }

    private void ajouterParties(org.openpdf.text.Document document, Object bailleur, Object locataire) throws DocumentException {
        ajouterTitreSection(document, "ENTRE LES SOUSSIGNÉS");

        PdfPTable tableau = utils.creerTableauInfos();
        Font fontLabel  = FontFactory.getFont(FontFactory.HELVETICA, 9, Font.BOLD, COULEUR_ENTETE);
        Font fontValeur = FontFactory.getFont(FontFactory.HELVETICA, 9, Font.NORMAL, Color.DARK_GRAY);

        var b = (User) bailleur;
        var l = (User) locataire;

        utils.ajouterLigneTableau(tableau, "Bailleur", b.getNom() + " " + b.getPrenom(), fontLabel, fontValeur);
        utils.ajouterLigneTableau(tableau, "Locataire", l.getNom() + " " + l.getPrenom(), fontLabel, fontValeur);

        document.add(tableau);
    }

    private void ajouterObjetLocation(org.openpdf.text.Document document, Object maisonObj, Object courObj) throws DocumentException {
        ajouterTitreSection(document, "OBJET DE LA LOCATION");

        var maison = (com.immobilier.gestionImmobiliere.donnees.biens.model.Maison) maisonObj;
        var cour = (com.immobilier.gestionImmobiliere.donnees.biens.model.Cour) courObj;

        PdfPTable tableau = utils.creerTableauInfos();
        Font fontLabel  = FontFactory.getFont(FontFactory.HELVETICA, 9, Font.BOLD, COULEUR_ENTETE);
        Font fontValeur = FontFactory.getFont(FontFactory.HELVETICA, 9, Font.NORMAL, Color.DARK_GRAY);

        utils.ajouterLigneTableau(tableau, "Bien loué", maison.getNomCommunMaison() + " (" + maison.getTypeMaison() + ")", fontLabel, fontValeur);
        utils.ajouterLigneTableau(tableau, "Référence cour", cour.getReferenceCour(), fontLabel, fontValeur);
        utils.ajouterLigneTableau(tableau, "Nombre de pièces", String.valueOf(maison.getNombrePiece()), fontLabel, fontValeur);

        document.add(tableau);
    }

    private void ajouterConditionsFinancieres(org.openpdf.text.Document document, ContratLocation contrat, Object maisonObj) throws DocumentException {
        ajouterTitreSection(document, "CONDITIONS FINANCIÈRES ET DURÉE");

        var maison = (com.immobilier.gestionImmobiliere.donnees.biens.model.Maison) maisonObj;

        PdfPTable tableau = utils.creerTableauInfos();
        Font fontLabel  = FontFactory.getFont(FontFactory.HELVETICA, 9, Font.BOLD, COULEUR_ENTETE);
        Font fontValeur = FontFactory.getFont(FontFactory.HELVETICA, 9, Font.NORMAL, Color.DARK_GRAY);

        utils.ajouterLigneTableau(tableau, "Date d'entrée", contrat.getDateEntree().format(FMT_DATE), fontLabel, fontValeur);
        utils.ajouterLigneTableau(tableau, "Date de sortie",
                contrat.getDateSortie() != null ? contrat.getDateSortie().format(FMT_DATE) : "Durée indéterminée", fontLabel, fontValeur);
        utils.ajouterLigneTableau(tableau, "Loyer mensuel", FMT_MONTANT.format(contrat.getMontantLoyer()) + " FCFA", fontLabel, fontValeur);
        utils.ajouterLigneTableau(tableau, "Caution exigée",
                maison.getCaution() != null ? FMT_MONTANT.format(maison.getCaution()) + " FCFA" : "—", fontLabel, fontValeur);
        utils.ajouterLigneTableau(tableau, "Avance exigée",
                maison.getAvance() != null ? FMT_MONTANT.format(maison.getAvance()) + " FCFA" : "—", fontLabel, fontValeur);

        document.add(tableau);
    }

    private void ajouterClause(org.openpdf.text.Document document) throws DocumentException {
        Font fontClause = FontFactory.getFont(FontFactory.HELVETICA, 9, Font.NORMAL, Color.DARK_GRAY);
        Paragraph clause = new Paragraph(
                "Le locataire s'engage à payer son loyer mensuellement, au plus tard le 5 du mois suivant " +
                        "le mois occupé, sous peine de pénalité de retard. Toute dégradation constatée à la sortie " +
                        "sera déduite de l'avance puis de la caution, selon les modalités en vigueur.", fontClause);
        clause.setSpacingBefore(15f);
        clause.setAlignment(Element.ALIGN_JUSTIFIED);
        document.add(clause);
    }

    private void ajouterSignaturesBilaterales(org.openpdf.text.Document document) throws DocumentException {
        Font fontSig = FontFactory.getFont(FontFactory.HELVETICA, 9, Font.BOLD, COULEUR_ENTETE);

        PdfPTable tableSignatures = new PdfPTable(2);
        tableSignatures.setWidthPercentage(100);
        tableSignatures.setSpacingBefore(35f);

        PdfPCell cellBailleur = new PdfPCell();
        cellBailleur.setBorder(Rectangle.NO_BORDER);
        cellBailleur.addElement(utils.creerParagrapheAligne("Le Bailleur", fontSig, Element.ALIGN_CENTER));
        tableSignatures.addCell(cellBailleur);

        PdfPCell cellLocataire = new PdfPCell();
        cellLocataire.setBorder(Rectangle.NO_BORDER);
        cellLocataire.addElement(utils.creerParagrapheAligne("Le Locataire", fontSig, Element.ALIGN_CENTER));
        tableSignatures.addCell(cellLocataire);

        document.add(tableSignatures);
    }

    private void ajouterTitreSection(org.openpdf.text.Document document, String titre) throws DocumentException {
        Font fontSection = FontFactory.getFont(FontFactory.HELVETICA, 11, Font.BOLD, COULEUR_ACCENT);
        Paragraph p = new Paragraph(titre, fontSection);
        p.setSpacingBefore(14f);
        p.setSpacingAfter(4f);
        document.add(p);
    }

    private String genererNumero(ContratLocation contrat) {
        return String.format("GI.BF-CL-%s-%s", contrat.getDateEntree().getYear(),
                UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    }
}