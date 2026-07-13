package com.immobilier.gestionImmobiliere.modules.medias.services;

import com.immobilier.gestionImmobiliere.exceptions.MediaStorageException;
import io.minio.*;
import io.minio.http.Method;
import jakarta.annotation.PostConstruct;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class FileStorageService {

    @Value("${app.minio.bucket}")
    private String bucket;

    private final MinioClient minioClient;
    private final MinioClient minioClientPresign;

    public FileStorageService(@Qualifier("minioClient") MinioClient minioClient,
                              @Qualifier("minioClientPresign") MinioClient minioClientPresign) {
        this.minioClient = minioClient;
        this.minioClientPresign = minioClientPresign;
    }

    @PostConstruct
    public void initBucket() {
        try {
            boolean existe = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!existe) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            }
        } catch (Exception e) {
            throw new MediaStorageException("Impossible d'initialiser le bucket MinIO", e);
        }
    }

    public String store(MultipartFile file, String sousDossier) {
        try {
            byte[] contenu = file.getBytes();
            String cle = construireCle(sousDossier, file.getOriginalFilename());

            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(cle)
                    .stream(new ByteArrayInputStream(contenu), contenu.length, -1)
                    .contentType(file.getContentType())
                    .build());

            return cle;
        } catch (Exception e) {
            throw new MediaStorageException("Impossible d'enregistrer le fichier sur MinIO", e);
        }
    }

    public String storeThumbnail(MultipartFile file, String sousDossier, int largeurMax) {
        try {
            byte[] contenu = file.getBytes();
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            Thumbnails.of(new ByteArrayInputStream(contenu))
                    .width(largeurMax)
                    .outputQuality(0.8)
                    .toOutputStream(buffer);

            byte[] miniature = buffer.toByteArray();
            String cle = construireCle(sousDossier + "/thumbnails", file.getOriginalFilename());

            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(cle)
                    .stream(new ByteArrayInputStream(miniature), miniature.length, -1)
                    .contentType(file.getContentType())
                    .build());

            return cle;
        } catch (Exception e) {
            throw new MediaStorageException("Impossible de générer la miniature sur MinIO", e);
        }
    }

    public void delete(String cle) {
        if (cle == null) return;
        try {
            minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(cle).build());
        } catch (Exception e) {
            // suppression best-effort
        }
    }

    /**
     * Génère une URL temporaire signée (valide 1h) permettant au client
     * d'accéder directement au fichier sur MinIO, sans passer par le backend.
     * Utilise le client "public" pour que l'URL générée soit atteignable
     * depuis le navigateur, pas seulement depuis le réseau Docker interne.
     */
    public String genererUrlPresignee(String cle) {
        if (cle == null) return null;
        try {
            return minioClientPresign.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucket)
                    .object(cle)
                    .expiry(1, TimeUnit.HOURS)
                    .build());
        } catch (Exception e) {
            throw new MediaStorageException("Impossible de générer l'URL présignée", e);
        }
    }

    private String construireCle(String sousDossier, String nomOriginal) {
        String extension = extraireExtension(nomOriginal);
        return sousDossier + "/" + UUID.randomUUID() + extension;
    }

    private String extraireExtension(String nomOriginal) {
        if (nomOriginal == null || !nomOriginal.contains(".")) return "";
        return nomOriginal.substring(nomOriginal.lastIndexOf('.'));
    }
}