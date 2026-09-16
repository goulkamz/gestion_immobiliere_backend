package com.immobilier.gestionImmobiliere.modules.documents.services;

import com.immobilier.gestionImmobiliere.exceptions.MediaStorageException;
import io.minio.*;
import io.minio.http.Method;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class DocumentStorageService {

    @Value("${app.minio.bucket-documents}")
    private String bucket;

    private final MinioClient minioClient;
    private final MinioClient minioClientPresign;

    public DocumentStorageService(@Qualifier("minioClient") MinioClient minioClient,
                                  @Qualifier("minioClientPresign") MinioClient minioClientPresign) {
        this.minioClient = minioClient;
        this.minioClientPresign = minioClientPresign;
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
     * Toujours présignée — un document est par nature privé, jamais public
     * (contrairement aux médias catalogue/annonces).
     */
    public String genererUrlPresignee(String cle) {
        try {
            return minioClientPresign.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucket)
                    .object(cle)
                    .expiry(1, TimeUnit.HOURS)
                    .build());
        } catch (Exception e) {
            throw new MediaStorageException("Impossible de générer l'URL présignée du document", e);
        }
    }
}