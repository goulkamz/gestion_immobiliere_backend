package com.immobilier.gestionImmobiliere.modules.documents.services;

import com.immobilier.gestionImmobiliere.exceptions.MediaStorageException;
import io.minio.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.UUID;

@Service
public class DocumentStorageService {

    @Value("${app.minio.bucket-documents}")
    private String bucket;

    private final MinioClient minioClient;

    public DocumentStorageService(@Qualifier("minioClient") MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    public String store(byte[] pdfBytes, String sousDossier) {
        try {
            String cle = sousDossier + "/" + UUID.randomUUID() + ".pdf";
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(cle)
                    .stream(new ByteArrayInputStream(pdfBytes), pdfBytes.length, -1)
                    .contentType("application/pdf")
                    .build());
            return cle;
        } catch (Exception e) {
            throw new MediaStorageException("Impossible d'enregistrer le document sur MinIO", e);
        }
    }

    /**
     * Télécharge les octets du document depuis MinIO — l'autorisation est
     * vérifiée par le service appelant (via @PreAuthorize) à chaque requête,
     * contrairement à une URL présignée valable pour une fenêtre de temps.
     */
    public byte[] telecharger(String cle) {
        try (var stream = minioClient.getObject(GetObjectArgs.builder()
                .bucket(bucket).object(cle).build())) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            stream.transferTo(buffer);
            return buffer.toByteArray();
        } catch (Exception e) {
            throw new MediaStorageException("Impossible de télécharger le document depuis MinIO", e);
        }
    }
}