package com.immobilier.gestionImmobiliere.modules.contrats.security;

import com.immobilier.gestionImmobiliere.donnees.contrats.repository.ContratMandatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("mandatSecurity")
@RequiredArgsConstructor
public class MandatSecurity {

    private final ContratMandatRepository mandatRepository;

    public boolean isProprietaireCour(Integer idMandat, Integer currentUserId) {
        return mandatRepository.findById(idMandat)
                .map(m -> m.getCour().getProprietaire().getIdUser().equals(currentUserId))
                .orElse(false);
    }
}