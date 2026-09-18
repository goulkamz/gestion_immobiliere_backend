package com.immobilier.gestionImmobiliere.modules.contrats.security;

import com.immobilier.gestionImmobiliere.donnees.contrats.model.ContratMandat;
import com.immobilier.gestionImmobiliere.donnees.contrats.repository.ContratMandatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("contratMandatSecurity")
@RequiredArgsConstructor
public class ContratMandatSecurity {

    private final ContratMandatRepository mandatRepository;
    private final ContratMandatRepository contratMandatRepository;

    public boolean isProprietaireCour(Integer idMandat, Integer currentUserId) {
        return mandatRepository.findById(idMandat)
                .map(m -> m.getCour().getProprietaire().getIdUser().equals(currentUserId))
                .orElse(false);
    }

    /**
     * Accessible par l'agent mandataire ou le bailleur propriétaire de la cour.
     */
    public boolean isAccessible(Integer idMandat, Integer idUser) {
        ContratMandat mandat = contratMandatRepository.findById(idMandat).orElse(null);
        if (mandat == null) return false;

        if (mandat.getAgent().getIdUser().equals(idUser)) return true;

        return mandat.getCour().getProprietaire().getIdUser().equals(idUser);
    }
}