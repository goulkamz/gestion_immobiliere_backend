package com.immobilier.gestionImmobiliere.modules.user;

import com.immobilier.gestionImmobiliere.support.AbstractIntegrationTest;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Inscription avec code d'activation et reinitialisation du mot de passe. Le serveur SMTP
 * est remplace par un faux JavaMailSender : on verifie chaque mail (destinataire, code).
 */
class InscriptionIntegrationTest extends AbstractIntegrationTest {

    private static final String EMAIL = "nouveau.client@gestimmo.test";

    @MockitoBean
    private JavaMailSender mailSender;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void fauxServeurMail() {
        when(mailSender.createMimeMessage()).thenAnswer(invocation -> new MimeMessage((Session) null));
    }

    @Test
    void inscription_activation_connexion_puisReinitialisationDuMotDePasse() throws Exception {
        appel("/api/auth/signup", inscription(EMAIL, "Secret@123a", "701234560"), status().isCreated());
        String code = jdbc.queryForObject("SELECT code FROM pending_registration WHERE email = ?", String.class, EMAIL);
        assertThat(dernierMail()).contains(EMAIL, code);

        appel("/api/auth/resend-code", "{\"email\":\"%s\"}".formatted(EMAIL), status().isOk());
        String nouveauCode = jdbc.queryForObject("SELECT code FROM pending_registration WHERE email = ?", String.class, EMAIL);
        assertThat(nouveauCode).isNotEqualTo(code);
        assertThat(dernierMail()).contains(nouveauCode);

        appel("/api/auth/activation", "{\"code\":\"%s\"}".formatted(code), status().isBadRequest());
        appel("/api/auth/activation", "{\"code\":\"%s\"}".formatted(nouveauCode), status().isOk());
        assertThat(jdbc.queryForObject("""
                SELECT r.libelle_role FROM users u JOIN role r ON r.id_role = u.id_role WHERE u.email = ?""",
                String.class, EMAIL)).isEqualTo("ROLE_CLIENT");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM pending_registration WHERE email = ?", Integer.class, EMAIL))
                .isZero();
        connexion(EMAIL, "Secret@123a", status().isOk());

        appel("/api/auth/forgot-password", "{\"email\":\"%s\"}".formatted(EMAIL), status().isAccepted());
        String jeton = jdbc.queryForObject(
                "SELECT token FROM password_reset_token WHERE email = ? AND used = FALSE", String.class, EMAIL);
        assertThat(dernierMail()).contains(jeton);

        appel("/api/auth/reset-password", "{\"code\":\"%s\",\"newPassword\":\"Nouveau@456b\"}".formatted(jeton),
                status().isAccepted());
        assertThat(dernierMail()).as("confirmation du changement").contains(EMAIL);
        appel("/api/auth/reset-password", "{\"code\":\"%s\",\"newPassword\":\"Autre@789c\"}".formatted(jeton),
                status().isBadRequest());

        connexion(EMAIL, "Secret@123a", status().isUnauthorized());
        connexion(EMAIL, "Nouveau@456b", status().isOk());
    }

    @Test
    void inscription_emailDejaUtilise_refusee() throws Exception {
        appel("/api/auth/signup", inscription("client@gestimmo.test", "Secret@123a", "701234570"), status().isConflict());
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void inscription_motDePasseFaible_refusee() throws Exception {
        appel("/api/auth/signup", inscription("faible@gestimmo.test", "motdepasse", "701234580"), status().isBadRequest());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM pending_registration WHERE email = 'faible@gestimmo.test'",
                Integer.class)).isZero();
    }

    @Test
    void motDePasseOublie_emailInconnu_memeReponseSansEnvoi() throws Exception {
        appel("/api/auth/forgot-password", "{\"email\":\"inconnu@gestimmo.test\"}", status().isAccepted());
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    private String inscription(String email, String motDePasse, String telephone) {
        return """
                {"nom":"Test","prenom":"Inscription","sexe":"M","email":"%s","password":"%s",
                 "dateNaissance":"1995-03-15","telephone":"%s","idRole":1}""".formatted(email, motDePasse, telephone);
    }

    private String dernierMail() throws Exception {
        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender, atLeastOnce()).send(captor.capture());
        List<MimeMessage> mails = captor.getAllValues();
        MimeMessage mail = mails.get(mails.size() - 1);
        clearInvocations(mailSender);
        ByteArrayOutputStream brut = new ByteArrayOutputStream();
        mail.writeTo(brut);
        return mail.getRecipients(Message.RecipientType.TO)[0] + "\n" + brut;
    }

    private void appel(String url, String json, ResultMatcher attendu) throws Exception {
        mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json)).andExpect(attendu);
    }

    private void connexion(String email, String motDePasse, ResultMatcher attendu) throws Exception {
        appel("/api/auth/signin", "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, motDePasse), attendu);
    }
}
