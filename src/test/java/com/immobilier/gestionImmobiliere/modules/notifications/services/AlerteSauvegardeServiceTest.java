package com.immobilier.gestionImmobiliere.modules.notifications.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/** Decision de la supervision des sauvegardes : tout cas anormal produit un probleme, un etat sain n'en produit aucun. */
class AlerteSauvegardeServiceTest {

    private static final Instant MAINTENANT = Instant.parse("2026-11-02T08:00:00Z");

    @TempDir
    Path dossier;

    private Optional<AlerteSauvegardeService.Probleme> avecStatut(String contenu) throws IOException {
        Path fichier = dossier.resolve(AlerteSauvegardeService.FICHIER_STATUT);
        Files.writeString(fichier, contenu);
        return AlerteSauvegardeService.evaluer(fichier, MAINTENANT, 26);
    }

    @Test
    void sauvegardeRecenteEtReussie_neProduitAucuneAlerte() throws IOException {
        assertThat(avecStatut("OK 2026-11-02T02:00:05+00:00 /backups/postgres/gestion_immobiliere_20261102_020000.dump")).isEmpty();
    }

    @Test
    void sauvegardeEnEchec_estSignalee() throws IOException {
        Optional<AlerteSauvegardeService.Probleme> p = avecStatut("ECHEC 2026-11-02T02:00:05+00:00 /backups/postgres/x.dump");
        assertThat(p).isPresent();
        assertThat(p.get().code()).isEqualTo("ECHEC");
    }

    @Test
    void sauvegardeReussieMaisTropAncienne_estEnRetard() throws IOException {
        // 29 h 54 avant « maintenant » (29 heures entières), limite de 26 heures
        Optional<AlerteSauvegardeService.Probleme> p = avecStatut("OK 2026-11-01T02:00:05+00:00 /backups/postgres/x.dump");
        assertThat(p).isPresent();
        assertThat(p.get().code()).isEqualTo("RETARD");
        assertThat(p.get().message()).contains("29 heures");
    }

    @Test
    void sauvegardeJusteSousLaLimite_neProduitPasDAlerte() throws IOException {
        assertThat(avecStatut("OK 2026-11-01T07:00:00+00:00 /backups/postgres/x.dump")).isEmpty(); // 25 heures
    }

    @Test
    void fichierDEtatAbsent_estSignale() {
        Optional<AlerteSauvegardeService.Probleme> p =
                AlerteSauvegardeService.evaluer(dossier.resolve("inexistant.txt"), MAINTENANT, 26);
        assertThat(p).isPresent();
        assertThat(p.get().code()).isEqualTo("ABSENT");
    }

    @Test
    void fichierDEtatIllisible_estSignale() throws IOException {
        assertThat(avecStatut("n'importe quoi").map(AlerteSauvegardeService.Probleme::code)).contains("ILLISIBLE");
        assertThat(avecStatut("").map(AlerteSauvegardeService.Probleme::code)).contains("ILLISIBLE");
        assertThat(avecStatut("OK").map(AlerteSauvegardeService.Probleme::code)).contains("ILLISIBLE");
    }
}
