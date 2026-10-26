package com.immobilier.gestionImmobiliere.support;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;

/**
 * Base des tests d'integration : demarre les memes conteneurs que docker-compose.yml
 * (Postgres dont le schema et le seed viennent de Flyway, Redis, MinIO).
 * Conteneurs singleton (demarres une fois par JVM, arretes par Ryuk) : avec des
 * @Container par classe, le contexte Spring mis en cache pointerait vers des
 * conteneurs deja arretes des la deuxieme classe de test.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    protected static final String MOT_DE_PASSE_SEED = "Password123!";

    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17")
            .withDatabaseName("gestion_immobiliere")
            .withUsername("postgres")
            .withPassword("test");

    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    static final GenericContainer<?> MINIO = new GenericContainer<>("minio/minio:latest")
            .withExposedPorts(9000)
            .withEnv("MINIO_ROOT_USER", "minioadmin")
            .withEnv("MINIO_ROOT_PASSWORD", "minioadmin123")
            .withCommand("server", "/data")
            .waitingFor(Wait.forHttp("/minio/health/live").forPort(9000));

    static {
        POSTGRES.start();
        REDIS.start();
        MINIO.start();
    }

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    // Tous les appels MockMvc viennent de 127.0.0.1 : sans remise a zero, les compteurs
    // de rate limiting (login par IP) s'accumulent d'un test a l'autre -> 429.
    @BeforeEach
    void viderRedis() {
        stringRedisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
    }

    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        // Flyway : schema + fixtures de test (db/testdata, ancien jeu de donnees avec contrats et mandats) dont les tests ont besoin
        registry.add("spring.flyway.locations", () -> "classpath:db/migration,classpath:db/testdata");

        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));

        String minioEndpoint = "http://" + MINIO.getHost() + ":" + MINIO.getMappedPort(9000);
        registry.add("app.minio.endpoint", () -> minioEndpoint);
    }
}
