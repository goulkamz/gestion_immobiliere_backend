package com.immobilier.gestionImmobiliere.modules.paiements.services;

import com.immobilier.gestionImmobiliere.donnees.contrats.repository.ContratLocationRepository;
import com.immobilier.gestionImmobiliere.donnees.contrats.repository.ContratMandatRepository;
import com.immobilier.gestionImmobiliere.donnees.paiements.model.EcheanceLoyer;
import com.immobilier.gestionImmobiliere.donnees.paiements.model.StatutEcheance;
import com.immobilier.gestionImmobiliere.donnees.paiements.model.TypeEcheance;
import com.immobilier.gestionImmobiliere.donnees.paiements.repository.EcheanceLoyerRepository;
import com.immobilier.gestionImmobiliere.exceptions.ResourceNotFoundException;
import com.immobilier.gestionImmobiliere.modules.paiements.dto.responses.EcheanceResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static com.immobilier.gestionImmobiliere.utils.BuildSuccessResponse.buildSuccessResponse;

@Service
public class EcheanceService {

    private final EcheanceLoyerRepository echeanceRepository;
    private final ContratLocationRepository contratLocationRepository;
    private final ContratMandatRepository contratMandatRepository;

    public EcheanceService(EcheanceLoyerRepository echeanceRepository, ContratLocationRepository contratLocationRepository, ContratMandatRepository contratMandatRepository) {
        this.echeanceRepository = echeanceRepository;
        this.contratLocationRepository = contratLocationRepository;
        this.contratMandatRepository = contratMandatRepository;
    }

    // Remplace getAll() par une version filtrée par rôle
    public ResponseEntity<?> getAllForCurrentUser(TypeEcheance type, Integer entiteId, StatutEcheance statut,
                                                  Integer currentUserId, boolean isAdminOrAgent, boolean isBailleur,
                                                  Pageable pageable) {
        Page<EcheanceLoyer> page;

        if (isAdminOrAgent) {
            if (type != null && entiteId != null) {
                page = echeanceRepository.findByEntiteEcheanceTypeAndEntiteEcheanceId(type, entiteId, pageable);
            } else if (statut != null) {
                page = echeanceRepository.findByStatut(statut, pageable);
            } else {
                page = echeanceRepository.findAll(pageable);
            }
        } else if (isBailleur) {
            List<Integer> locationIds = contratLocationRepository.findIdsByProprietaire(currentUserId);
            List<Integer> mandatIds = contratMandatRepository.findIdsByProprietaire(currentUserId);
            page = echeanceRepository.findForBailleur(
                    locationIds.isEmpty() ? List.of(-1) : locationIds,
                    mandatIds.isEmpty() ? List.of(-1) : mandatIds,
                    pageable);
        } else {
            // CLIENT : uniquement les échéances de ses propres contrats de location
            List<Integer> locationIds = contratLocationRepository.findIdsByLocataire(currentUserId);
            page = echeanceRepository.findByEntiteEcheanceTypeAndEntiteEcheanceIdIn(
                    TypeEcheance.LOCATION, locationIds.isEmpty() ? List.of(-1) : locationIds, pageable);
        }

        return buildSuccessResponse(HttpStatus.OK, "Liste des échéances", "ECHEANCE_LIST", page.map(this::toDto));
    }


    @PostAuthorize(
            "hasAnyRole('ADMIN','AGENT') " +
                    "or @ownershipResolver.isEcheanceOwnedByClient(returnObject.type, returnObject.entiteId, authentication.principal.id) " +
                    "or @ownershipResolver.isEcheanceOwnedByBailleur(returnObject.type, returnObject.entiteId, authentication.principal.id)"
    )
    public EcheanceResponseDTO getEcheanceById(Integer id) {
        return toDto(findOrThrow(id));
    }

    public ResponseEntity<?> getById(Integer id) {
        return buildSuccessResponse(HttpStatus.OK, "Échéance trouvée", "ECHEANCE_FOUND", toDto(findOrThrow(id)));
    }

    public ResponseEntity<?> getEnRetard() {
        List<EcheanceLoyer> enRetard = echeanceRepository.findByStatutAndDateEcheanceBefore(StatutEcheance.EN_ATTENTE, LocalDate.now());
        return buildSuccessResponse(HttpStatus.OK, "Échéances en retard", "ECHEANCE_EN_RETARD_LIST",
                enRetard.stream().map(this::toDto).toList());
    }

    /**
     * Job de bascule EN_ATTENTE -> EN_RETARD (à brancher sur un @Scheduled quotidien).
     */
    @Transactional
    public int marquerEnRetard() {
        List<EcheanceLoyer> expirees = echeanceRepository.findByStatutAndDateEcheanceBefore(StatutEcheance.EN_ATTENTE, LocalDate.now());
        expirees.forEach(e -> e.setStatut(StatutEcheance.EN_RETARD));
        echeanceRepository.saveAll(expirees);
        return expirees.size();
    }

    EcheanceLoyer findOrThrow(Integer id) {
        return echeanceRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("échéance", id));
    }

    private EcheanceResponseDTO toDto(EcheanceLoyer e) {
        return EcheanceResponseDTO.builder()
                .idEcheance(e.getIdEcheance())
                .type(e.getEntiteEcheanceType())
                .entiteId(e.getEntiteEcheanceId())
                .dateEcheance(e.getDateEcheance())
                .montantDu(e.getMontantDu())
                .montantPaye(e.getMontantPaye())
                .statut(e.getStatut())
                .build();
    }
}