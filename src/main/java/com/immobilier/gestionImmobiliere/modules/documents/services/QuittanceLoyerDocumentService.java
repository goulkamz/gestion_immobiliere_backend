package com.immobilier.gestionImmobiliere.modules.documents.services;

import com.immobilier.gestionImmobiliere.donnees.contrats.model.ContratLocation;
import com.immobilier.gestionImmobiliere.donnees.contrats.repository.ContratLocationRepository;
import com.immobilier.gestionImmobiliere.donnees.documents.model.Document;
import com.immobilier.gestionImmobiliere.donnees.documents.model.TypeDocument;
import com.immobilier.gestionImmobiliere.donnees.documents.model.TypeEntiteDocument;
import com.immobilier.gestionImmobiliere.donnees.documents.repository.DocumentRepository;
import com.immobilier.gestionImmobiliere.donnees.paiements.model.EcheanceLoyer;
import com.immobilier.gestionImmobiliere.donnees.paiements.model.StatutEcheance;
import com.immobilier.gestionImmobiliere.donnees.paiements.model.TypeEcheance;
import com.immobilier.gestionImmobiliere.donnees.paiements.repository.EcheanceLoyerRepository;
import com.immobilier.gestionImmobiliere.donnees.user.model.User;
import com.immobilier.gestionImmobiliere.exceptions.ResourceNotFoundException;
import com.immobilier.gestionImmobiliere.modules.documents.UtilsDocuments.Utils;
import com.immobilier.gestionImmobiliere.utils.DateUtils;
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
import java.util.UUID;

import static com.immobilier.gestionImmobiliere.utils.BuildSuccessResponse.buildSuccessResponse;

@Service
public class QuittanceLoyerDocumentService {

    private static final Logger log = LoggerFactory.getLogger(QuittanceLoyerDocumentService.class);

    private static final Color COULEUR_ENTETE = new Color(31, 56, 100);
    private static final Color COULEUR_ACCENT = new Color(206, 17, 38);
    private static final String LOGO_PATH     = "/image/axios-logo.png";
    private static final DecimalFormat FMT_MONTANT = new DecimalFormat("#,##0");

    private final EcheanceLoyerRepository echeanceLoyerRepository;
    private final ContratLocationRepository contratLocationRepository;
    private final DocumentRepository documentRepository;
    private final DocumentStorageService documentStorageService;
    private final Utils utils;

    public QuittanceLoyerDocumentService(EcheanceLoyerRepository echeanceLoyerRepository,
                                         ContratLocationRepository contratLocationRepository,
                                         DocumentRepository documentRepository,
                                         DocumentStorageService documentStorageService,
                                         Utils utils) {
        this.echeanceLoyerRepository = echeanceLoyerRepository;
        this.contratLocationRepository = contratLocationRepository;
        this.documentRepository = documentRepository;
        this.documentStorageService = documentStorageService;
        this.utils = utils;
    }

    /**
     * La quittance n'est disponible qu'une fois le loyer intégralement payé
     * (montant_du + pénalité éventuelle couverts) — document légal attestant
     * que le locataire est à jour pour ce mois précis.
     */
    @PreAuthorize(
            "hasAnyRole('ADMIN','AGENT') " +
                    "or @quittanceLoyerSecurity.isAccessible(#idEcheance, authentication.principal.idUser)"
    )
    @Transactional
    public ResponseEntity<?> genererOuRecuperer(Integer idEcheance, Integer currentUserId) {
        EcheanceLoyer echeance = echeanceLoyerRepository.findById(idEcheance)
                .orElseThrow(() -> new ResourceNotFoundException("échéance", idEcheance));

        if (echeance.getEntiteEcheanceType() != TypeEcheance.LOCATION) {
            throw new IllegalStateException("Cette échéance n'est pas un loyer de location");
        }
        if (echeance.getStatut() != StatutEcheance.PAYE) {
            throw new IllegalStateException("La quittance n'est disponible qu'une fois le loyer intégralement payé");
        }

        var existant = documentRepository.findByEntiteTypeAndEntiteIdOrderByCreatedAtDesc(
                TypeEntiteDocument.ECHEANCE_LOYER, idEcheance);

        Document document;
        if (!existant.isEmpty()) {
            document = existant.getFirst();
        } else {
            ContratLocation contrat = contratLocationRepository.findById(echeance.getEntiteEcheanceId())
                    .orElseThrow(() -> new ResourceNotFoundException("contrat de location", echeance.getEntiteEcheanceId()));

            byte[] pdf = genererPdf(echeance, contrat);
            String cle = documentStorageService.store(pdf, "quittances");

            document = Document.builder()
                    .typeDocument(TypeDocument.QUITTANCE_LOYER)
                    .entiteType(TypeEntiteDocument.ECHEANCE_LOYER)
                    .entiteId(idEcheance)
                    .cheminFichier(cle)
                    .createdAt(LocalDateTime.now())
                    .userCreate(currentUserId)
                    .build();
            documentRepository.save(document);
        }

        String url = documentStorageService.genererUrlPresignee(document.getCheminFichier());
        return buildSuccessResponse(HttpStatus.OK, "Quittance de loyer disponible", "QUITTANCE_GENERATED", url);
    }

    // ── Génération PDF (OpenPDF) ─────────────────────────────────────────────

    private byte[] genererPdf(EcheanceLoyer echeance, ContratLocation contrat) {
        String periode = DateUtils.nomMoisFrancais(echeance.getDateEcheance()) + " " + echeance.getDateEcheance().getYear();
        var maison = contrat.getMaison();
        var locataire = (User) contrat.getLocataire();
        var bailleur = (User) maison.getCour().getProprietaire();
        String numero = genererNumero(echeance);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        org.openpdf.text.Document document = new org.openpdf.text.Document(PageSize.A4, 50, 50, 60, 60);

        try {
            PdfWriter writer = PdfWriter.getInstance(document, out);
            Image logoFiligrane = utils.chargerLogo(LOGO_PATH);
            writer.setPageEvent(new Utils.FiligraneLogo(logoFiligrane, 380, 380));

            document.open();

            utils.ajouterBandeauNational(document, writer);
            utils.ajouterEntete(document);
            utils.ajouterTitre(document, "QUITTANCE DE LOYER N° " + numero, periode);

            ajouterInfosQuittance(document, bailleur, locataire, maison, periode, echeance);
            ajouterMontantEnLettres(document, echeance.getMontantPaye());
            ajouterFormuleQuittance(document, bailleur, locataire);

            String donnees = numero + "|" + echeance.getIdEcheance() + "|" + echeance.getMontantPaye() + "|" + periode;
            String payloadQr = donnees + "|" + utils.signer(donnees);
            utils.ajouterPied(document, "N° Quittance : "+numero, payloadQr,"","QUITTANCE");

        } catch (Exception e) {
            log.error("Erreur génération PDF quittance de loyer", e);
            throw new RuntimeException("Échec génération quittance de loyer PDF", e);
        } finally {
            if (document.isOpen()) document.close();
        }

        return out.toByteArray();
    }

    private void ajouterInfosQuittance(org.openpdf.text.Document document, Object bailleurObj, Object locataireObj,
                                       Object maisonObj, String periode, EcheanceLoyer echeance) throws DocumentException {
        var bailleur = (User) bailleurObj;
        var locataire = (User) locataireObj;
        var maison = (com.immobilier.gestionImmobiliere.donnees.biens.model.Maison) maisonObj;

        PdfPTable tableau = utils.creerTableauInfos();
        Font fontLabel  = FontFactory.getFont(FontFactory.HELVETICA, 9, Font.BOLD, COULEUR_ENTETE);
        Font fontValeur = FontFactory.getFont(FontFactory.HELVETICA, 9, Font.NORMAL, Color.DARK_GRAY);

        utils.ajouterLigneTableau(tableau, "Bailleur", bailleur.getNom() + " " + bailleur.getPrenom(), fontLabel, fontValeur);
        utils.ajouterLigneTableau(tableau, "Locataire", locataire.getNom() + " " + locataire.getPrenom(), fontLabel, fontValeur);
        utils.ajouterLigneTableau(tableau, "Bien loué", maison.getNomCommunMaison(), fontLabel, fontValeur);
        utils.ajouterLigneTableau(tableau, "Période concernée", periode, fontLabel, fontValeur);
        utils.ajouterLigneTableau(tableau, "Montant du loyer", FMT_MONTANT.format(echeance.getMontantDu()) + " FCFA", fontLabel, fontValeur);

        BigDecimal penalite = echeance.getPenalite() != null ? echeance.getPenalite() : BigDecimal.ZERO;
        if (penalite.compareTo(BigDecimal.ZERO) > 0) {
            utils.ajouterLigneTableau(tableau, "Pénalité de retard réglée", FMT_MONTANT.format(penalite) + " FCFA", fontLabel, fontValeur);
        }
        utils.ajouterLigneTableau(tableau, "Montant total réglé", FMT_MONTANT.format(echeance.getMontantPaye()) + " FCFA", fontLabel, fontValeur);

        document.add(tableau);
    }

    /**
     * Montant en toutes lettres — mention légale standard sur une quittance,
     * absente de la version précédente (visible dans le modèle CampusFaso fourni).
     */
    private void ajouterMontantEnLettres(org.openpdf.text.Document document, BigDecimal montant) throws DocumentException {
        Font fontLettres = FontFactory.getFont(FontFactory.HELVETICA, 9, Font.BOLDITALIC, COULEUR_ACCENT);
        long valeur = montant != null ? montant.longValue() : 0L;
        Paragraph p = new Paragraph("En lettres : " + capitaliser(nombreEnLettres(valeur)) + " francs CFA", fontLettres);
        p.setSpacingBefore(6f);
        p.setSpacingAfter(10f);
        document.add(p);
    }

    private void ajouterFormuleQuittance(org.openpdf.text.Document document, Object bailleurObj, Object locataireObj) throws DocumentException {
        var bailleur = (User) bailleurObj;
        var locataire = (User) locataireObj;

        Font fontTexte = FontFactory.getFont(FontFactory.HELVETICA, 10, Font.NORMAL, Color.DARK_GRAY);
        Paragraph formule = new Paragraph(
                "Je soussigné(e) " + bailleur.getNom() + " " + bailleur.getPrenom() +
                        ", bailleur du logement désigné ci-dessus, déclare avoir reçu de " +
                        locataire.getNom() + " " + locataire.getPrenom() +
                        " la somme mentionnée au titre du loyer de la période indiquée, et lui donne quittance " +
                        "de cette somme, sous réserve de tous mes droits.", fontTexte);
        formule.setAlignment(Element.ALIGN_JUSTIFIED);
        formule.setSpacingBefore(10f);
        document.add(formule);
    }

    private String genererNumero(EcheanceLoyer echeance) {
        return String.format("GI.BF-QUI-%s-%s", echeance.getDateEcheance().getYear(),
                UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    }

    private String capitaliser(String texte) {
        if (texte == null || texte.isBlank()) return texte;
        return Character.toUpperCase(texte.charAt(0)) + texte.substring(1);
    }

    // ── Conversion montant → lettres (français) ──────────────────────────────

    private static final String[] UNITES = {"", "un", "deux", "trois", "quatre", "cinq", "six", "sept", "huit", "neuf",
            "dix", "onze", "douze", "treize", "quatorze", "quinze", "seize", "dix-sept", "dix-huit", "dix-neuf"};
    private static final String[] DIZAINES = {"", "", "vingt", "trente", "quarante", "cinquante", "soixante"};

    private String nombreEnLettres(long montant) {
        if (montant == 0) return "zéro";
        StringBuilder sb = new StringBuilder();
        long millions  = montant / 1_000_000;
        long milliers  = (montant / 1000) % 1000;
        long reste     = montant % 1000;

        if (millions > 0) sb.append(convertirTrois(millions)).append(millions > 1 ? " millions " : " million ");
        if (milliers > 0) sb.append(milliers == 1 ? "mille " : convertirTrois(milliers) + " mille ");
        if (reste > 0 || sb.length() == 0) sb.append(convertirTrois(reste));

        return sb.toString().trim();
    }

    private String convertirTrois(long n) {
        StringBuilder sb = new StringBuilder();
        long centaines = n / 100;
        long reste = n % 100;

        if (centaines > 0) {
            sb.append(centaines > 1 ? UNITES[(int) centaines] + " cent" : "cent");
            if (centaines > 1 && reste == 0) sb.append("s");
            sb.append(" ");
        }
        if (reste > 0) sb.append(convertirDeux(reste));
        return sb.toString().trim();
    }

    private String convertirDeux(long n) {
        if (n < 20) return UNITES[(int) n];
        if (n < 70) {
            long dizaine = n / 10, unite = n % 10;
            StringBuilder sb = new StringBuilder(DIZAINES[(int) dizaine]);
            if (unite == 1) sb.append(" et un");
            else if (unite > 0) sb.append("-").append(UNITES[(int) unite]);
            return sb.toString();
        }
        if (n < 80) return "soixante-" + UNITES[(int) (n - 60)]; // soixante-dix à soixante-dix-neuf
        long unite = n % 20;
        StringBuilder sb = new StringBuilder("quatre-vingt");
        if (n == 80) return sb.append("s").toString();
        if (unite == 1 && n < 90) sb.append("-et-un");
        else sb.append("-").append(UNITES[(int) unite]);
        return sb.toString();
    }
}