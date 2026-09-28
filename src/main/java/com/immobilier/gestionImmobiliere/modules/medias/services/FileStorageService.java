package com.immobilier.gestionImmobiliere.modules.medias.services;

import com.immobilier.gestionImmobiliere.donnees.medias.model.TypeEntiteMedia;
import com.immobilier.gestionImmobiliere.exceptions.MediaStorageException;
import io.minio.*;
import jakarta.annotation.PostConstruct;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Set<TypeEntiteMedia> ENTITES_PUBLIQUES = Set.of(
            TypeEntiteMedia.COUR, TypeEntiteMedia.ANNONCE, TypeEntiteMedia.MAISON
    );

    @Value("${app.minio.bucket-medias-public}")
    private String bucketPublic;

    @Value("${app.minio.bucket-medias-prive}")
    private String bucketPrive;

    @Value("${app.minio.bucket-documents}")
    private String bucketDocuments;

    private final MinioClient minioClient;
    private final Tika tika = new Tika();

    public FileStorageService(@Qualifier("minioClient") MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    @PostConstruct
    public void initBuckets() {
        try {
            creerBucketSiAbsent(bucketPublic);
            appliquerPolitiqueLecturePublique(bucketPublic);
            creerBucketSiAbsent(bucketPrive);
            creerBucketSiAbsent(bucketDocuments);
        } catch (Exception e) {
            throw new MediaStorageException("Impossible d'initialiser les buckets MinIO", e);
        }
    }

    private void creerBucketSiAbsent(String bucket) throws Exception {
        boolean existe = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        if (!existe) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        }
    }

    private void appliquerPolitiqueLecturePublique(String bucket) throws Exception {
        String policy = """
                {
                  "Version": "2012-10-17",
                  "Statement": [
                    {
                      "Effect": "Allow",
                      "Principal": {"AWS": ["*"]},
                      "Action": ["s3:GetObject"],
                      "Resource": ["arn:aws:s3:::%s/*"]
                    }
                  ]
                }
                """.formatted(bucket);
        minioClient.setBucketPolicy(SetBucketPolicyArgs.builder().bucket(bucket).config(policy).build());
    }

    public boolean estPublic(TypeEntiteMedia type) {
        return ENTITES_PUBLIQUES.contains(type);
    }

    public String bucketPour(TypeEntiteMedia type) {
        return estPublic(type) ? bucketPublic : bucketPrive;
    }

    public String detecterTypeReel(MultipartFile fichier) {
        try {
            return tika.detect(fichier.getInputStream());
        } catch (IOException e) {
            throw new MediaStorageException("Impossible de détecter le type réel du fichier", e);
        }
    }

    /**
     * @param typeReel type MIME reellement detecte (Tika), jamais celui declare par le
     *                 client (falsifiable) — determinant sur le bucket public en lecture
     *                 anonyme, ou un Content-Type mensonger servirait un fichier comme HTML.
     */
    public String store(MultipartFile file, String bucket, String sousDossier, String typeReel) {
        try {
            byte[] contenu = file.getBytes();
            String cle = construireCle(sousDossier, file.getOriginalFilename());

            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(cle)
                    .stream(new ByteArrayInputStream(contenu), contenu.length, -1)
                    .contentType(typeReel)
                    .build());

            return cle;
        } catch (Exception e) {
            throw new MediaStorageException("Impossible d'enregistrer le fichier sur MinIO", e);
        }
    }

    public String storeThumbnail(MultipartFile file, String bucket, String sousDossier, int largeurMax, String typeReel) {
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
                    .contentType(typeReel)
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
            throw new MediaStorageException("Échec suppression MinIO : " + cle, e);
        }
    }

    public String storeVideoThumbnailFromFile(Path fichier, String bucket, String sousDossier) {
        try {
            long taille = Files.size(fichier);
            String cle = construireCle(sousDossier + "/thumbnails", "frame.jpg");

            try (var in = Files.newInputStream(fichier)) {
                minioClient.putObject(PutObjectArgs.builder()
                        .bucket(bucket)
                        .object(cle)
                        .stream(in, taille, -1)
                        .contentType("image/jpeg")
                        .build());
            }
            return cle;
        } catch (Exception e) {
            throw new MediaStorageException("Impossible d'uploader la miniature vidéo", e);
        }
    }

    public void downloadToFile(String bucket, String cle, Path destination) {
        try (var stream = minioClient.getObject(GetObjectArgs.builder()
                .bucket(bucket).object(cle).build())) {
            Files.copy(stream, destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            throw new MediaStorageException("Échec téléchargement depuis MinIO", e);
        }
    }

    public record FichierStocke(InputStream contenu, String contentType, long taille) {}

    /** Flux a fermer par l'appelant (Spring le ferme apres ecriture de la reponse). */
    public FichierStocke lire(String bucket, String cle) {
        try {
            StatObjectResponse infos = minioClient.statObject(StatObjectArgs.builder()
                    .bucket(bucket).object(cle).build());
            InputStream contenu = minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucket).object(cle).build());
            return new FichierStocke(contenu, infos.contentType(), infos.size());
        } catch (Exception e) {
            throw new MediaStorageException("Impossible de lire le fichier depuis MinIO", e);
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

    public String getBucketMediasPublic() { return bucketPublic; }
    public String getBucketMediasPrive() { return bucketPrive; }
    public String getBucketDocuments() { return bucketDocuments; }
}