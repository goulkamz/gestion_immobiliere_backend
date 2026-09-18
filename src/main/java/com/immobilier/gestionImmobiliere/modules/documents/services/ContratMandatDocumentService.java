package com.immobilier.gestionImmobiliere.modules.documents.services;

import com.immobilier.gestionImmobiliere.donnees.contrats.model.ContratMandat;
import com.immobilier.gestionImmobiliere.donnees.contrats.repository.ContratMandatRepository;
import com.immobilier.gestionImmobiliere.donnees.documents.model.Document;
import com.immobilier.gestionImmobiliere.donnees.documents.model.TypeDocument;
import com.immobilier.gestionImmobiliere.donnees.documents.model.TypeEntiteDocument;
import com.immobilier.gestionImmobiliere.donnees.documents.repository.DocumentRepository;
import com.immobilier.gestionImmobiliere.exceptions.ResourceNotFoundException;
import com.itextpdf.layout.element.Cell;
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
public class ContratMandatDocumentService {

    private final ContratMandatRepository contratMandatRepository;
    private final DocumentRepository documentRepository;
    private final DocumentStorageService documentStorageService;

    public ContratMandatDocumentService(ContratMandatRepository contratMandatRepository,
                                        DocumentRepository documentRepository,
                                        DocumentStorageService documentStorageService) {
        this.contratMandatRepository = contratMandatRepository;
        this.documentRepository = documentRepository;
        this.documentStorageService = documentStorageService;
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

    private byte[] genererPdf(ContratMandat mandat) {
        PdfBuilder builder = new PdfBuilder("CONTRAT DE MANDAT DE GESTION N°" + mandat.getIdMandat());

        var cour = mandat.getCour();
        var bailleur = cour.getProprietaire();
        var agent = mandat.getAgent();

        builder.document.add(new Paragraph("ENTRE LES SOUSSIGNÉS").setBold().setMarginTop(10));

        Table parties = new Table(UnitValue.createPercentArray(new float[]{1, 2})).useAllAvailableWidth();
        ajouterLigne(parties, "Mandant (Bailleur)", bailleur.getNom() + " " + bailleur.getPrenom());
        ajouterLigne(parties, "Mandataire (Agent)", agent.getNom() + " " + agent.getPrenom());
        builder.document.add(parties);

        builder.document.add(new Paragraph("OBJET DU MANDAT").setBold().setMarginTop(15));

        Table objet = new Table(UnitValue.createPercentArray(new float[]{1, 2})).useAllAvailableWidth();
        ajouterLigne(objet, "Cour concernée", cour.getReferenceCour());
        ajouterLigne(objet, "Lot", cour.getLotCour() != null ? cour.getLotCour() : "—");
        ajouterLigne(objet, "Type de mandat", mandat.getTypeMandat() != null ? mandat.getTypeMandat().name() : "—");
        builder.document.add(objet);

        builder.document.add(new Paragraph("CONDITIONS FINANCIÈRES ET DURÉE").setBold().setMarginTop(15));

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        Table conditions = new Table(UnitValue.createPercentArray(new float[]{1, 2})).useAllAvailableWidth();
        ajouterLigne(conditions, "Date de début", mandat.getDateDebut().format(fmt));
        ajouterLigne(conditions, "Date de fin", mandat.getDateFin() != null ? mandat.getDateFin().format(fmt) : "Durée indéterminée");
        ajouterLigne(conditions, "Commission", mandat.getCommission() + " %");
        ajouterLigne(conditions, "Mode de facturation", mandat.getModeFacturation() != null ? mandat.getModeFacturation() : "—");
        builder.document.add(conditions);

        builder.document.add(new Paragraph(
                "L'agent mandataire est chargé de la gestion locative de la cour désignée ci-dessus : " +
                        "recherche de locataires, encaissement des loyers, entretien courant. La commission indiquée " +
                        "est déduite mensuellement des loyers encaissés avant reversement au mandant.")
                .setFontSize(9).setMarginTop(20));

        Table signatures = new Table(UnitValue.createPercentArray(new float[]{1, 1})).useAllAvailableWidth().setMarginTop(40);
        signatures.addCell(new Cell().add(new Paragraph("Le Mandant").setTextAlignment(TextAlignment.CENTER)).setBorder(null));
        signatures.addCell(new Cell().add(new Paragraph("Le Mandataire").setTextAlignment(TextAlignment.CENTER)).setBorder(null));
        builder.document.add(signatures);

        return builder.genererEtFermer();
    }

    private void ajouterLigne(Table table, String libelle, String valeur) {
        table.addCell(libelle);
        table.addCell(valeur);
    }
}