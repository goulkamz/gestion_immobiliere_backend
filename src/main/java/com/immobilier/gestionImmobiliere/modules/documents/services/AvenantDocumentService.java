package com.immobilier.gestionImmobiliere.modules.documents.services;

import com.immobilier.gestionImmobiliere.donnees.contrats.model.Avenant;
import com.immobilier.gestionImmobiliere.donnees.contrats.model.AvenantLigne;
import com.immobilier.gestionImmobiliere.donnees.contrats.repository.AvenantRepository;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Avenant a un contrat de location ou a un mandat : genere une seule fois a partir de l'avenant enregistre,
 * stocke puis renvoye tel quel (le contrat d'origine n'est jamais modifie).
 */
@Service
public class AvenantDocumentService {

    private static final Logger log = LoggerFactory.getLogger(AvenantDocumentService.class);
    private static final Color COULEUR_ENTETE = new Color(31, 56, 100);
    private static final Color COULEUR_ACCENT = new Color(206, 17, 38);
    private static final String LOGO_PATH = "/image/axios-logo.png";
    private static final DecimalFormat FMT_MONTANT = new DecimalFormat("#,##0.##");
    private static final DateTimeFormatter FMT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final AvenantRepository avenantRepository;
    private final DocumentRepository documentRepository;
    private final DocumentRegistrar documentRegistrar;
    private final DocumentStorageService documentStorageService;
    private final Utils utils;

    public AvenantDocumentService(AvenantRepository avenantRepository, DocumentRepository documentRepository,
                                  DocumentRegistrar documentRegistrar, DocumentStorageService documentStorageService,
                                  Utils utils) {
        this.avenantRepository = avenantRepository;
        this.documentRepository = documentRepository;
        this.documentRegistrar = documentRegistrar;
        this.documentStorageService = documentStorageService;
        this.utils = utils;
    }

    @PreAuthorize("hasAnyRole('ADMIN','AGENT') or @avenantSecurity.isAccessible(#idAvenant, authentication.principal.idUser)")
    @Transactional
    public byte[] genererOuRecuperer(Integer idAvenant, Integer currentUserId) {
        var existant = documentRepository.findByEntiteTypeAndEntiteIdOrderByCreatedAtDesc(TypeEntiteDocument.AVENANT, idAvenant)
                .stream().filter(d -> d.getTypeDocument() == TypeDocument.AVENANT).findFirst();
        if (existant.isPresent()) {
            return documentStorageService.telecharger(existant.get().getCheminFichier());
        }

        Avenant avenant = avenantRepository.findById(idAvenant)
                .orElseThrow(() -> new ResourceNotFoundException("avenant", idAvenant));

        byte[] pdf = genererPdf(avenant);
        String cle = documentStorageService.store(pdf, "avenants");

        Document document = Document.builder()
                .typeDocument(TypeDocument.AVENANT)
                .entiteType(TypeEntiteDocument.AVENANT)
                .entiteId(idAvenant)
                .cheminFichier(cle)
                .createdAt(LocalDateTime.now())
                .userCreate(currentUserId)
                .build();
        return documentRegistrar.enregistrer(document, pdf);
    }

    // ── Génération PDF (OpenPDF) ─────────────────────────────────────────────

    private byte[] genererPdf(Avenant avenant) {
        boolean location = avenant.getContratLocation() != null;
        String numero = String.format("GI.BF-AVT-%d-%05d", avenant.getDateEffet().getYear(), avenant.getIdAvenant());
        String titre = "AVENANT N° " + avenant.getNumeroOrdre() + (location ? " AU CONTRAT DE LOCATION" : " AU MANDAT DE GESTION");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        org.openpdf.text.Document document = new org.openpdf.text.Document(PageSize.A4, 50, 50, 60, 60);

        try {
            PdfWriter writer = PdfWriter.getInstance(document, out);
            Image logoFiligrane = utils.chargerLogo(LOGO_PATH);
            writer.setPageEvent(new Utils.FiligraneLogo(logoFiligrane, 380, 380));

            document.open();

            utils.ajouterBandeauNational(document, writer);
            utils.ajouterEntete(document);
            utils.ajouterTitre(document, titre, "Agence Générale Immobilière GI.BF");

            if (location) {
                ajouterContratLocation(document, avenant);
            } else {
                ajouterMandat(document, avenant);
            }
            ajouterModifications(document, avenant);
            ajouterEffetEtMotif(document, avenant);
            ajouterClause(document);
            ajouterSignatures(document, location ? "Le Bailleur" : "Le Mandant", location ? "Le Locataire" : "Le Mandataire");

            String donnees = numero + "|" + (location ? "LOCATION|" + avenant.getContratLocation().getIdContratLocation()
                    : "MANDAT|" + avenant.getMandat().getIdMandat()) + "|" + avenant.getDateEffet();
            String payloadQr = donnees + "|" + utils.signer(donnees);
            utils.ajouterPied(document, "N° Avenant : " + numero, payloadQr, "", "AVENANT");

        } catch (Exception e) {
            log.error("Erreur génération PDF avenant", e);
            throw new RuntimeException("Échec génération avenant PDF", e);
        } finally {
            if (document.isOpen()) document.close();
        }

        return out.toByteArray();
    }

    private void ajouterContratLocation(org.openpdf.text.Document document, Avenant avenant) throws DocumentException {
        var contrat = avenant.getContratLocation();
        var maison = contrat.getMaison();
        User bailleur = maison.getCour().getProprietaire();
        User locataire = contrat.getLocataire();

        ajouterTitreSection(document, "CONTRAT CONCERNÉ");
        PdfPTable tableau = utils.creerTableauInfos();
        Font fontLabel = FontFactory.getFont(FontFactory.HELVETICA, 9, Font.BOLD, COULEUR_ENTETE);
        Font fontValeur = FontFactory.getFont(FontFactory.HELVETICA, 9, Font.NORMAL, Color.DARK_GRAY);

        utils.ajouterLigneTableau(tableau, "Contrat de location", "Réf. n° " + contrat.getIdContratLocation()
                + ", entrée le " + contrat.getDateEntree().format(FMT_DATE), fontLabel, fontValeur);
        utils.ajouterLigneTableau(tableau, "Bailleur", bailleur.getNom() + " " + bailleur.getPrenom(), fontLabel, fontValeur);
        utils.ajouterLigneTableau(tableau, "Locataire", locataire.getNom() + " " + locataire.getPrenom(), fontLabel, fontValeur);
        utils.ajouterLigneTableau(tableau, "Bien loué", maison.getNomCommunMaison()
                + " (cour " + maison.getCour().getReferenceCour() + ")", fontLabel, fontValeur);
        document.add(tableau);
    }

    private void ajouterMandat(org.openpdf.text.Document document, Avenant avenant) throws DocumentException {
        var mandat = avenant.getMandat();
        User bailleur = mandat.getCour().getProprietaire();
        User agent = mandat.getAgent();

        ajouterTitreSection(document, "MANDAT CONCERNÉ");
        PdfPTable tableau = utils.creerTableauInfos();
        Font fontLabel = FontFactory.getFont(FontFactory.HELVETICA, 9, Font.BOLD, COULEUR_ENTETE);
        Font fontValeur = FontFactory.getFont(FontFactory.HELVETICA, 9, Font.NORMAL, Color.DARK_GRAY);

        utils.ajouterLigneTableau(tableau, "Mandat de gestion", "Réf. n° " + mandat.getIdMandat()
                + ", début le " + mandat.getDateDebut().format(FMT_DATE), fontLabel, fontValeur);
        utils.ajouterLigneTableau(tableau, "Mandant (Bailleur)", bailleur.getNom() + " " + bailleur.getPrenom(), fontLabel, fontValeur);
        utils.ajouterLigneTableau(tableau, "Mandataire (Agent)", agent.getNom() + " " + agent.getPrenom(), fontLabel, fontValeur);
        utils.ajouterLigneTableau(tableau, "Cour concernée", mandat.getCour().getReferenceCour(), fontLabel, fontValeur);
        document.add(tableau);
    }

    private void ajouterModifications(org.openpdf.text.Document document, Avenant avenant) throws DocumentException {
        ajouterTitreSection(document, "MODIFICATIONS CONVENUES");

        Font fontEntete = FontFactory.getFont(FontFactory.HELVETICA, 9, Font.BOLD, Color.WHITE);
        Font fontLabel = FontFactory.getFont(FontFactory.HELVETICA, 9, Font.BOLD, COULEUR_ENTETE);
        Font fontValeur = FontFactory.getFont(FontFactory.HELVETICA, 9, Font.NORMAL, Color.DARK_GRAY);

        PdfPTable tableau = new PdfPTable(3);
        tableau.setWidthPercentage(100);
        tableau.setWidths(new float[]{34f, 33f, 33f});

        for (String entete : new String[]{"Élément", "Avant", "Après"}) {
            PdfPCell cell = new PdfPCell(new Phrase(entete, fontEntete));
            cell.setBackgroundColor(COULEUR_ENTETE);
            cell.setPadding(6f);
            tableau.addCell(cell);
        }
        for (AvenantLigne ligne : avenant.getLignes()) {
            tableau.addCell(cellule(libelleChamp(ligne.getChamp()), fontLabel));
            tableau.addCell(cellule(formaterValeur(ligne.getChamp(), ligne.getAncienneValeur()), fontValeur));
            tableau.addCell(cellule(formaterValeur(ligne.getChamp(), ligne.getNouvelleValeur()), fontValeur));
        }
        document.add(tableau);
    }

    private void ajouterEffetEtMotif(org.openpdf.text.Document document, Avenant avenant) throws DocumentException {
        PdfPTable tableau = utils.creerTableauInfos();
        Font fontLabel = FontFactory.getFont(FontFactory.HELVETICA, 9, Font.BOLD, COULEUR_ENTETE);
        Font fontValeur = FontFactory.getFont(FontFactory.HELVETICA, 9, Font.NORMAL, Color.DARK_GRAY);

        Paragraph espace = new Paragraph(" ");
        espace.setSpacingBefore(6f);
        document.add(espace);
        utils.ajouterLigneTableau(tableau, "Prend effet le", avenant.getDateEffet().format(FMT_DATE), fontLabel, fontValeur);
        utils.ajouterLigneTableau(tableau, "Motif", avenant.getMotif(), fontLabel, fontValeur);
        document.add(tableau);
    }

    private void ajouterClause(org.openpdf.text.Document document) throws DocumentException {
        Font fontClause = FontFactory.getFont(FontFactory.HELVETICA, 9, Font.NORMAL, Color.DARK_GRAY);
        Paragraph clause = new Paragraph(
                "Les parties conviennent de modifier le contrat désigné ci-dessus comme indiqué. Toutes les autres " +
                        "clauses et conditions du contrat initial demeurent inchangées. Le présent avenant fait partie " +
                        "intégrante du contrat et prend effet à la date indiquée.", fontClause);
        clause.setSpacingBefore(15f);
        clause.setAlignment(Element.ALIGN_JUSTIFIED);
        document.add(clause);
    }

    private void ajouterSignatures(org.openpdf.text.Document document, String gauche, String droite) throws DocumentException {
        Font fontSig = FontFactory.getFont(FontFactory.HELVETICA, 9, Font.BOLD, COULEUR_ENTETE);

        PdfPTable tableSignatures = new PdfPTable(2);
        tableSignatures.setWidthPercentage(100);
        tableSignatures.setSpacingBefore(35f);

        for (String libelle : new String[]{gauche, droite}) {
            PdfPCell cell = new PdfPCell();
            cell.setBorder(Rectangle.NO_BORDER);
            cell.addElement(utils.creerParagrapheAligne(libelle, fontSig, Element.ALIGN_CENTER));
            tableSignatures.addCell(cell);
        }
        document.add(tableSignatures);
    }

    private void ajouterTitreSection(org.openpdf.text.Document document, String titre) throws DocumentException {
        Font fontSection = FontFactory.getFont(FontFactory.HELVETICA, 11, Font.BOLD, COULEUR_ACCENT);
        Paragraph p = new Paragraph(titre, fontSection);
        p.setSpacingBefore(14f);
        p.setSpacingAfter(4f);
        document.add(p);
    }

    private PdfPCell cellule(String texte, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(texte, font));
        cell.setPadding(6f);
        return cell;
    }

    private String libelleChamp(String champ) {
        return switch (champ) {
            case "montantLoyer" -> "Loyer mensuel";
            case "dateSortie" -> "Date de sortie";
            case "dateFin" -> "Date de fin";
            case "commission" -> "Commission";
            case "modeFacturation" -> "Mode de facturation";
            case "typeMandat" -> "Type de mandat";
            default -> champ;
        };
    }

    private String formaterValeur(String champ, String valeur) {
        if (valeur == null || valeur.isBlank()) {
            return champ.startsWith("date") ? "Durée indéterminée" : "—";
        }
        try {
            return switch (champ) {
                case "montantLoyer" -> FMT_MONTANT.format(new BigDecimal(valeur)) + " FCFA";
                case "commission" -> FMT_MONTANT.format(new BigDecimal(valeur)) + " %";
                case "dateSortie", "dateFin" -> LocalDateTime.parse(valeur).format(FMT_DATE);
                default -> valeur;
            };
        } catch (RuntimeException e) {
            return valeur;
        }
    }
}
