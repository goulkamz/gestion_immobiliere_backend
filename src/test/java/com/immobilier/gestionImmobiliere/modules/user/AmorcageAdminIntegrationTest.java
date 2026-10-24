package com.immobilier.gestionImmobiliere.modules.user;

import com.immobilier.gestionImmobiliere.modules.user.services.AmorcageAdminService;
import com.immobilier.gestionImmobiliere.support.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Premier admin en production : cree depuis les variables d'environnement seulement s'il n'existe aucun admin.
 * La base de test est partagee entre les classes : les admins du seed sont mis de cote le temps du test,
 * puis remis en etat.
 */
class AmorcageAdminIntegrationTest extends AbstractIntegrationTest {

    private static final String EMAIL = "premier.admin@exemple.test";
    private static final String MOT_DE_PASSE = "Admin-Initial#2026";

    @Autowired
    private AmorcageAdminService amorcage;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private List<Integer> adminsDuSeed;

    @BeforeEach
    void mettreDeCoteLesAdmins() {
        adminsDuSeed = jdbc.queryForList("""
                SELECT id_user FROM users
                WHERE is_deleted = false AND id_role = (SELECT id_role FROM role WHERE libelle_role = 'ROLE_ADMIN')""",
                Integer.class);
        adminsDuSeed.forEach(id -> jdbc.update("UPDATE users SET is_deleted = true WHERE id_user = ?", id));
    }

    @AfterEach
    void remettreEnEtat() {
        jdbc.update("DELETE FROM users WHERE email = ?", EMAIL);
        adminsDuSeed.forEach(id -> jdbc.update("UPDATE users SET is_deleted = false WHERE id_user = ?", id));
    }

    @Test
    void sansAdmin_creeLeCompteAvecUnMotDePasseChiffre_puisNeFaitPlusRien() {
        assertThat(amorcage.amorcer(" Premier.Admin@Exemple.test ", MOT_DE_PASSE, "Dupont", "Awa")).isTrue();

        var admin = jdbc.queryForMap("""
                SELECT u.mot_de_passe, u.flag_actif, u.is_deleted, r.libelle_role
                FROM users u JOIN role r ON r.id_role = u.id_role WHERE u.email = ?""", EMAIL);
        assertThat(admin.get("libelle_role")).isEqualTo("ROLE_ADMIN");
        assertThat(admin.get("flag_actif")).isEqualTo(true);
        assertThat(admin.get("is_deleted")).isEqualTo(false);
        assertThat((String) admin.get("mot_de_passe")).isNotEqualTo(MOT_DE_PASSE);
        assertThat(passwordEncoder.matches(MOT_DE_PASSE, (String) admin.get("mot_de_passe"))).isTrue();

        // Un admin existe maintenant : un second appel ne cree rien
        assertThat(amorcage.amorcer("autre.admin@exemple.test", MOT_DE_PASSE, "X", "Y")).isFalse();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM users WHERE email = ?", Integer.class,
                "autre.admin@exemple.test")).isZero();
    }

    @Test
    void avecUnAdminActif_neCreeRien() {
        jdbc.update("UPDATE users SET is_deleted = false WHERE id_user = ?", adminsDuSeed.get(0));

        assertThat(amorcage.amorcer(EMAIL, MOT_DE_PASSE, "Dupont", "Awa")).isFalse();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM users WHERE email = ?", Integer.class, EMAIL)).isZero();
    }

    @Test
    void variablesAbsentesOuInvalides_neCreentRien() {
        assertThat(amorcage.amorcer("", MOT_DE_PASSE, "D", "A")).isFalse();
        assertThat(amorcage.amorcer(EMAIL, "", "D", "A")).isFalse();
        assertThat(amorcage.amorcer("pas-un-email", MOT_DE_PASSE, "D", "A")).isFalse();
        // trop court, sans special, sans majuscule, avec un espace
        assertThat(amorcage.amorcer(EMAIL, "Court#1a", "D", "A")).isFalse();
        assertThat(amorcage.amorcer(EMAIL, "SansSpecial2026Abc", "D", "A")).isFalse();
        assertThat(amorcage.amorcer(EMAIL, "sans-majuscule#2026", "D", "A")).isFalse();
        assertThat(amorcage.amorcer(EMAIL, "Avec Espace#2026x", "D", "A")).isFalse();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM users WHERE email = ?", Integer.class, EMAIL)).isZero();
    }

    @Test
    void emailDejaUtiliseParUnAutreCompte_neCreeRien() {
        assertThat(amorcage.amorcer("agent@gestimmo.test", MOT_DE_PASSE, "D", "A")).isFalse();
        assertThat(jdbc.queryForObject("""
                SELECT r.libelle_role FROM users u JOIN role r ON r.id_role = u.id_role WHERE u.email = ?""",
                String.class, "agent@gestimmo.test")).isEqualTo("ROLE_AGENT");
    }
}
