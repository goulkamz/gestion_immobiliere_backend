package com.immobilier.gestionImmobiliere.modules.documents.services;

import com.immobilier.gestionImmobiliere.donnees.biens.model.LocationBienService;
import com.immobilier.gestionImmobiliere.donnees.biens.repository.LocationBienServiceRepository;
import com.immobilier.gestionImmobiliere.donnees.documents.model.Document;
import com.immobilier.gestionImmobiliere.donnees.documents.model.TypeDocument;
import com.immobilier.gestionImmobiliere.donnees.documents.model.TypeEntiteDocument;
import com.immobilier.gestionImmobiliere.donnees.documents.repository.DocumentRepository;
import com.immobilier.gestionImmobiliere.donnees.paiements.model.EcheanceLoyer;
import com.immobilier.gestionImmobiliere.donnees.paiements.model.Paiement;
import com.immobilier.gestionImmobiliere.donnees.paiements.model.PaiementEcheance;
import com.immobilier.gestionImmobiliere.donnees.paiements.model.PaiementLocationBienService;
import com.immobilier.gestionImmobiliere.donnees.paiements.repository.EcheanceLoyerRepository;
import com.immobilier.gestionImmobiliere.donnees.paiements.repository.PaiementEcheanceRepository;
import com.immobilier.gestionImmobiliere.donnees.paiements.repository.PaiementLocationBienServiceRepository;
import com.immobilier.gestionImmobiliere.donnees.paiements.repository.PaiementRepository;
import com.immobilier.gestionImmobiliere.exceptions.ResourceNotFoundException;
import com.immobilier.gestionImmobiliere.modules.documents.UtilsDocuments.Utils;
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
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import static com.immobilier.gestionImmobiliere.utils.BuildSuccessResponse.buildSuccessResponse;

    @Service
    public class RecuDocumentService {

        private static final Logger log = LoggerFactory.getLogger(RecuDocumentService.class);

        private static final Color COULEUR_ENTETE       = new Color(31, 56, 100);
        private static final Color COULEUR_TEXTE        = new Color(33, 33, 33);
        private static final Color COULEUR_ACCENT       = new Color(55, 86, 35);
        private static final Color COULEUR_FOND_MONTANT = new Color(230, 245, 230);
        private static final String LOGO_PATH           = "/image/axios-logo.png";
        private static final DecimalFormat FMT_MONTANT  = new DecimalFormat("#,##0");
        private static final DateTimeFormatter FMT_DATETIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        private static final DateTimeFormatter FMT_DATE     = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        private final PaiementRepository paiementRepository;
        private final DocumentRepository documentRepository;
        private final DocumentStorageService documentStorageService;
        private final PaiementEcheanceRepository paiementEcheanceRepository;
        private final EcheanceLoyerRepository echeanceLoyerRepository;
        private final PaiementLocationBienServiceRepository paiementLocationBienServiceRepository;
        private final LocationBienServiceRepository locationBienServiceRepository;
        private final Utils utils;

        public RecuDocumentService(PaiementRepository paiementRepository, DocumentRepository documentRepository,
                                   DocumentStorageService documentStorageService, PaiementEcheanceRepository paiementEcheanceRepository,
                                   EcheanceLoyerRepository echeanceLoyerRepository, PaiementLocationBienServiceRepository paiementLocationBienServiceRepository,
                                   LocationBienServiceRepository locationBienServiceRepository, Utils utils) {
            this.paiementRepository = paiementRepository;
            this.documentRepository = documentRepository;
            this.documentStorageService = documentStorageService;
            this.paiementEcheanceRepository = paiementEcheanceRepository;
            this.echeanceLoyerRepository = echeanceLoyerRepository;
            this.paiementLocationBienServiceRepository = paiementLocationBienServiceRepository;
            this.locationBienServiceRepository = locationBienServiceRepository;
            this.utils = utils;
        }

        /**
         * Génère le reçu une seule fois (immuable) — si un document existe déjà
         * pour ce paiement, le renvoie sans régénérer.
         */
        @Transactional
        public ResponseEntity<?> genererOuRecuperer(Integer idPaiement, Integer currentUserId) {
            var existant = documentRepository.findByEntiteTypeAndEntiteIdOrderByCreatedAtDesc(
                    TypeEntiteDocument.PAIEMENT, idPaiement);

            Document document;
            if (!existant.isEmpty()) {
                document = existant.getFirst();
            } else {
                Paiement paiement = paiementRepository.findById(idPaiement)
                        .orElseThrow(() -> new ResourceNotFoundException("paiement", idPaiement));

                byte[] pdf = genererPdf(paiement);
                String cle = documentStorageService.store(pdf, "recus");

                document = Document.builder()
                        .typeDocument(TypeDocument.RECU)
                        .entiteType(TypeEntiteDocument.PAIEMENT)
                        .entiteId(idPaiement)
                        .cheminFichier(cle)
                        .createdAt(LocalDateTime.now())
                        .userCreate(currentUserId)
                        .build();
                documentRepository.save(document);
            }

            String url = documentStorageService.genererUrlPresignee(document.getCheminFichier());
            return buildSuccessResponse(HttpStatus.OK, "Reçu disponible", "RECU_GENERATED", url);
        }

        // ── Génération PDF (OpenPDF) ─────────────────────────────────────────────

        private byte[] genererPdf(Paiement paiement) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            org.openpdf.text.Document document = new org.openpdf.text.Document(PageSize.A4, 30, 30, 45, 40);
            String numero = genererNumero();

            try {
                PdfWriter writer = PdfWriter.getInstance(document, out);
                Image logoFiligrane = utils.chargerLogo(LOGO_PATH);
                writer.setPageEvent(new Utils.FiligraneLogo(logoFiligrane, 280, 280));

                document.open();

                utils.ajouterBandeauNational(document, writer);
                utils.ajouterEntete(document);
                utils.ajouterTitre(document, "REÇU DE PAIEMENT N° " + numero, "Gestion Immobilière GI.BF");

                ajouterCorpsRecu(document, paiement);
                ajouterMontantEnCadre(document, paiement, numero);

                utils.ajouterSignature(document, "Ouagadougou", "L'Agence GI.BF");

            } catch (Exception e) {
                log.error("Erreur génération PDF reçu", e);
                throw new RuntimeException("Échec génération reçu PDF", e);
            } finally {
                if (document.isOpen()) document.close();
            }

            return out.toByteArray();
        }

        private void ajouterCorpsRecu(org.openpdf.text.Document document, Paiement paiement) throws DocumentException {
            Font fontLabel  = FontFactory.getFont(FontFactory.HELVETICA, 8, Font.BOLD, COULEUR_ENTETE);
            Font fontValeur = FontFactory.getFont(FontFactory.HELVETICA, 8, Font.NORMAL, COULEUR_TEXTE);

            PdfPTable tableau = utils.creerTableauInfos();
            tableau.setSpacingBefore(4f);
            tableau.setSpacingAfter(4f);

            utils.ajouterLigneTableau(tableau, "Date", paiement.getDatePaiement().format(FMT_DATETIME), fontLabel, fontValeur);
            utils.ajouterLigneTableau(tableau, "Mode de paiement", paiement.getModePaiement(), fontLabel, fontValeur);
            utils.ajouterLigneTableau(tableau, "Référence", paiement.getReferencePaiement() != null ? paiement.getReferencePaiement() : "—", fontLabel, fontValeur);
            utils.ajouterLigneTableau(tableau, "Sens", paiement.getSens().name(), fontLabel, fontValeur);

            ajouterContexteSpecifique(tableau, paiement, fontLabel, fontValeur);

            document.add(tableau);
        }

        /**
         * Ajoute les lignes propres au contexte du paiement : échéance de loyer/mandat
         * si liée via paiement_echeance, ou location de bien/service si liée via
         * paiement_location_bien_service. Les deux voies sont mutuellement exclusives.
         */
        private void ajouterContexteSpecifique(PdfPTable tableau, Paiement paiement, Font fontLabel, Font fontValeur) {
            List<PaiementEcheance> liensEcheance = paiementEcheanceRepository.findByIdPaiement(paiement.getIdPaiement());
            if (!liensEcheance.isEmpty()) {
                EcheanceLoyer echeance = echeanceLoyerRepository.findById(liensEcheance.get(0).getIdEcheance()).orElse(null);
                if (echeance != null) {
                    utils.ajouterLigneTableau(tableau, "Type", echeance.getEntiteEcheanceType().name(), fontLabel, fontValeur);
                    utils.ajouterLigneTableau(tableau, "Échéance",
                            DateUtils.nomMoisFrancais(echeance.getDateEcheance()) + " " + echeance.getDateEcheance().getYear(), fontLabel, fontValeur);
                }
                return;
            }

            List<PaiementLocationBienService> liensBienService = paiementLocationBienServiceRepository.findByIdPaiement(paiement.getIdPaiement());
            if (!liensBienService.isEmpty()) {
                PaiementLocationBienService lien = liensBienService.get(0);
                LocationBienService location = locationBienServiceRepository.findById(lien.getIdLocationBienService()).orElse(null);
                if (location != null) {
                    utils.ajouterLigneTableau(tableau, "Bien/Service", location.getBienService().getLibelle(), fontLabel, fontValeur);
                    utils.ajouterLigneTableau(tableau, "Destination", location.getDestination() != null ? location.getDestination() : "—", fontLabel, fontValeur);
                    utils.ajouterLigneTableau(tableau, "Période",
                            location.getDateDebut().format(FMT_DATE) + " au " + location.getDateFin().format(FMT_DATE), fontLabel, fontValeur);
                    utils.ajouterLigneTableau(tableau, "Nature du versement", lien.getTypePaiement().name(), fontLabel, fontValeur); // NORMAL ou PROLONGATION
                }
            }
        }

        // ── Cadre montant + QR signé ──────────────────────────────────────────────

        private void ajouterMontantEnCadre(org.openpdf.text.Document document, Paiement paiement, String numero) throws Exception {
            String donnees = numero + "|reference:" + paiement.getReferencePaiement() + "|montant:" + paiement.getMontantPaiement()
                    + "|date:" + paiement.getDatePaiement();
            String payloadQr = donnees + "|" + utils.signer(donnees);

            PdfPTable cadre = new PdfPTable(2);
            cadre.setWidthPercentage(100);
            cadre.setWidths(new float[]{55f, 45f});
            cadre.setSpacingBefore(4f);
            cadre.setSpacingAfter(4f);
            cadre.addCell(creerCelluleQrEtTexte(payloadQr, numero));
            cadre.addCell(creerCelluleMontant(paiement));

            document.add(cadre);
        }

        private PdfPCell creerCelluleQrEtTexte(String payloadQr, String numero) throws Exception {
            Font fontTexte = FontFactory.getFont(FontFactory.HELVETICA, 8, Font.NORMAL, COULEUR_TEXTE);

            PdfPCell cell = new PdfPCell();
            cell.setBorder(Rectangle.NO_BORDER);
            cell.setPadding(0f);

            PdfPTable sousTable = new PdfPTable(2);
            sousTable.setWidthPercentage(100);
            sousTable.setWidths(new float[]{40f, 60f});

            PdfPCell cellQr = new PdfPCell(utils.genererQrCode(payloadQr, 70, 70));
            cellQr.setBorder(Rectangle.NO_BORDER);
            cellQr.setVerticalAlignment(Element.ALIGN_MIDDLE);
            cellQr.setPadding(2f);
            sousTable.addCell(cellQr);

            PdfPCell cellTexte = new PdfPCell();
            cellTexte.setBorder(Rectangle.NO_BORDER);
            cellTexte.setVerticalAlignment(Element.ALIGN_MIDDLE);
            cellTexte.setPadding(2f);
            cellTexte.addElement(new Paragraph("N° " + numero, FontFactory.getFont(FontFactory.HELVETICA, 8, Font.BOLD, COULEUR_ENTETE)));
            cellTexte.addElement(new Paragraph("Scannant le QR code.", fontTexte));
            sousTable.addCell(cellTexte);

            cell.addElement(sousTable);
            return cell;
        }

        private PdfPCell creerCelluleMontant(Paiement paiement) {
            Font fontLabel   = FontFactory.getFont(FontFactory.HELVETICA, 9, Font.BOLD, COULEUR_ACCENT);
            Font fontMontant = FontFactory.getFont(FontFactory.HELVETICA, 12, Font.BOLD, COULEUR_ACCENT);

            PdfPCell wrapper = new PdfPCell();
            wrapper.setBorder(Rectangle.NO_BORDER);
            wrapper.setHorizontalAlignment(Element.ALIGN_RIGHT);
            wrapper.setPadding(0f);

            PdfPTable sousTable = new PdfPTable(1);
            sousTable.setWidthPercentage(60f);
            sousTable.setHorizontalAlignment(Element.ALIGN_RIGHT);

            PdfPCell cellMontant = new PdfPCell();
            cellMontant.setBackgroundColor(COULEUR_FOND_MONTANT);
            cellMontant.setPadding(4f);

            Paragraph label = new Paragraph("MONTANT (" + paiement.getSens().name() + ")", fontLabel);
            label.setAlignment(Element.ALIGN_CENTER);
            cellMontant.addElement(label);

            Paragraph montant = new Paragraph(FMT_MONTANT.format(paiement.getMontantPaiement()) + " FCFA", fontMontant);
            montant.setAlignment(Element.ALIGN_CENTER);
            cellMontant.addElement(montant);

            sousTable.addCell(cellMontant);
            wrapper.addElement(sousTable);
            return wrapper;
        }

        private String genererNumero() {
            return String.format("GI.BF-REC-%s-%s", LocalDate.now().getYear(),
                    UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        }
    }

//    private final PaiementRepository paiementRepository;
//    private final DocumentRepository documentRepository;
//    private final DocumentStorageService documentStorageService;
//    private final PaiementEcheanceRepository paiementEcheanceRepository;
//    private final EcheanceLoyerRepository echeanceLoyerRepository;
//    private final PaiementLocationBienServiceRepository paiementLocationBienServiceRepository;
//    private final LocationBienServiceRepository locationBienServiceRepository;
//
//    public RecuDocumentService(PaiementRepository paiementRepository, DocumentRepository documentRepository,
//                               DocumentStorageService documentStorageService, PaiementEcheanceRepository paiementEcheanceRepository, EcheanceLoyerRepository echeanceLoyerRepository, PaiementLocationBienServiceRepository paiementLocationBienServiceRepository, LocationBienServiceRepository locationBienServiceRepository) {
//        this.paiementRepository = paiementRepository;
//        this.documentRepository = documentRepository;
//        this.documentStorageService = documentStorageService;
//        this.paiementEcheanceRepository = paiementEcheanceRepository;
//        this.echeanceLoyerRepository = echeanceLoyerRepository;
//        this.paiementLocationBienServiceRepository = paiementLocationBienServiceRepository;
//        this.locationBienServiceRepository = locationBienServiceRepository;
//    }
//
//    /**
//     * Génère le reçu une seule fois (immuable) — si un document existe déjà
//     * pour ce paiement, le renvoie sans régénérer.
//     */
//    @Transactional
//    public ResponseEntity<?> genererOuRecuperer(Integer idPaiement, Integer currentUserId) {
//        var existant = documentRepository.findByEntiteTypeAndEntiteIdOrderByCreatedAtDesc(
//                TypeEntiteDocument.PAIEMENT, idPaiement);
//
//        Document document;
//        if (!existant.isEmpty()) {
//            document = existant.getFirst();
//        } else {
//            Paiement paiement = paiementRepository.findById(idPaiement)
//                    .orElseThrow(() -> new ResourceNotFoundException("paiement", idPaiement));
//
//            byte[] pdf = genererPdf(paiement);
//            String cle = documentStorageService.store(pdf, "recus");
//
//            document = Document.builder()
//                    .typeDocument(TypeDocument.RECU)
//                    .entiteType(TypeEntiteDocument.PAIEMENT)
//                    .entiteId(idPaiement)
//                    .cheminFichier(cle)
//                    .createdAt(LocalDateTime.now())
//                    .userCreate(currentUserId)
//                    .build();
//            documentRepository.save(document);
//        }
//
//        String url = documentStorageService.genererUrlPresignee(document.getCheminFichier());
//        return buildSuccessResponse(HttpStatus.OK, "Reçu disponible", "RECU_GENERATED", url);
//    }
//
//    private byte[] genererPdf(Paiement paiement) {
//        PdfBuilder builder = new PdfBuilder("REÇU DE PAIEMENT N°" + paiement.getIdPaiement());
//
//        Table table = new Table(UnitValue.createPercentArray(new float[]{1, 2})).useAllAvailableWidth();
//        ajouterLigne(table, "Date", paiement.getDatePaiement().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
//        ajouterLigne(table, "Montant", paiement.getMontantPaiement() + " FCFA");
//        ajouterLigne(table, "Mode de paiement", paiement.getModePaiement());
//        ajouterLigne(table, "Référence", paiement.getReferencePaiement() != null ? paiement.getReferencePaiement() : "—");
//        ajouterLigne(table, "Sens", paiement.getSens().name());
//
//        ajouterContexteSpecifique(table, paiement);
//
//        builder.document.add(table);
//        builder.document.add(new Paragraph("Ce reçu fait foi de la transaction décrite ci-dessus.").setMarginTop(20));
//
//        return builder.genererEtFermer();
//    }
//
//    /**
//     * Ajoute les lignes propres au contexte du paiement : échéance de loyer/mandat
//     * si liée via paiement_echeance, ou location de bien/service si liée via
//     * paiement_location_bien_service. Les deux voies sont mutuellement exclusives.
//     */
//    private void ajouterContexteSpecifique(Table table, Paiement paiement) {
//        List<PaiementEcheance> liensEcheance = paiementEcheanceRepository.findByIdPaiement(paiement.getIdPaiement());
//        if (!liensEcheance.isEmpty()) {
//            EcheanceLoyer echeance = echeanceLoyerRepository.findById(liensEcheance.get(0).getIdEcheance()).orElse(null);
//            if (echeance != null) {
//                ajouterLigne(table, "Type", echeance.getEntiteEcheanceType().name());
//                ajouterLigne(table, "Échéance", DateUtils.nomMoisFrancais(echeance.getDateEcheance()) + " " + echeance.getDateEcheance().getYear());
//            }
//            return;
//        }
//
//        List<PaiementLocationBienService> liensBienService = paiementLocationBienServiceRepository.findByIdPaiement(paiement.getIdPaiement());
//        if (!liensBienService.isEmpty()) {
//            PaiementLocationBienService lien = liensBienService.get(0);
//            LocationBienService location = locationBienServiceRepository.findById(lien.getIdLocationBienService()).orElse(null);
//            if (location != null) {
//                ajouterLigne(table, "Bien/Service", location.getBienService().getLibelle());
//                ajouterLigne(table, "Destination", location.getDestination() != null ? location.getDestination() : "—");
//                ajouterLigne(table, "Période",
//                        location.getDateDebut().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + " au " +
//                                location.getDateFin().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
//                ajouterLigne(table, "Nature du versement", lien.getTypePaiement().name()); // NORMAL ou PROLONGATION
//            }
//        }
//
//    }
//
//    private void ajouterLigne(Table table, String libelle, String valeur) {
//        table.addCell(libelle);
//        table.addCell(valeur);
//    }

