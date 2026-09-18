package com.immobilier.gestionImmobiliere.modules.documents.services;

import com.immobilier.gestionImmobiliere.donnees.contrats.model.ContratLocation;
import com.immobilier.gestionImmobiliere.donnees.contrats.repository.ContratLocationRepository;
import com.immobilier.gestionImmobiliere.donnees.documents.model.Document;
import com.immobilier.gestionImmobiliere.donnees.documents.model.TypeDocument;
import com.immobilier.gestionImmobiliere.donnees.documents.model.TypeEntiteDocument;
import com.immobilier.gestionImmobiliere.donnees.documents.repository.DocumentRepository;
import com.immobilier.gestionImmobiliere.exceptions.ResourceNotFoundException;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static com.immobilier.gestionImmobiliere.utils.BuildSuccessResponse.buildSuccessResponse;

@Service
public class ContratLocationDocumentService {

    private final ContratLocationRepository contratLocationRepository;
    private final DocumentRepository documentRepository;
    private final DocumentStorageService documentStorageService;

    public ContratLocationDocumentService(ContratLocationRepository contratLocationRepository,
                                          DocumentRepository documentRepository,
                                          DocumentStorageService documentStorageService) {
        this.contratLocationRepository = contratLocationRepository;
        this.documentRepository = documentRepository;
        this.documentStorageService = documentStorageService;
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

    private byte[] genererPdf(ContratLocation contrat) {
        PdfBuilder builder = new PdfBuilder("CONTRAT DE LOCATION N°" + contrat.getIdContratLocation());

        var maison = contrat.getMaison();
        var cour = maison.getCour();
        var bailleur = cour.getProprietaire();
        var locataire = contrat.getLocataire();

        builder.document.add(new Paragraph("ENTRE LES SOUSSIGNÉS").setBold().setMarginTop(10));

        Table parties = new Table(UnitValue.createPercentArray(new float[]{1, 2})).useAllAvailableWidth();
        ajouterLigne(parties, "Bailleur", bailleur.getNom() + " " + bailleur.getPrenom());
        ajouterLigne(parties, "Locataire", locataire.getNom() + " " + locataire.getPrenom());
        builder.document.add(parties);

        builder.document.add(new Paragraph("OBJET DE LA LOCATION").setBold().setMarginTop(15));

        Table objet = new Table(UnitValue.createPercentArray(new float[]{1, 2})).useAllAvailableWidth();
        ajouterLigne(objet, "Bien loué", maison.getNomCommunMaison() + " (" + maison.getTypeMaison() + ")");
        ajouterLigne(objet, "Référence cour", cour.getReferenceCour());
        ajouterLigne(objet, "Nombre de pièces", String.valueOf(maison.getNombrePiece()));
        builder.document.add(objet);

        builder.document.add(new Paragraph("CONDITIONS FINANCIÈRES ET DURÉE").setBold().setMarginTop(15));

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        Table conditions = new Table(UnitValue.createPercentArray(new float[]{1, 2})).useAllAvailableWidth();
        ajouterLigne(conditions, "Date d'entrée", contrat.getDateEntree().format(fmt));
        ajouterLigne(conditions, "Date de sortie", contrat.getDateSortie() != null ? contrat.getDateSortie().format(fmt) : "Durée indéterminée");
        ajouterLigne(conditions, "Loyer mensuel", contrat.getMontantLoyer() + " FCFA");
        ajouterLigne(conditions, "Caution exigée", maison.getCaution() != null ? maison.getCaution() + " FCFA" : "—");
        ajouterLigne(conditions, "Avance exigée", maison.getAvance() != null ? maison.getAvance() + " FCFA" : "—");
        builder.document.add(conditions);

        builder.document.add(new Paragraph(
                "Le locataire s'engage à payer son loyer mensuellement, au plus tard le 5 du mois suivant " +
                        "le mois occupé, sous peine de pénalité de retard. Toute dégradation constatée à la sortie " +
                        "sera déduite de l'avance puis de la caution, selon les modalités en vigueur.")
                .setFontSize(9).setMarginTop(20));

        Table signatures = new Table(UnitValue.createPercentArray(new float[]{1, 1})).useAllAvailableWidth().setMarginTop(40);
        signatures.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph("Le Bailleur").setTextAlignment(TextAlignment.CENTER)).setBorder(null));
        signatures.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph("Le Locataire").setTextAlignment(TextAlignment.CENTER)).setBorder(null));
        builder.document.add(signatures);

        return builder.genererEtFermer();
    }

    private void ajouterLigne(Table table, String libelle, String valeur) {
        table.addCell(libelle);
        table.addCell(valeur);
    }
}