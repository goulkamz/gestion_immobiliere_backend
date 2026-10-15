package com.immobilier.gestionImmobiliere.modules.contrats.services;

import com.immobilier.gestionImmobiliere.donnees.contrats.model.Avenant;
import com.immobilier.gestionImmobiliere.donnees.contrats.model.AvenantLigne;
import com.immobilier.gestionImmobiliere.donnees.contrats.model.ContratLocation;
import com.immobilier.gestionImmobiliere.donnees.contrats.model.ContratMandat;
import com.immobilier.gestionImmobiliere.donnees.contrats.repository.AvenantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Enregistre les avenants des contrats actifs. Doit etre appele dans la transaction de la
 * modification du contrat : l'avenant et la modification sont enregistres ensemble ou pas du tout.
 */
@Service
public class AvenantService {

    /** Un champ modifie : nom technique, ancienne et nouvelle valeur (deja converties en texte). */
    public record Modification(String champ, String ancienneValeur, String nouvelleValeur) {}

    private final AvenantRepository avenantRepository;

    public AvenantService(AvenantRepository avenantRepository) {
        this.avenantRepository = avenantRepository;
    }

    /**
     * Controle la saisie d'un avenant et renvoie la date d'effet retenue (aujourd'hui par defaut).
     * Le motif est obligatoire, le client doit avoir accepte la modification, et la date d'effet
     * ne peut pas etre dans le passe (on ne modifie pas des periodes deja facturees).
     */
    public LocalDate verifierSaisie(String motif, Boolean accordClient, LocalDate dateEffet) {
        if (motif == null || motif.isBlank()) {
            throw new IllegalArgumentException("Le motif de la modification est obligatoire");
        }
        if (!Boolean.TRUE.equals(accordClient)) {
            throw new IllegalArgumentException("La modification d'un contrat actif exige l'accord du client");
        }
        LocalDate aujourdhui = LocalDate.now();
        LocalDate retenue = dateEffet != null ? dateEffet : aujourdhui;
        if (retenue.isBefore(aujourdhui)) {
            throw new IllegalArgumentException("La date d'effet ne peut pas etre dans le passe");
        }
        return retenue;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Avenant enregistrerPourLocation(ContratLocation contrat, LocalDate dateEffet, String motif,
                                           List<Modification> modifications, Integer currentUserId) {
        Avenant avenant = Avenant.builder()
                .contratLocation(contrat)
                .numeroOrdre(avenantRepository.dernierNumeroLocation(contrat.getIdContratLocation()) + 1)
                .dateEffet(dateEffet)
                .motif(motif.trim())
                .userCreate(currentUserId)
                .build();
        return sauvegarder(avenant, modifications);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Avenant enregistrerPourMandat(ContratMandat mandat, LocalDate dateEffet, String motif,
                                         List<Modification> modifications, Integer currentUserId) {
        Avenant avenant = Avenant.builder()
                .mandat(mandat)
                .numeroOrdre(avenantRepository.dernierNumeroMandat(mandat.getIdMandat()) + 1)
                .dateEffet(dateEffet)
                .motif(motif.trim())
                .userCreate(currentUserId)
                .build();
        return sauvegarder(avenant, modifications);
    }

    private Avenant sauvegarder(Avenant avenant, List<Modification> modifications) {
        for (Modification m : modifications) {
            avenant.getLignes().add(AvenantLigne.builder()
                    .avenant(avenant)
                    .champ(m.champ())
                    .ancienneValeur(m.ancienneValeur())
                    .nouvelleValeur(m.nouvelleValeur())
                    .build());
        }
        return avenantRepository.save(avenant);
    }
}
