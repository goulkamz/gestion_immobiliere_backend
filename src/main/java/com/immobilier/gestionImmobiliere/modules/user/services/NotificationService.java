package com.immobilier.gestionImmobiliere.modules.user.services;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class NotificationService {

    private final JavaMailSender javaMailSender;

    @Value("${app.base-url}")
    private String baseUrl;

    private void sendEmail(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("goulkamz@gmail.com");
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        javaMailSender.send(message);
    }

    public void envoyerCodeActivation(String email, String nom, String code){
        String subject = "Votre code d'activation";
        String texte =String.format("Bonjour %s , votre code d'activation est %s ; A bientot!",
                nom,
                code);
        sendEmail(email,subject,texte);
    }

    public void envoyerCodeReinitialisation(String email, String nom, String token) {
        String subject = "Réinitialisation de votre mot de passe";
        String resetLink = baseUrl + "/reset-password?token=" + token;
        String body = String.format("""
        Bonjour %s,
        Vous avez demandé la réinitialisation de votre mot de passe.  
        Voici votre code de réinitialisation : %s
        🔗 LIEN DE RÉINITIALISATION :
        %s
        Ce code est valable 15 minutes.
        CONSEIL DE SÉCURITÉ :
        - Ne partagez ce code avec personne
        - L'équipe ne vous demandera jamais ce code
        Si vous n'êtes pas à l'origine de cette demande, ignorez cet email.
        Cordialement,
        L'équipe de gestion immobilière
        """, nom, token,resetLink);

        sendEmail(email, subject, body);
    }

    public void envoyerConfirmationMotDePasseModifie(String email, String nom) {
        String subject = "Votre mot de passe a été modifié";
        String body = String.format("""
        Bonjour %s,
        Votre mot de passe a été modifié avec succès.
        Si vous n'êtes pas à l'origine de cette modification, veuillez nous contacter immédiatement.
        Cordialement,
        L'équipe de gestion immobilière
        """, nom);

        sendEmail(email, subject, body);
    }

}