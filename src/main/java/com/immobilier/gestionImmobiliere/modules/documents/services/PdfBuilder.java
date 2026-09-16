package com.immobilier.gestionImmobiliere.modules.documents.services;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.TextAlignment;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Squelette PDF commun : en-tête (nom agence) + pied de page (date de génération),
 * pour garder une identité visuelle cohérente sur tous les documents émis.
 */
public class PdfBuilder {

    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    private final PdfDocument pdfDocument;
    protected final Document document;

    public PdfBuilder(String titreDocument) {
        PdfWriter writer = new PdfWriter(buffer);
        this.pdfDocument = new PdfDocument(writer);
        this.pdfDocument.setDefaultPageSize(PageSize.A4);
        this.document = new Document(pdfDocument);

        document.add(new Paragraph("Gestion Immobilière")
                .setBold().setFontSize(16).setTextAlignment(TextAlignment.CENTER));
        document.add(new Paragraph(titreDocument)
                .setFontSize(13).setTextAlignment(TextAlignment.CENTER).setMarginBottom(20));
    }

    public byte[] genererEtFermer() {
        String dateGeneration = "Généré le " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        document.add(new Paragraph(dateGeneration)
                .setFontSize(8).setFontColor(ColorConstants.GRAY).setTextAlignment(TextAlignment.RIGHT).setMarginTop(30));

        document.close();
        return buffer.toByteArray();
    }
}