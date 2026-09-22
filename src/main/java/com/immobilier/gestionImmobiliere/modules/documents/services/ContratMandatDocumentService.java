package com.immobilier.gestionImmobiliere.modules.documents.services;

import com.immobilier.gestionImmobiliere.donnees.contrats.model.ContratMandat;
import com.immobilier.gestionImmobiliere.donnees.contrats.repository.ContratMandatRepository;
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
public class ContratMandatDocumentService {

    private static final Logger log = LoggerFactory.getLogger(ContratMandatDocumentService.class);

    private static final Color COULEUR_ENTETE = new Color(31, 56, 100);
    private static final Color COULEUR_ACCENT = new Color(206, 17, 38);
    private static final String LOGO_PATH     = "/image/axios-logo.png";
    private static final DecimalFormat FMT_MONTANT = new DecimalFormat("#,##0");
    private static final DateTimeFormatter FMT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ContratMandatRepository contratMandatRepository;
    private final DocumentRepository documentRepository;
    private final DocumentStorageService documentStorageService;
    private final Utils utils;

    public ContratMandatDocumentService(ContratMandatRepository contratMandatRepository,
                                        DocumentRepository documentRepository,
                                        DocumentStorageService documentStorageService,
                                        Utils utils) {
        this.contratMandatRepository = contratMandatRepository;
        this.documentRepository = documentRepository;
        this.documentStorageService = documentStorageService;
        this.utils = utils;
    }

    @PreAuthorize(
            "hasAnyRole('ADMIN','AGENT') " +
                    "or @contratMandatSecurity.isAccessible(#idMandat, authentication.principal.idUser)"
    )
    @Transactional
    public ResponseEntity<?> genererOuRecuperer(Integer idMandat, Integer currentUserId) {
        var existant = documentRepository.findByEntiteTypeAndEntiteIdOrderByCreatedAtDesc(
                TypeEntiteDocument.CONTRAT_MANDAT, idMandat);

        Document document;
        if (!existant.isEmpty()) {
            document = existant.getFirst();
        } else {
            ContratMandat mandat = contratMandatRepository.findById(idMandat)
                    .orElseThrow(() -> new ResourceNotFoundException("contrat de mandat", idMandat));

            byte[] pdf = genererPdf(mandat);
            String cle = documentStorageService.store(pdf, "contrats-mandat");

            document = Document.builder()
                    .typeDocument(TypeDocument.CONTRAT_MANDAT)
                    .entiteType(TypeEntiteDocument.CONTRAT_MANDAT)
                    .entiteId(idMandat)
                    .cheminFichier(cle)
                    .createdAt(LocalDateTime.now())
                    .userCreate(currentUserId)
                    .build();
            documentRepository.save(document);
        }

        String url = documentStorageService.genererUrlPresignee(document.getCheminFichier());
        return buildSuccessResponse(HttpStatus.OK, "Contrat de mandat disponible", "CONTRAT_MANDAT_GENERATED", url);
    }

    // ── Génération PDF (OpenPDF) ─────────────────────────────────────────────

    private byte[] genererPdf(ContratMandat mandat) {
        var cour = mandat.getCour();
        var bailleur = cour.getProprietaire();
        var agent = mandat.getAgent();

        String numero = genererNumero(mandat);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        org.openpdf.text.Document document = new org.openpdf.text.Document(PageSize.A4, 50, 50, 60, 60);

        try {
            PdfWriter writer = PdfWriter.getInstance(document, out);
            Image logoFiligrane = utils.chargerLogo(LOGO_PATH);
            writer.setPageEvent(new Utils.FiligraneLogo(logoFiligrane, 380, 380));

            document.open();

            utils.ajouterBandeauNational(document, writer);
            utils.ajouterEntete(document);
            utils.ajouterTitre(document, "MANDAT DE GESTION N° " + numero, "Agence Générale Immobilière GI.BF");

            ajouterParties(document, bailleur, agent);
            ajouterObjetMandat(document, cour, mandat);
            ajouterConditionsFinancieres(document, mandat);
            ajouterClause(document);
            ajouterSignaturesBilaterales(document);

            String donnees = numero + "|" + mandat.getIdMandat() + "|" + mandat.getCommission()
                    + "|" + mandat.getDateDebut();
            String payloadQr = donnees + "|" + utils.signer(donnees);
            utils.ajouterPied(document, "N° Mandat : "+numero, payloadQr,"","CONTRAT_MANDAT");

        } catch (Exception e) {
            log.error("Erreur génération PDF contrat de mandat", e);
            throw new RuntimeException("Échec génération contrat de mandat PDF", e);
        } finally {
            if (document.isOpen()) document.close();
        }

        return out.toByteArray();
    }

    private void ajouterParties(org.openpdf.text.Document document, Object bailleurObj, Object agentObj) throws DocumentException {
        ajouterTitreSection(document, "ENTRE LES SOUSSIGNÉS");

        var bailleur = (User) bailleurObj;
        var agent = (User) agentObj;

        PdfPTable tableau = utils.creerTableauInfos();
        Font fontLabel  = FontFactory.getFont(FontFactory.HELVETICA, 9, Font.BOLD, COULEUR_ENTETE);
        Font fontValeur = FontFactory.getFont(FontFactory.HELVETICA, 9, Font.NORMAL, Color.DARK_GRAY);

        utils.ajouterLigneTableau(tableau, "Mandant (Bailleur)", bailleur.getNom() + " " + bailleur.getPrenom(), fontLabel, fontValeur);
        utils.ajouterLigneTableau(tableau, "Mandataire (Agent)", agent.getNom() + " " + agent.getPrenom(), fontLabel, fontValeur);

        document.add(tableau);
    }

    private void ajouterObjetMandat(org.openpdf.text.Document document, Object courObj, ContratMandat mandat) throws DocumentException {
        ajouterTitreSection(document, "OBJET DU MANDAT");

        var cour = (com.immobilier.gestionImmobiliere.donnees.biens.model.Cour) courObj;

        PdfPTable tableau = utils.creerTableauInfos();
        Font fontLabel  = FontFactory.getFont(FontFactory.HELVETICA, 9, Font.BOLD, COULEUR_ENTETE);
        Font fontValeur = FontFactory.getFont(FontFactory.HELVETICA, 9, Font.NORMAL, Color.DARK_GRAY);

        utils.ajouterLigneTableau(tableau, "Cour concernée", cour.getReferenceCour(), fontLabel, fontValeur);
        utils.ajouterLigneTableau(tableau, "Lot", cour.getLotCour() != null ? cour.getLotCour() : "—", fontLabel, fontValeur);
        utils.ajouterLigneTableau(tableau, "Type de mandat", mandat.getTypeMandat() != null ? mandat.getTypeMandat().name() : "—", fontLabel, fontValeur);

        document.add(tableau);
    }

    private void ajouterConditionsFinancieres(org.openpdf.text.Document document, ContratMandat mandat) throws DocumentException {
        ajouterTitreSection(document, "CONDITIONS FINANCIÈRES ET DURÉE");

        PdfPTable tableau = utils.creerTableauInfos();
        Font fontLabel  = FontFactory.getFont(FontFactory.HELVETICA, 9, Font.BOLD, COULEUR_ENTETE);
        Font fontValeur = FontFactory.getFont(FontFactory.HELVETICA, 9, Font.NORMAL, Color.DARK_GRAY);

        utils.ajouterLigneTableau(tableau, "Date de début", mandat.getDateDebut().format(FMT_DATE), fontLabel, fontValeur);
        utils.ajouterLigneTableau(tableau, "Date de fin",
                mandat.getDateFin() != null ? mandat.getDateFin().format(FMT_DATE) : "Durée indéterminée", fontLabel, fontValeur);
        utils.ajouterLigneTableau(tableau, "Commission", FMT_MONTANT.format(mandat.getCommission()) + " %", fontLabel, fontValeur);
        utils.ajouterLigneTableau(tableau, "Mode de facturation", mandat.getModeFacturation() != null ? mandat.getModeFacturation() : "—", fontLabel, fontValeur);

        document.add(tableau);
    }

    private void ajouterClause(org.openpdf.text.Document document) throws DocumentException {
        Font fontClause = FontFactory.getFont(FontFactory.HELVETICA, 9, Font.NORMAL, Color.DARK_GRAY);
        Paragraph clause = new Paragraph(
                "L'agent mandataire est chargé de la gestion locative de la cour désignée ci-dessus : " +
                        "recherche de locataires, encaissement des loyers, entretien courant. La commission indiquée " +
                        "est déduite mensuellement des loyers encaissés avant reversement au mandant.", fontClause);
        clause.setSpacingBefore(15f);
        clause.setAlignment(Element.ALIGN_JUSTIFIED);
        document.add(clause);
    }

    private void ajouterSignaturesBilaterales(org.openpdf.text.Document document) throws DocumentException {
        Font fontSig = FontFactory.getFont(FontFactory.HELVETICA, 9, Font.BOLD, COULEUR_ENTETE);

        PdfPTable tableSignatures = new PdfPTable(2);
        tableSignatures.setWidthPercentage(100);
        tableSignatures.setSpacingBefore(35f);

        PdfPCell cellMandant = new PdfPCell();
        cellMandant.setBorder(Rectangle.NO_BORDER);
        cellMandant.addElement(utils.creerParagrapheAligne("Le Mandant", fontSig, Element.ALIGN_CENTER));
        tableSignatures.addCell(cellMandant);

        PdfPCell cellMandataire = new PdfPCell();
        cellMandataire.setBorder(Rectangle.NO_BORDER);
        cellMandataire.addElement(utils.creerParagrapheAligne("Le Mandataire", fontSig, Element.ALIGN_CENTER));
        tableSignatures.addCell(cellMandataire);

        document.add(tableSignatures);
    }

    private void ajouterTitreSection(org.openpdf.text.Document document, String titre) throws DocumentException {
        Font fontSection = FontFactory.getFont(FontFactory.HELVETICA, 11, Font.BOLD, COULEUR_ACCENT);
        Paragraph p = new Paragraph(titre, fontSection);
        p.setSpacingBefore(14f);
        p.setSpacingAfter(4f);
        document.add(p);
    }

    private String genererNumero(ContratMandat mandat) {
        return String.format("GI.BF-CM-%s-%s", mandat.getDateDebut().getYear(),
                UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    }
}