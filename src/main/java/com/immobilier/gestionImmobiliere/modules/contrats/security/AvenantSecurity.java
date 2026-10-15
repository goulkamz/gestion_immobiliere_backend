package com.immobilier.gestionImmobiliere.modules.contrats.security;

import com.immobilier.gestionImmobiliere.donnees.contrats.model.Avenant;
import com.immobilier.gestionImmobiliere.donnees.contrats.repository.AvenantRepository;
import org.springframework.stereotype.Component;

/** Un avenant est accessible a ceux qui peuvent acceder au contrat qu'il modifie. */
@Component("avenantSecurity")
public class AvenantSecurity {

    private final AvenantRepository avenantRepository;
    private final ContratLocationSecurity contratLocationSecurity;
    private final ContratMandatSecurity contratMandatSecurity;

    public AvenantSecurity(AvenantRepository avenantRepository, ContratLocationSecurity contratLocationSecurity,
                           ContratMandatSecurity contratMandatSecurity) {
        this.avenantRepository = avenantRepository;
        this.contratLocationSecurity = contratLocationSecurity;
        this.contratMandatSecurity = contratMandatSecurity;
    }

    public boolean isAccessible(Integer idAvenant, Integer idUser) {
        if (idAvenant == null || idUser == null) return false;
        Avenant avenant = avenantRepository.findById(idAvenant).orElse(null);
        if (avenant == null) return false;
        if (avenant.getContratLocation() != null) {
            return contratLocationSecurity.isAccessible(avenant.getContratLocation().getIdContratLocation(), idUser);
        }
        if (avenant.getMandat() != null) {
            return contratMandatSecurity.isAccessible(avenant.getMandat().getIdMandat(), idUser);
        }
        return false;
    }
}
