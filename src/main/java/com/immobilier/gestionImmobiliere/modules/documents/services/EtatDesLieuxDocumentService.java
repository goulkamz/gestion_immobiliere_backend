package com.immobilier.gestionImmobiliere.modules.documents.services;

import com.immobilier.gestionImmobiliere.donnees.biens.model.Maison;
import com.immobilier.gestionImmobiliere.donnees.contrats.model.ContratLocation;
import com.immobilier.gestionImmobiliere.donnees.contrats.model.StatutLocation;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static com.immobilier.gestionImmobiliere.utils.BuildSuccessResponse.buildSuccessResponse;

@Service
public class EtatDesLieuxDocumentService {

    private static final Logger log = LoggerFactory.getLogger(EtatDesLieuxDocumentService.class);

    private static final Color COULEUR_ENTETE = new Color(31, 56, 100);
    private static final Color COULEUR_ACCENT = new Color(206, 17, 38);
    private static final String LOGO_PATH     = "/image/axios-logo.png";
    private static final DateTimeFormatter FMT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ContratLocationRepository contratLocationRepository;
    private final DocumentRepository documentRepository;
    private final DocumentStorageService documentStorageService;
    private final Utils utils;

    public EtatDesLieuxDocumentService(ContratLocationRepository contratLocationRepository,
                                       DocumentRepository documentRepository,
                                       DocumentStorageService documentStorageService,
                                       Utils utils) {
        this.contratLocationRepository = contratLocationRepository;
        this.documentRepository = documentRepository;
        this.documentStorageService = documentStorageService;
        this.utils = utils;
    }

    @PreAuthorize(
            "hasAnyRole('ADMIN','AGENT') " +
                    "or @contratLocationSecurity.isAccessible(#idContrat, authentication.principal.idUser)"
    )
    @Transactional
    public ResponseEntity<?> genererEntree(Integer idContrat, Integer currentUserId) {
        ContratLocation contrat = contratLocationRepository.findById(idContrat)
                .orElseThrow(() -> new ResourceNotFoundException("contrat de location", idContrat));

        if (contrat.getEtatDesLieuxEntree() == null || contrat.getEtatDesLieuxEntree().isBlank()) {
            throw new IllegalStateException("Aucun état des lieux d'entrée n'a été renseigné pour ce contrat");
        }

        return genererOuRecuperer(TypeDocument.ETAT_DES_LIEUX_ENTREE, contrat, contrat.getEtatDesLieuxEntree(),
                "Entrée", currentUserId);
    }

    @PreAuthorize(
            "hasAnyRole('ADMIN','AGENT') " +
                    "or @contratLocationSecurity.isAccessible(#idContrat, authentication.principal.idUser)"
    )
    @Transactional
    public ResponseEntity<?> genererSortie(Integer idContrat, Integer currentUserId) {
        ContratLocation contrat = contratLocationRepository.findById(idContrat)
                .orElseThrow(() -> new ResourceNotFoundException("contrat de location", idContrat));

        if (contrat.getStatut() != StatutLocation.TERMINE && contrat.getStatut() != StatutLocation.RESILIE) {
            throw new IllegalStateException("L'état des lieux de sortie n'est disponible qu'après clôture du contrat");
        }
        if (contrat.getEtatDesLieuxSortie() == null || contrat.getEtatDesLieuxSortie().isBlank()) {
            throw new IllegalStateException("Aucun état des lieux de sortie n'a été renseigné pour ce contrat");
        }

        return genererOuRecuperer(TypeDocument.ETAT_DES_LIEUX_SORTIE, contrat, contrat.getEtatDesLieuxSortie(),
                "Sortie", currentUserId);
    }

    private ResponseEntity<?> genererOuRecuperer(TypeDocument type, ContratLocation contrat, String descriptionEtat,
                                                 String libelle, Integer currentUserId) {
        var existant = documentRepository.findByEntiteTypeAndEntiteIdOrderByCreatedAtDesc(
                TypeEntiteDocument.CONTRAT_LOCATION, contrat.getIdContratLocation());

        // Filtre sur le bon type précisément (entrée/sortie), le repository renvoie les deux confondus
        var existantPourType = existant.stream().filter(d -> d.getTypeDocument() == type).findFirst();

        Document document;
        if (existantPourType.isPresent()) {
            document = existantPourType.get();
        } else {
            byte[] pdf = genererPdf(contrat, descriptionEtat, libelle);
            String cle = documentStorageService.store(pdf, "etats-des-lieux");

            document = Document.builder()
                    .typeDocument(type)
                    .entiteType(TypeEntiteDocument.CONTRAT_LOCATION)
                    .entiteId(contrat.getIdContratLocation())
                    .cheminFichier(cle)
                    .createdAt(LocalDateTime.now())
                    .userCreate(currentUserId)
                    .build();
            documentRepository.save(document);
        }

        String url = documentStorageService.genererUrlPresignee(document.getCheminFichier());
        return buildSuccessResponse(HttpStatus.OK, "État des lieux (" + libelle + ") disponible", "ETAT_DES_LIEUX_GENERATED", url);
    }

    // ── Génération PDF (OpenPDF) ─────────────────────────────────────────────

    private byte[] genererPdf(ContratLocation contrat, String descriptionEtat, String libelle) {
        var maison = contrat.getMaison();
        var locataire = contrat.getLocataire();
        var dateConstat = "Entrée".equals(libelle) ? contrat.getDateEntree() : contrat.getDateSortie();
        String numero = genererNumero(contrat, libelle);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        org.openpdf.text.Document document = new org.openpdf.text.Document(PageSize.A4, 50, 50, 60, 60);

        try {
            PdfWriter writer = PdfWriter.getInstance(document, out);
            Image logoFiligrane = utils.chargerLogo(LOGO_PATH);
            writer.setPageEvent(new Utils.FiligraneLogo(logoFiligrane, 380, 380));

            document.open();

            utils.ajouterBandeauNational(document, writer);
            utils.ajouterEntete(document);
            utils.ajouterTitre(document, "ÉTAT DES LIEUX — " + libelle.toUpperCase() + " N° " + numero,
                    "Agence Générale Immobilière GI.BF");

            ajouterInfosLocation(document, locataire, maison, dateConstat);
            ajouterConstat(document, descriptionEtat);

            String donnees = numero + "|" + contrat.getIdContratLocation() + "|" + libelle + "|" + dateConstat;
            String payloadQr = donnees + "|" + utils.signer(donnees);
            utils.ajouterPied(document, "N° État des lieux : "+ numero, payloadQr,"","ETAT_DES_LIEUX");

        } catch (Exception e) {
            log.error("Erreur génération PDF état des lieux", e);
            throw new RuntimeException("Échec génération état des lieux PDF", e);
        } finally {
            if (document.isOpen()) document.close();
        }

        return out.toByteArray();
    }

    private void ajouterInfosLocation(org.openpdf.text.Document document, Object locataireObj, Object maisonObj, Object dateConstat) throws DocumentException {
        var locataire = (User) locataireObj;
        var maison = (Maison) maisonObj;

        PdfPTable tableau = utils.creerTableauInfos();
        Font fontLabel  = FontFactory.getFont(FontFactory.HELVETICA, 9, Font.BOLD, COULEUR_ENTETE);
        Font fontValeur = FontFactory.getFont(FontFactory.HELVETICA, 9, Font.NORMAL, Color.DARK_GRAY);

        utils.ajouterLigneTableau(tableau, "Bien", maison.getNomCommunMaison(), fontLabel, fontValeur);
        utils.ajouterLigneTableau(tableau, "Locataire", locataire.getNom() + " " + locataire.getPrenom(), fontLabel, fontValeur);
        utils.ajouterLigneTableau(tableau, "Date du constat", formaterDate(dateConstat), fontLabel, fontValeur);

        document.add(tableau);
    }

    private void ajouterConstat(org.openpdf.text.Document document, String descriptionEtat) throws DocumentException {
        ajouterTitreSection(document, "CONSTAT");

        Font fontTexte = FontFactory.getFont(FontFactory.HELVETICA, 9, Font.NORMAL, Color.DARK_GRAY);
        Paragraph constat = new Paragraph(descriptionEtat, fontTexte);
        constat.setAlignment(Element.ALIGN_JUSTIFIED);
        document.add(constat);
    }

    private void ajouterTitreSection(org.openpdf.text.Document document, String titre) throws DocumentException {
        Font fontSection = FontFactory.getFont(FontFactory.HELVETICA, 11, Font.BOLD, COULEUR_ACCENT);
        Paragraph p = new Paragraph(titre, fontSection);
        p.setSpacingBefore(14f);
        p.setSpacingAfter(4f);
        document.add(p);
    }

    /**
     * dateConstat peut être un LocalDate ou un LocalDateTime selon le champ source
     * (getDateEntree()/getDateSortie()) — formatage défensif plutôt qu'un cast fixe.
     */
    private String formaterDate(Object dateConstat) {
        if (dateConstat == null) return "—";
        if (dateConstat instanceof LocalDateTime ldt) return ldt.format(FMT_DATE);
        if (dateConstat instanceof java.time.LocalDate ld) return ld.format(FMT_DATE);
        return dateConstat.toString();
    }

    private String genererNumero(ContratLocation contrat, String libelle) {
        String suffixe = "Entrée".equals(libelle) ? "E" : "S";
        return String.format("GI.BF-EDL%s-%s-%s", suffixe, contrat.getIdContratLocation(),
                UUID.randomUUID().toString().substring(0, 6).toUpperCase());
    }
}