package com.immobilier.gestionImmobiliere.modules.documents.services;

import com.immobilier.gestionImmobiliere.donnees.contrats.model.DecompteSortie;
import com.immobilier.gestionImmobiliere.donnees.contrats.repository.DecompteSortieRepository;
import com.immobilier.gestionImmobiliere.donnees.documents.model.Document;
import com.immobilier.gestionImmobiliere.donnees.documents.model.TypeDocument;
import com.immobilier.gestionImmobiliere.donnees.documents.model.TypeEntiteDocument;
import com.immobilier.gestionImmobiliere.donnees.documents.repository.DocumentRepository;
import com.immobilier.gestionImmobiliere.exceptions.ResourceNotFoundException;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static com.immobilier.gestionImmobiliere.utils.BuildSuccessResponse.buildSuccessResponse;

@Service
public class DecompteSortieDocumentService {

    private final DecompteSortieRepository decompteSortieRepository;
    private final DocumentRepository documentRepository;
    private final DocumentStorageService documentStorageService;

    public DecompteSortieDocumentService(DecompteSortieRepository decompteSortieRepository,
                                         DocumentRepository documentRepository,
                                         DocumentStorageService documentStorageService) {
        this.decompteSortieRepository = decompteSortieRepository;
        this.documentRepository = documentRepository;
        this.documentStorageService = documentStorageService;
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

        if (decompte.getStatut() != com.immobilier.gestionImmobiliere.donnees.contrats.model.StatutDecompteSortie.REGLE) {
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

    private byte[] genererPdf(DecompteSortie decompte) {
        var contrat = decompte.getContratLocation();
        var maison = contrat.getMaison();
        var locataire = contrat.getLocataire();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        PdfBuilder builder = new PdfBuilder("DÉCOMPTE DE SORTIE N°" + decompte.getIdDecompte());

        Table infos = new Table(UnitValue.createPercentArray(new float[]{1, 2})).useAllAvailableWidth();
        infos.addCell("Locataire"); infos.addCell(locataire.getNom() + " " + locataire.getPrenom());
        infos.addCell("Bien"); infos.addCell(maison.getNomCommunMaison());
        infos.addCell("Date de sortie"); infos.addCell(decompte.getDateSortie().format(fmt));
        builder.document.add(infos);

        builder.document.add(new Paragraph("DÉTAIL DU CALCUL").setBold().setMarginTop(15));

        Table detail = new Table(UnitValue.createPercentArray(new float[]{2, 1})).useAllAvailableWidth();
        ajouterLigne(detail, "Avance de référence", decompte.getMontantAvanceReference());
        ajouterLigne(detail, "Caution de référence", decompte.getMontantCautionReference());
        ajouterLigne(detail, "Arriérés de loyer constatés", decompte.getMontantArrieres());
        ajouterLigne(detail, "Coût des réparations évalué", decompte.getCoutReparation());
        ajouterLigne(detail, "Déduit sur l'avance", decompte.getMontantDeduitAvance());
        ajouterLigne(detail, "Déduit sur la caution", decompte.getMontantDeduitCaution());
        builder.document.add(detail);

        boolean remboursement = decompte.getMontantARembourser().compareTo(BigDecimal.ZERO) > 0;
        String libelleFinal = remboursement ? "MONTANT REMBOURSÉ AU LOCATAIRE" : "MONTANT DÛ PAR LE LOCATAIRE";
        BigDecimal montantFinal = remboursement ? decompte.getMontantARembourser() : decompte.getMontantManquant();

        builder.document.add(new Paragraph(libelleFinal + " : " + montantFinal + " FCFA")
                .setBold().setFontSize(13)
                .setFontColor(remboursement ? ColorConstants.DARK_GRAY : ColorConstants.RED)
                .setMarginTop(20));

        return builder.genererEtFermer();
    }

    private void ajouterLigne(Table table, String libelle, BigDecimal montant) {
        table.addCell(libelle);
        table.addCell((montant != null ? montant : BigDecimal.ZERO) + " FCFA");
    }
}