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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class FileStorageService {

    @Value("${app.minio.bucket-medias}")
    private String bucketMedias;

    @Value("${app.minio.bucket-documents}")
    private String bucketDocuments;

    @Value("${app.medias.max-size-video-mo:50}")
    private int tailleMaxVideoMo;

    private final MinioClient minioClient;
    private final MinioClient minioClientPresign;

    public FileStorageService(@Qualifier("minioClient") MinioClient minioClient,
                              @Qualifier("minioClientPresign") MinioClient minioClientPresign) {
        this.minioClient = minioClient;
        this.minioClientPresign = minioClientPresign;
    }

    /**
     * Crée tous les buckets déclarés au démarrage, s'ils n'existent pas déjà.
     */
    @PostConstruct
    public void initBuckets() {
        for (String bucket : List.of(bucketMedias, bucketDocuments)) {
            try {
                boolean existe = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
                if (!existe) {
                    minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                }
            } catch (Exception e) {
                throw new MediaStorageException("Impossible d'initialiser le bucket MinIO : " + bucket, e);
            }
        }
    }

    /**
     * Stocke un fichier dans le bucket et le sous-dossier (préfixe de clé) donnés.
     * Ex: store(file, "medias-immobilier", "images/cours") -> "images/cours/uuid.jpg"
     */
    public String store(MultipartFile file, String bucket, String sousDossier) {
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

    public String storeThumbnail(MultipartFile file, String bucket, String sousDossier, int largeurMax) {
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

    public void delete(String bucket, String cle) {
        if (cle == null) return;
        try {
            minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(cle).build());
        } catch (Exception e) {
            // suppression best-effort
        }
    }

    // Ajouts dans FileStorageService

    /**
     * Génère une miniature vidéo en extrayant une frame à la 1ère seconde via FFmpeg.
     * ⚠️ Opération bloquante et synchrone — pour un vrai volume de production,
     * ce traitement devrait être déporté en tâche asynchrone (queue + worker),
     * pas exécuté dans le thread de la requête HTTP.
     */
    public String storeVideoThumbnailFromBytes(byte[] videoBytes, String extension, String bucket, String sousDossier) {
        Path tempVideo = null;
        Path tempFrame = null;
        try {
            tempVideo = Files.createTempFile("video-", extension);
            Files.write(tempVideo, videoBytes);

            tempFrame = Files.createTempFile("frame-", ".jpg");

            ProcessBuilder pb = new ProcessBuilder(
                    "ffmpeg", "-y",
                    "-i", tempVideo.toString(),
                    "-ss", "00:00:01",
                    "-vframes", "1",
                    "-vf", "scale=400:-1",
                    tempFrame.toString()
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (var reader = process.getInputStream()) {
                reader.readAllBytes();
            }

            boolean termine = process.waitFor(30, java.util.concurrent.TimeUnit.SECONDS);
            if (!termine || process.exitValue() != 0) {
                throw new MediaStorageException("Échec extraction frame vidéo (ffmpeg)", null);
            }

            byte[] miniature = Files.readAllBytes(tempFrame);
            String cle = construireCle(sousDossier + "/thumbnails", "frame.jpg");

            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(cle)
                    .stream(new ByteArrayInputStream(miniature), miniature.length, -1)
                    .contentType("image/jpeg")
                    .build());

            return cle;
        } catch (Exception e) {
            throw new MediaStorageException("Impossible de générer la miniature vidéo", e);
        } finally {
            supprimerSiExiste(tempVideo);
            supprimerSiExiste(tempFrame);
        }
    }

    private void supprimerSiExiste(Path chemin) {
        if (chemin == null) return;
        try {
            Files.deleteIfExists(chemin);
        } catch (java.io.IOException ignored) { }
    }

    /**
     * Génère une URL temporaire signée (valide 1h) permettant au client
     * d'accéder directement au fichier sur MinIO, sans passer par le backend.
     * Utilise le client "public" pour que l'URL générée soit atteignable
     * depuis le navigateur, pas seulement depuis le réseau Docker interne.
     */
    public String genererUrlPresignee(String bucket, String cle) {
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

    public String extraireExtension(String nomOriginal) {
        if (nomOriginal == null || !nomOriginal.contains(".")) return "";
        return nomOriginal.substring(nomOriginal.lastIndexOf('.'));
    }

    // Getters exposant les noms de buckets aux services appelants
    public String getBucketMedias() { return bucketMedias; }
    public String getBucketDocuments() { return bucketDocuments; }
}