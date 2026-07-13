package com.immobilier.gestionImmobiliere.configurations;


import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioConfig {

    @Value("${app.minio.endpoint}")
    private String endpoint;

    @Value("${app.minio.public-endpoint}")
    private String publicEndpoint;

    @Value("${app.minio.access-key}")
    private String accessKey;

    @Value("${app.minio.secret-key}")
    private String secretKey;

    /**
     * Client interne — utilisé par le backend pour uploader/supprimer les fichiers.
     * Endpoint résolu sur le réseau Docker interne (ex: http://minio:9000).
     */
    @Bean("minioClient")
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }

    /**
     * Client dédié à la signature d'URL — l'endpoint doit être atteignable
     * depuis le navigateur du client final (ex: http://localhost:9000 en dev,
     * un domaine public en prod).
     */
    @Bean("minioClientPresign")
    public MinioClient minioClientPresign() {
        return MinioClient.builder()
                .endpoint(publicEndpoint)
                .credentials(accessKey, secretKey)
                .build();
    }
}