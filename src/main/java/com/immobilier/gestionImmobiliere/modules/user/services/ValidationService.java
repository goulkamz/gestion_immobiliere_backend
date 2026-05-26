package com.immobilier.gestionImmobiliere.modules.user.services;

import com.immobilier.gestionImmobiliere.donnees.user.model.User;
import com.immobilier.gestionImmobiliere.donnees.user.model.Validation;
import com.immobilier.gestionImmobiliere.donnees.user.repository.ValidationRepository;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Random;

import static java.time.temporal.ChronoUnit.MINUTES;

@AllArgsConstructor
@Service
public class ValidationService {
    @Autowired
    private ValidationRepository validationaRepository;
    @Autowired
    private NotificationService notificationService;

    public void enregistrer(User user){
        Validation validation =new Validation();
        validation.setUser(user);
        Instant creation = Instant.now();
        validation.setCreation(creation);
        Instant expiration = creation.plus(10,MINUTES);
        validation.setExpiration(expiration);
        Random random =new Random();
        int randomInteger = random.nextInt(999999);
        String code = String.format("%06d",randomInteger);

        validation.setCode(code);
        validationaRepository.save(validation);
        notificationService.envoyer(validation);
    }

    public Validation lireEnFontionDuCode(String code){
        return this.validationaRepository.findByCode(code).orElseThrow(()-> new RuntimeException("votre code est invalide "));
    }

}
