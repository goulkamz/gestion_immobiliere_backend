package com.immobilier.gestionImmobiliere.modules.documents.services;

import com.immobilier.gestionImmobiliere.donnees.contrats.model.ContratLocation;
import com.immobilier.gestionImmobiliere.donnees.contrats.model.StatutLocation;
import com.immobilier.gestionImmobiliere.donnees.contrats.repository.ContratLocationRepository;
import com.immobilier.gestionImmobiliere.donnees.documents.model.Document;
import com.immobilier.gestionImmobiliere.donnees.documents.model.TypeDocument;
import com.immobilier.gestionImmobiliere.donnees.documents.model.TypeEntiteDocument;
import com.immobilier.gestionImmobiliere.donnees.documents.repository.DocumentRepository;
import com.immobilier.gestionImmobiliere.exceptions.ResourceNotFoundException;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
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
public class EtatDesLieuxDocumentService {

    private final ContratLocationRepository contratLocationRepository;
    private final DocumentRepository documentRepository;
    private final DocumentStorageService documentStorageService;

    public EtatDesLieuxDocumentService(ContratLocationRepository contratLocationRepository,
                                       DocumentRepository documentRepository,
                                       DocumentStorageService documentStorageService) {
        this.contratLocationRepository = contratLocationRepository;
        this.documentRepository = documentRepository;
        this.documentStorageService = documentStorageService;
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

    private byte[] genererPdf(ContratLocation contrat, String descriptionEtat, String libelle) {
        PdfBuilder builder = new PdfBuilder("ÉTAT DES LIEUX — " + libelle.toUpperCase() +
                " — Contrat N°" + contrat.getIdContratLocation());

        var maison = contrat.getMaison();
        var locataire = contrat.getLocataire();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        LocalDateTime dateConstat = "Entrée".equals(libelle) ? contrat.getDateEntree() : contrat.getDateSortie();

        Table infos = new Table(UnitValue.createPercentArray(new float[]{1, 2})).useAllAvailableWidth();
        infos.addCell("Bien"); infos.addCell(maison.getNomCommunMaison());
        infos.addCell("Locataire"); infos.addCell(locataire.getNom() + " " + locataire.getPrenom());
        infos.addCell("Date du constat"); infos.addCell(dateConstat != null ? dateConstat.format(fmt) : "—");
        builder.document.add(infos);

        builder.document.add(new Paragraph("Constat :").setBold().setMarginTop(15));
        builder.document.add(new Paragraph(descriptionEtat).setMarginTop(5));

        return builder.genererEtFermer();
    }
}