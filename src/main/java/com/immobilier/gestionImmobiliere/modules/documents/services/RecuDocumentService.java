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
import com.immobilier.gestionImmobiliere.utils.DateUtils;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static com.immobilier.gestionImmobiliere.utils.BuildSuccessResponse.buildSuccessResponse;

@Service
public class RecuDocumentService {

    private final PaiementRepository paiementRepository;
    private final DocumentRepository documentRepository;
    private final DocumentStorageService documentStorageService;
    private final PaiementEcheanceRepository paiementEcheanceRepository;
    private final EcheanceLoyerRepository echeanceLoyerRepository;
    private final PaiementLocationBienServiceRepository paiementLocationBienServiceRepository;
    private final LocationBienServiceRepository locationBienServiceRepository;

    public RecuDocumentService(PaiementRepository paiementRepository, DocumentRepository documentRepository,
                               DocumentStorageService documentStorageService, PaiementEcheanceRepository paiementEcheanceRepository, EcheanceLoyerRepository echeanceLoyerRepository, PaiementLocationBienServiceRepository paiementLocationBienServiceRepository, LocationBienServiceRepository locationBienServiceRepository) {
        this.paiementRepository = paiementRepository;
        this.documentRepository = documentRepository;
        this.documentStorageService = documentStorageService;
        this.paiementEcheanceRepository = paiementEcheanceRepository;
        this.echeanceLoyerRepository = echeanceLoyerRepository;
        this.paiementLocationBienServiceRepository = paiementLocationBienServiceRepository;
        this.locationBienServiceRepository = locationBienServiceRepository;
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

    private byte[] genererPdf(Paiement paiement) {
        PdfBuilder builder = new PdfBuilder("REÇU DE PAIEMENT N°" + paiement.getIdPaiement());

        Table table = new Table(UnitValue.createPercentArray(new float[]{1, 2})).useAllAvailableWidth();
        ajouterLigne(table, "Date", paiement.getDatePaiement().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        ajouterLigne(table, "Montant", paiement.getMontantPaiement() + " FCFA");
        ajouterLigne(table, "Mode de paiement", paiement.getModePaiement());
        ajouterLigne(table, "Référence", paiement.getReferencePaiement() != null ? paiement.getReferencePaiement() : "—");
        ajouterLigne(table, "Sens", paiement.getSens().name());

        ajouterContexteSpecifique(table, paiement);

        builder.document.add(table);
        builder.document.add(new Paragraph("Ce reçu fait foi de la transaction décrite ci-dessus.").setMarginTop(20));

        return builder.genererEtFermer();
    }

    /**
     * Ajoute les lignes propres au contexte du paiement : échéance de loyer/mandat
     * si liée via paiement_echeance, ou location de bien/service si liée via
     * paiement_location_bien_service. Les deux voies sont mutuellement exclusives.
     */
    private void ajouterContexteSpecifique(Table table, Paiement paiement) {
        List<PaiementEcheance> liensEcheance = paiementEcheanceRepository.findByIdPaiement(paiement.getIdPaiement());
        if (!liensEcheance.isEmpty()) {
            EcheanceLoyer echeance = echeanceLoyerRepository.findById(liensEcheance.get(0).getIdEcheance()).orElse(null);
            if (echeance != null) {
                ajouterLigne(table, "Type", echeance.getEntiteEcheanceType().name());
                ajouterLigne(table, "Échéance", DateUtils.nomMoisFrancais(echeance.getDateEcheance()) + " " + echeance.getDateEcheance().getYear());
            }
            return;
        }

        List<PaiementLocationBienService> liensBienService = paiementLocationBienServiceRepository.findByIdPaiement(paiement.getIdPaiement());
        if (!liensBienService.isEmpty()) {
            PaiementLocationBienService lien = liensBienService.get(0);
            LocationBienService location = locationBienServiceRepository.findById(lien.getIdLocationBienService()).orElse(null);
            if (location != null) {
                ajouterLigne(table, "Bien/Service", location.getBienService().getLibelle());
                ajouterLigne(table, "Destination", location.getDestination() != null ? location.getDestination() : "—");
                ajouterLigne(table, "Période",
                        location.getDateDebut().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + " au " +
                                location.getDateFin().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                ajouterLigne(table, "Nature du versement", lien.getTypePaiement().name()); // NORMAL ou PROLONGATION
            }
        }

    }

    private void ajouterLigne(Table table, String libelle, String valeur) {
        table.addCell(libelle);
        table.addCell(valeur);
    }
}