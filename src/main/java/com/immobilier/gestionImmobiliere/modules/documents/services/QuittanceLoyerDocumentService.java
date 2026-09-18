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
import com.immobilier.gestionImmobiliere.exceptions.ResourceNotFoundException;
import com.immobilier.gestionImmobiliere.utils.DateUtils;
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

import static com.immobilier.gestionImmobiliere.utils.BuildSuccessResponse.buildSuccessResponse;

@Service
public class QuittanceLoyerDocumentService {

    private final EcheanceLoyerRepository echeanceLoyerRepository;
    private final ContratLocationRepository contratLocationRepository;
    private final DocumentRepository documentRepository;
    private final DocumentStorageService documentStorageService;

    public QuittanceLoyerDocumentService(EcheanceLoyerRepository echeanceLoyerRepository,
                                         ContratLocationRepository contratLocationRepository,
                                         DocumentRepository documentRepository,
                                         DocumentStorageService documentStorageService) {
        this.echeanceLoyerRepository = echeanceLoyerRepository;
        this.contratLocationRepository = contratLocationRepository;
        this.documentRepository = documentRepository;
        this.documentStorageService = documentStorageService;
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

    private byte[] genererPdf(EcheanceLoyer echeance, ContratLocation contrat) {
        String periode = DateUtils.nomMoisFrancais(echeance.getDateEcheance()) + " " + echeance.getDateEcheance().getYear();
        PdfBuilder builder = new PdfBuilder("QUITTANCE DE LOYER — " + periode);

        var maison = contrat.getMaison();
        var locataire = contrat.getLocataire();
        var bailleur = maison.getCour().getProprietaire();

        Table infos = new Table(UnitValue.createPercentArray(new float[]{1, 2})).useAllAvailableWidth();
        ajouterLigne(infos, "Bailleur", bailleur.getNom() + " " + bailleur.getPrenom());
        ajouterLigne(infos, "Locataire", locataire.getNom() + " " + locataire.getPrenom());
        ajouterLigne(infos, "Bien loué", maison.getNomCommunMaison());
        ajouterLigne(infos, "Période concernée", periode);
        ajouterLigne(infos, "Montant du loyer", echeance.getMontantDu() + " FCFA");

        BigDecimal penalite = echeance.getPenalite() != null ? echeance.getPenalite() : BigDecimal.ZERO;
        if (penalite.compareTo(BigDecimal.ZERO) > 0) {
            ajouterLigne(infos, "Pénalité de retard réglée", penalite + " FCFA");
        }
        ajouterLigne(infos, "Montant total réglé", echeance.getMontantPaye() + " FCFA");
        builder.document.add(infos);

        builder.document.add(new Paragraph(
                "Je soussigné(e) " + bailleur.getNom() + " " + bailleur.getPrenom() +
                        ", bailleur du logement désigné ci-dessus, déclare avoir reçu de " +
                        locataire.getNom() + " " + locataire.getPrenom() +
                        " la somme mentionnée au titre du loyer de la période indiquée, et lui donne quittance " +
                        "de cette somme, sous réserve de tous mes droits.")
                .setFontSize(10).setMarginTop(20));

        return builder.genererEtFermer();
    }

    private void ajouterLigne(Table table, String libelle, String valeur) {
        table.addCell(libelle);
        table.addCell(valeur);
    }
}