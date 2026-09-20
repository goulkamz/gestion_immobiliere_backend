package com.immobilier.gestionImmobiliere.modules.documents.services;

import com.immobilier.gestionImmobiliere.donnees.contrats.repository.DecompteSortieRepository;
import com.immobilier.gestionImmobiliere.donnees.documents.model.Document;
import com.immobilier.gestionImmobiliere.donnees.documents.model.TypeDocument;
import com.immobilier.gestionImmobiliere.donnees.documents.repository.DocumentRepository;
import com.immobilier.gestionImmobiliere.donnees.paiements.repository.PaiementLocationBienServiceRepository;
import com.immobilier.gestionImmobiliere.donnees.paiements.repository.EcheanceLoyerRepository;
import com.immobilier.gestionImmobiliere.donnees.paiements.repository.RemboursementRepository;
import com.immobilier.gestionImmobiliere.modules.statistiques.projection.SumRetard;
import com.immobilier.gestionImmobiliere.utils.DateUtils;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static com.immobilier.gestionImmobiliere.utils.BuildSuccessResponse.buildSuccessResponse;

@Service
public class RapportMensuelDocumentService {

    private final EcheanceLoyerRepository echeanceLoyerRepository;
    private final PaiementLocationBienServiceRepository paiementLocationBienServiceRepository;
    private final RemboursementRepository remboursementRepository;
    private final DecompteSortieRepository decompteSortieRepository;
    private final DocumentRepository documentRepository;
    private final DocumentStorageService documentStorageService;

    public RapportMensuelDocumentService(EcheanceLoyerRepository echeanceLoyerRepository,
                                         PaiementLocationBienServiceRepository paiementLocationBienServiceRepository,
                                         RemboursementRepository remboursementRepository,
                                         DecompteSortieRepository decompteSortieRepository,
                                         DocumentRepository documentRepository,
                                         DocumentStorageService documentStorageService) {
        this.echeanceLoyerRepository = echeanceLoyerRepository;
        this.paiementLocationBienServiceRepository = paiementLocationBienServiceRepository;
        this.remboursementRepository = remboursementRepository;
        this.decompteSortieRepository = decompteSortieRepository;
        this.documentRepository = documentRepository;
        this.documentStorageService = documentStorageService;
    }

    /**
     * Réservé Admin. Un seul rapport par mois (immuable une fois généré) —
     * appelable à tout moment (avant ou après le 15), mais le job planifié
     * s'assure qu'il existe automatiquement chaque 15 du mois.
     */
    @Transactional
    public ResponseEntity<?> genererOuRecuperer(LocalDate periode, Integer currentUserId) {
        LocalDate debutMois = periode.withDayOfMonth(1);

        var existant = documentRepository.findByTypeDocumentAndPeriodeMois(TypeDocument.RAPPORT_MENSUEL, debutMois);

        Document document = existant.orElseGet(() -> {
            byte[] pdf = genererPdf(debutMois);
            String cle = documentStorageService.store(pdf, "rapports-mensuels");

            Document nouveau = Document.builder()
                    .typeDocument(TypeDocument.RAPPORT_MENSUEL)
                    .periodeMois(debutMois)
                    .cheminFichier(cle)
                    .createdAt(LocalDateTime.now())
                    .userCreate(currentUserId)
                    .build();
            return documentRepository.save(nouveau);
        });

        String url = documentStorageService.genererUrlPresignee(document.getCheminFichier());
        return buildSuccessResponse(HttpStatus.OK, "Rapport mensuel disponible", "RAPPORT_MENSUEL_GENERATED", url);
    }

    private byte[] genererPdf(LocalDate periode) {
        String libellePeriode = DateUtils.nomMoisFrancais(periode) + " " + periode.getYear();
        PdfBuilder builder = new PdfBuilder("RAPPORT MENSUEL DE GESTION — " + libellePeriode);

        // --- Loyers ---
        builder.document.add(new Paragraph("LOYERS & COMMISSIONS").setBold().setFontSize(12).setMarginTop(10));
        Table loyers = new Table(UnitValue.createPercentArray(new float[]{2, 1})).useAllAvailableWidth();
        ajouterLigne(loyers, "Loyers encaissés", echeanceLoyerRepository.sumLoyersEncaissesDuMois(periode));
        ajouterLigne(loyers, "Pénalités de retard encaissées", echeanceLoyerRepository.sumPenalitesEncaisseesDuMois(periode));
        ajouterLigne(loyers, "Commission de l'agence", echeanceLoyerRepository.sumCommissionAgenceDuMois(periode));
        ajouterLigne(loyers, "Montant dû aux bailleurs", echeanceLoyerRepository.sumMontantDuAuxBailleursDuMois(periode));
        builder.document.add(loyers);

        // --- Retards ---
        SumRetard retard = echeanceLoyerRepository.sumEnRetard();
        builder.document.add(new Paragraph("ÉCHÉANCES EN RETARD (situation actuelle)").setBold().setFontSize(12).setMarginTop(15));
        Table retards = new Table(UnitValue.createPercentArray(new float[]{2, 1})).useAllAvailableWidth();
        retards.addCell("Nombre d'échéances en retard"); retards.addCell(String.valueOf(retard.getNombre()));
        retards.addCell("Montant total en retard"); retards.addCell(retard.getMontant() + " FCFA");
        builder.document.add(retards);

        // --- Biens/services ---
        builder.document.add(new Paragraph("LOCATIONS BIENS & SERVICES").setBold().setFontSize(12).setMarginTop(15));
        Table biensServices = new Table(UnitValue.createPercentArray(new float[]{2, 1})).useAllAvailableWidth();
        ajouterLigne(biensServices, "Total encaissé", paiementLocationBienServiceRepository.sumEncaisseBienServiceDuMois(periode));
        ajouterLigne(biensServices, "Total remboursé", remboursementRepository.sumRembourseDuMois(periode));
        builder.document.add(biensServices);

        // --- Décomptes de sortie ---
        var decomptesRegles = decompteSortieRepository.findReglesDuMois(periode);
        builder.document.add(new Paragraph("DÉCOMPTES DE SORTIE RÉGLÉS CE MOIS").setBold().setFontSize(12).setMarginTop(15));
        builder.document.add(new Paragraph(decomptesRegles.size() + " décompte(s) réglé(s) durant cette période.").setMarginTop(5));

        BigDecimal totalRembourseLocataires = decomptesRegles.stream()
                .map(d -> d.getMontantARembourser() != null ? d.getMontantARembourser() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalManquantsLocataires = decomptesRegles.stream()
                .map(d -> d.getMontantManquant() != null ? d.getMontantManquant() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Table decomptes = new Table(UnitValue.createPercentArray(new float[]{2, 1})).useAllAvailableWidth();
        ajouterLigne(decomptes, "Total remboursé aux locataires", totalRembourseLocataires);
        ajouterLigne(decomptes, "Total manquants facturés", totalManquantsLocataires);
        builder.document.add(decomptes);

        builder.document.add(new Paragraph(
                "Rapport généré le 15 du mois : on suppose qu'à cette date tous les paiements " +
                        "et virements du mois sont effectués.")
                .setFontSize(8).setMarginTop(25));

        return builder.genererEtFermer();
    }

    private void ajouterLigne(Table table, String libelle, BigDecimal montant) {
        table.addCell(libelle);
        table.addCell((montant != null ? montant : BigDecimal.ZERO) + " FCFA");
    }
}
