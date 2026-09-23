package com.immobilier.gestionImmobiliere.modules.user.services;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class NotificationService {

    private final JavaMailSender javaMailSender;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${spring.mail.from}")
    private String mailFrom;

    // Couleurs alignées sur Utils (PDF) pour une identité visuelle cohérente
    private static final String COULEUR_ENTETE = "#1f3864"; // COULEUR_ENTETE (31,56,100)
    private static final String COULEUR_ACCENT = "#ce1126"; // COULEUR_ACCENT (206,17,38)
    private static final String COULEUR_VERT   = "#007a3d"; // COULEUR_VERT (0,122,61)

    // ── Envoi HTML avec fallback texte ────────────────────────────────────────

    private void envoyerEmailHtml(String to, String subject, String htmlBody, String textBody) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(mailFrom);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(textBody, htmlBody);
            javaMailSender.send(message);
        } catch (MessagingException e) {
            SimpleMailMessage fallback = new SimpleMailMessage();
            fallback.setFrom(mailFrom);
            fallback.setTo(to);
            fallback.setSubject(subject);
            fallback.setText(textBody);
            javaMailSender.send(fallback);
        }
    }

    // ── Template HTML commun (bannière + pied de page) ────────────────────────

    private String getTemplateHtml(String contenu, String couleurAccent) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <style>
                        body { font-family: 'Segoe UI', Arial, sans-serif; margin: 0; padding: 0; background-color: #f4f7f6; }
                        .container { max-width: 600px; margin: 20px auto; background: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.1); }
                        .header { background: %s; padding: 20px 30px; text-align: center; color: white; border-bottom: 6px solid #fcd116; }
                        .header h1 { margin: 0; font-size: 26px; font-weight: 700; letter-spacing: 1px; color: white }
                        .header .sub { font-size: 15px; opacity: 0.9; margin-top: 5px; }
                        .content { padding: 30px 30px 20px; color: #1e2a3a; line-height: 1.6; }
                        .highlight { background-color: #f4f7fb; padding: 15px 20px; border-left: 5px solid %s; border-radius: 6px; margin: 20px 0; }
                        .code-box { background: #1e2a3a; color: white; padding: 15px; border-radius: 8px; font-size: 30px; font-weight: bold; text-align: center; letter-spacing: 6px; margin: 20px 0; }
                        .btn { display: inline-block; background: %s; color: white; padding: 12px 28px; text-decoration: none; border-radius: 50px; font-weight: 600; margin: 15px 0; }
                        .footer { background: #eef2f0; padding: 18px 30px; text-align: center; font-size: 13px; color: #4a5a5a; border-top: 1px solid #d0dbd6; }
                        .footer a { color: %s; text-decoration: none; }
                        .security-tip { background: #fff3e0; padding: 12px 18px; border-radius: 6px; border-left: 4px solid #1f3864; margin: 20px 0; font-size: 14px; }
                        .divider { border-top: 2px dashed #d0dbd6; margin: 25px 0; }
              
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1 style="margin:0; font-size:26px; font-weight:700; letter-spacing:1px; color:#ffffff !important;">GI.BF</h1>
                            <div class="sub">Agence Générale Immobilière · Burkina Faso</div>
                        </div>
                        <div class="content">
                            %s
                            <div class="divider"></div>
                            <p style="font-size:13px; color:#5a6b6b; text-align: center"><em>« Chaque personne merite la paix! »</em></p>
                        </div>
                        <div class="footer">
                            Agence Générale Immobilière GI.BF · Ouagadougou<br>
                            <a href="%s">%s</a>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(COULEUR_ENTETE, COULEUR_ENTETE, COULEUR_VERT, couleurAccent, contenu, baseUrl, baseUrl);
    }

    // ── Notifications ──────────────────────────────────────────────────────────

    public void envoyerCodeActivation(String email, String nom, String code) {
        String subject = "Votre code d'activation GI.BF";
        String contenuHtml = """
                <h2>Bienvenue sur GI.BF, %s !</h2>
                <p>Vous êtes sur le point de rejoindre la plateforme de gestion immobilière <strong>GI.BF</strong>.</p>
                <p>Voici votre code d'activation :</p>
                <div class="code-box">%s</div>
                <p>Ce code est valable <strong>10 minutes</strong>.</p>
                <div class="security-tip">
                    ⚠️ <strong>Conseil de sécurité :</strong> ne communiquez jamais ce code à quiconque. L'équipe GI.BF ne vous le demandera jamais par téléphone ou par message.
                </div>
                """.formatted(nom, code);

        String textePlain = """
                Bonjour %s,
                Bienvenue sur GI.BF, la plateforme de gestion immobilière.
                Votre code d'activation est : %s
                Il est valable 10 minutes. Ne le partagez pas.
                """.formatted(nom, code);

        envoyerEmailHtml(email, subject, getTemplateHtml(contenuHtml, COULEUR_ENTETE), textePlain);
    }

    public void envoyerCodeReinitialisation(String email, String nom, String token) {
        String subject = "Réinitialisation de votre mot de passe GI.BF";
        String resetLink = baseUrl + "/reset-password?token=" + token;
        String contenuHtml = """
                <h2>Bonjour %s,</h2>
                <p>Vous avez demandé la réinitialisation de votre mot de passe sur GI.BF.</p>
                <div class="highlight">
                    <p><strong>🔐 Votre code de réinitialisation :</strong></p>
                    <div class="code-box">%s</div>
                    <p>Valable 15 minutes.</p>
                </div>
                <p>Ou cliquez directement sur le bouton ci-dessous :</p>
                <p style="text-align:center;">
                    <a href="%s" class="btn">Réinitialiser mon mot de passe</a>
                </p>
                <div class="security-tip">
                    ⚠️ <strong>Rappel de sécurité :</strong><br>
                    • Ne partagez ce code avec personne.<br>
                    • L'équipe GI.BF ne vous demandera jamais ce code.<br>
                    • Si vous n'êtes pas à l'origine de cette demande, ignorez cet email.
                </div>
                """.formatted(nom, token, resetLink);

        String textePlain = """
                Bonjour %s,
                Vous avez demandé la réinitialisation de votre mot de passe.
                Votre code est : %s
                Lien : %s
                Ce code est valable 15 minutes.
                """.formatted(nom, token, resetLink);

        envoyerEmailHtml(email, subject, getTemplateHtml(contenuHtml, COULEUR_ENTETE), textePlain);
    }

    public void envoyerConfirmationMotDePasseModifie(String email, String nom) {
        String subject = "Votre mot de passe GI.BF a été modifié";
        String contenuHtml = """
                <h2>Bonjour %s,</h2>
                <p>Ce message vous confirme que votre mot de passe pour la plateforme <strong>GI.BF</strong> a bien été modifié.</p>
                <div class="highlight" style="border-left-color:#1f3864;">
                    <p>✔️ Si vous avez effectué cette opération, aucune action n'est requise.</p>
                </div>
                <div class="security-tip">
                    ⚠️ Si vous ne reconnaissez pas cette modification, contactez-nous immédiatement.
                </div>
                """.formatted(nom);

        String textePlain = """
                Bonjour %s,
                Votre mot de passe pour GI.BF a été modifié avec succès.
                Si vous n'êtes pas à l'origine de cette modification, veuillez nous contacter immédiatement.
                Cordialement,
                L'équipe GI.BF
                """.formatted(nom);

        envoyerEmailHtml(email, subject, getTemplateHtml(contenuHtml, COULEUR_ENTETE), textePlain);
    }
}