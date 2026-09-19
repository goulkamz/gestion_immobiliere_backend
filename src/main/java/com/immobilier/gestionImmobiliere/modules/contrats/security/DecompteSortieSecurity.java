package com.immobilier.gestionImmobiliere.modules.contrats.security;

import com.immobilier.gestionImmobiliere.donnees.contrats.model.DecompteSortie;
import com.immobilier.gestionImmobiliere.donnees.contrats.repository.DecompteSortieRepository;
import org.springframework.stereotype.Component;

@Component("decompteSortieSecurity")
public class DecompteSortieSecurity {

    private final DecompteSortieRepository decompteSortieRepository;

    public DecompteSortieSecurity(DecompteSortieRepository decompteSortieRepository) {
        this.decompteSortieRepository = decompteSortieRepository;
    }

    public boolean isAccessible(Integer idDecompte, Integer idUser) {
        DecompteSortie decompte = decompteSortieRepository.findById(idDecompte).orElse(null);
        if (decompte == null) return false;

        var contrat = decompte.getContratLocation();
        if (contrat.getLocataire().getIdUser().equals(idUser)) return true;

        return contrat.getMaison().getCour().getProprietaire().getIdUser().equals(idUser);
    }
}