package com.immobilier.gestionImmobiliere.modules.documents.services;

import com.immobilier.gestionImmobiliere.donnees.contrats.model.ContratLocation;
import com.immobilier.gestionImmobiliere.donnees.contrats.repository.ContratLocationRepository;
import com.immobilier.gestionImmobiliere.donnees.paiements.repository.EcheanceLoyerRepository;
import com.immobilier.gestionImmobiliere.exceptions.ResourceNotFoundException;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.layout.element.Paragraph;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class AttestationLoyerDocumentService {

    private final ContratLocationRepository contratLocationRepository;
    private final EcheanceLoyerRepository echeanceLoyerRepository;

    public AttestationLoyerDocumentService(ContratLocationRepository contratLocationRepository,
                                           EcheanceLoyerRepository echeanceLoyerRepository) {
        this.contratLocationRepository = contratLocationRepository;
        this.echeanceLoyerRepository = echeanceLoyerRepository;
    }

    /**
     * Toujours régénérée à la demande, jamais stockée : reflète la situation
     * de paiement ACTUELLE, pas un instant figé dans le passé. Aucune écriture
     * en base, aucun envoi à MinIO — retourne directement les octets du PDF.
     */
    @PreAuthorize(
            "hasAnyRole('ADMIN','AGENT') " +
                    "or @contratLocationSecurity.isAccessible(#idContrat, authentication.principal.idUser)"
    )
    public byte[] genererAttestation(Integer idContrat) {
        ContratLocation contrat = contratLocationRepository.findById(idContrat)
                .orElseThrow(() -> new ResourceNotFoundException("contrat de location", idContrat));

        BigDecimal arrieres = echeanceLoyerRepository.sumArrieresParContrat(idContrat);
        if (arrieres == null) arrieres = BigDecimal.ZERO;

        return genererPdf(contrat, arrieres);
    }

    private byte[] genererPdf(ContratLocation contrat, BigDecimal arrieres) {
        PdfBuilder builder = new PdfBuilder("ATTESTATION DE LOYER");

        var maison = contrat.getMaison();
        var cour = maison.getCour();
        var locataire = contrat.getLocataire();
        var bailleur = cour.getProprietaire();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        builder.document.add(new Paragraph(
                "Je soussigné(e) " + bailleur.getNom() + " " + bailleur.getPrenom() +
                        ", bailleur, atteste que :")
                .setMarginTop(10));

        builder.document.add(new Paragraph(
                locataire.getNom() + " " + locataire.getPrenom() +
                        (contrat.getStatut().name().equals("ACTIF")
                                ? " est locataire depuis le " + contrat.getDateEntree().format(fmt)
                                : " a été locataire du " + contrat.getDateEntree().format(fmt) +
                                " au " + (contrat.getDateSortie() != null ? contrat.getDateSortie().format(fmt) : "—")) +
                        " du logement suivant : " + maison.getNomCommunMaison() +
                        ", situé dans la cour " + cour.getReferenceCour() +
                        ", moyennant un loyer mensuel de " + contrat.getMontantLoyer() + " FCFA.")
                .setMarginTop(15));

        String statutPaiement = arrieres.compareTo(BigDecimal.ZERO) > 0
                ? "présente à ce jour un solde impayé de " + arrieres + " FCFA."
                : "est à jour de ses paiements de loyer à la date de ce jour.";

        builder.document.add(new Paragraph("À la date de ce document, le/la locataire " + statutPaiement)
                .setFontColor(arrieres.compareTo(BigDecimal.ZERO) > 0 ? ColorConstants.RED : ColorConstants.DARK_GRAY)
                .setMarginTop(15));

        builder.document.add(new Paragraph(
                "Cette attestation est délivrée à la demande de l'intéressé(e) pour servir et valoir ce que de droit, " +
                        "à la date du " + LocalDate.now().format(fmt) + ".")
                .setFontSize(9).setMarginTop(25));

        return builder.genererEtFermer();
    }
}