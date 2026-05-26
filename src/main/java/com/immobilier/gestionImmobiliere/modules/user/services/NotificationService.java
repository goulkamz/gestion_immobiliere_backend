package com.immobilier.gestionImmobiliere.modules.user.services;

import com.immobilier.gestionImmobiliere.donnees.user.model.Validation;
import lombok.AllArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Service
public class NotificationService {
    JavaMailSender javaMailSender;
    public void envoyer (Validation validation){
        SimpleMailMessage message= new SimpleMailMessage();
        message.setFrom("goulkamz@gmail.com");
        message.setTo(validation.getUser().getEmail());
        message.setSubject("Votre code d'activation");
        String texte =String.format("Bonjour %s , votre code d'activation est %s ; A bientot!",
                validation.getUser().getNom(),
                validation.getCode());
        message.setText(texte);
        javaMailSender.send(message);

    }
}