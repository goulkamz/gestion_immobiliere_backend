package com.immobilier.gestionImmobiliere.modules.documents.services;

import com.immobilier.gestionImmobiliere.donnees.biens.model.Maison;
import com.immobilier.gestionImmobiliere.donnees.contrats.model.DecompteSortie;
import com.immobilier.gestionImmobiliere.donnees.contrats.model.StatutDecompteSortie;
import com.immobilier.gestionImmobiliere.donnees.contrats.repository.DecompteSortieRepository;
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
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static com.immobilier.gestionImmobiliere.utils.BuildSuccessResponse.buildSuccessResponse;

@Service
public class DecompteSortieDocumentService {

    private static final Logger log = LoggerFactory.getLogger(DecompteSortieDocumentService.class);

    private static final Color COULEUR_ENTETE      = new Color(31, 56, 100);
    private static final Color COULEUR_ACCENT      = new Color(206, 17, 38);
    private static final Color COULEUR_FOND_REMBOURSE = new Color(230, 245, 230);
    private static final Color COULEUR_FOND_DU     = new Color(250, 230, 230);
    private static final Color COULEUR_VERT        = new Color(0, 122, 61);
    private static final String LOGO_PATH          = "/image/axios-logo.png";
    private static final DecimalFormat FMT_MONTANT = new DecimalFormat("#,##0");
    private static final DateTimeFormatter FMT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final DecompteSortieRepository decompteSortieRepository;
    private final DocumentRepository documentRepository;
    private final DocumentStorageService documentStorageService;
    private final Utils utils;

    public DecompteSortieDocumentService(DecompteSortieRepository decompteSortieRepository,
                                         DocumentRepository documentRepository,
                                         DocumentStorageService documentStorageService,
                                         Utils utils) {
        this.decompteSortieRepository = decompteSortieRepository;
        this.documentRepository = documentRepository;
        this.documentStorageService = documentStorageService;
        this.utils = utils;
    }

    /**
     * Le décompte n'est figé en document qu'une fois RÉGLÉ — tant qu'il est
     * EN_ATTENTE, les montants pourraient encore évoluer (l'agent pourrait
     * en théorie annuler et recalculer avant confirmation).
     */
    @PreAuthorize(
            "hasAnyRole('ADMIN','AGENT') " +
                    "or @decompteSortieSecurity.isAccessible(#idDecompte, authentication.principal.idUser)"
    )
    @Transactional
    public ResponseEntity<?> genererOuRecuperer(Integer idDecompte, Integer currentUserId) {
        DecompteSortie decompte = decompteSortieRepository.findById(idDecompte)
                .orElseThrow(() -> new ResourceNotFoundException("décompte de sortie", idDecompte));

        if (decompte.getStatut() != StatutDecompteSortie.REGLE) {
            throw new IllegalStateException("Le document n'est disponible qu'une fois le décompte réglé");
        }

        var existant = documentRepository.findByEntiteTypeAndEntiteIdOrderByCreatedAtDesc(
                TypeEntiteDocument.DECOMPTE_SORTIE, idDecompte);

        Document document;
        if (!existant.isEmpty()) {
            document = existant.getFirst();
        } else {
            byte[] pdf = genererPdf(decompte);
            String cle = documentStorageService.store(pdf, "decomptes-sortie");

            document = Document.builder()
                    .typeDocument(TypeDocument.DECOMPTE_SORTIE)
                    .entiteType(TypeEntiteDocument.DECOMPTE_SORTIE)
                    .entiteId(idDecompte)
                    .cheminFichier(cle)
                    .createdAt(LocalDateTime.now())
                    .userCreate(currentUserId)
                    .build();
            documentRepository.save(document);
        }

        String url = documentStorageService.genererUrlPresignee(document.getCheminFichier());
        return buildSuccessResponse(HttpStatus.OK, "Décompte de sortie disponible", "DECOMPTE_SORTIE_DOCUMENT_GENERATED", url);
    }

    // ── Génération PDF (OpenPDF) ─────────────────────────────────────────────

    private byte[] genererPdf(DecompteSortie decompte) {
        var contrat = decompte.getContratLocation();
        var maison = contrat.getMaison();
        var locataire = contrat.getLocataire();

        String numero = genererNumero(decompte);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        org.openpdf.text.Document document = new org.openpdf.text.Document(PageSize.A4, 50, 50, 60, 60);

        try {
            PdfWriter writer = PdfWriter.getInstance(document, out);
            Image logoFiligrane = utils.chargerLogo(LOGO_PATH);
            writer.setPageEvent(new Utils.FiligraneLogo(logoFiligrane, 380, 380));

            document.open();

            utils.ajouterBandeauNational(document, writer);
            utils.ajouterEntete(document);
            utils.ajouterTitre(document, "DÉCOMPTE DE SORTIE N° " + numero, "Agence Générale Immobilière GI.BF");

            ajouterInfosLocation(document, locataire, maison, decompte);
            ajouterDetailCalcul(document, decompte);
            ajouterMontantFinalEnCadre(document, decompte);

            String donnees = numero + "|maison : " + decompte.getContratLocation().getMaison().getNomCommunMaison() + "|montant : " + decompte.getMontantARembourser()
                    + "|manquant : " + decompte.getMontantManquant() + "|date sortie : " + decompte.getDateSortie();
            String payloadQr = donnees + "|" + utils.signer(donnees);
            utils.ajouterPied(document, "N° Décompte : "+numero, payloadQr,"","DECOMPTE_SORTIE");

        } catch (Exception e) {
            log.error("Erreur génération PDF décompte de sortie", e);
            throw new RuntimeException("Échec génération décompte de sortie PDF", e);
        } finally {
            if (document.isOpen()) document.close();
        }

        return out.toByteArray();
    }

    private void ajouterInfosLocation(org.openpdf.text.Document document, Object locataireObj, Object maisonObj, DecompteSortie decompte) throws DocumentException {
        var locataire = (User) locataireObj;
        var maison = (Maison) maisonObj;

        PdfPTable tableau = utils.creerTableauInfos();
        Font fontLabel  = FontFactory.getFont(FontFactory.HELVETICA, 9, Font.BOLD, COULEUR_ENTETE);
        Font fontValeur = FontFactory.getFont(FontFactory.HELVETICA, 9, Font.NORMAL, Color.DARK_GRAY);

        utils.ajouterLigneTableau(tableau, "Locataire", locataire.getNom() + " " + locataire.getPrenom(), fontLabel, fontValeur);
        utils.ajouterLigneTableau(tableau, "Bien", maison.getNomCommunMaison(), fontLabel, fontValeur);
        utils.ajouterLigneTableau(tableau, "Date de sortie", decompte.getDateSortie().format(FMT_DATE), fontLabel, fontValeur);

        document.add(tableau);
    }

    private void ajouterDetailCalcul(org.openpdf.text.Document document, DecompteSortie decompte) throws DocumentException {
        ajouterTitreSection(document, "DÉTAIL DU CALCUL");

        PdfPTable tableau = utils.creerTableauInfos();
        Font fontLabel  = FontFactory.getFont(FontFactory.HELVETICA, 9, Font.BOLD, COULEUR_ENTETE);
        Font fontValeur = FontFactory.getFont(FontFactory.HELVETICA, 9, Font.NORMAL, Color.DARK_GRAY);

        ajouterLigneMontant(tableau, "Avance de référence", decompte.getMontantAvanceReference(), fontLabel, fontValeur);
        ajouterLigneMontant(tableau, "Caution de référence", decompte.getMontantCautionReference(), fontLabel, fontValeur);
        ajouterLigneMontant(tableau, "Arriérés de loyer constatés", decompte.getMontantArrieres(), fontLabel, fontValeur);
        ajouterLigneMontant(tableau, "Coût des réparations évalué", decompte.getCoutReparation(), fontLabel, fontValeur);
        ajouterLigneMontant(tableau, "Déduit sur l'avance", decompte.getMontantDeduitAvance(), fontLabel, fontValeur);
        ajouterLigneMontant(tableau, "Déduit sur la caution", decompte.getMontantDeduitCaution(), fontLabel, fontValeur);

        document.add(tableau);
    }

    private void ajouterMontantFinalEnCadre(org.openpdf.text.Document document, DecompteSortie decompte) throws DocumentException {
        boolean remboursement = decompte.getMontantARembourser() != null
                && decompte.getMontantARembourser().compareTo(BigDecimal.ZERO) > 0;
        String libelle = remboursement ? "MONTANT REMBOURSÉ AU LOCATAIRE" : "MONTANT DÛ PAR LE LOCATAIRE";
        BigDecimal montantFinal = remboursement ? decompte.getMontantARembourser() : decompte.getMontantManquant();
        Color couleurTexte = remboursement ? COULEUR_VERT : COULEUR_ACCENT;
        Color couleurFond  = remboursement ? COULEUR_FOND_REMBOURSE : COULEUR_FOND_DU;

        PdfPTable cadre = new PdfPTable(1);
        cadre.setWidthPercentage(100);
        cadre.setSpacingBefore(15f);

        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(couleurFond);
        cell.setBorderColor(couleurTexte);
        cell.setPadding(10f);

        Font fontLabel   = FontFactory.getFont(FontFactory.HELVETICA, 10, Font.BOLD, couleurTexte);
        Font fontMontant = FontFactory.getFont(FontFactory.HELVETICA, 14, Font.BOLD, couleurTexte);

        Paragraph label = new Paragraph(libelle, fontLabel);
        label.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(label);

        Paragraph montant = new Paragraph(FMT_MONTANT.format(montantFinal != null ? montantFinal : BigDecimal.ZERO) + " FCFA", fontMontant);
        montant.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(montant);

        cadre.addCell(cell);
        document.add(cadre);
    }

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

    private String genererNumero(DecompteSortie decompte) {
        return String.format("GI.BF-DS-%s-%s", decompte.getDateSortie().getYear(),
                UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    }
}