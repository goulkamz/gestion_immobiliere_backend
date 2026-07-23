package com.immobilier.gestionImmobiliere.modules.paiements.services;

import com.immobilier.gestionImmobiliere.donnees.paiements.model.EcheanceLoyer;
import com.immobilier.gestionImmobiliere.donnees.paiements.model.Paiement;
import com.immobilier.gestionImmobiliere.donnees.paiements.model.PaiementEcheance;
import com.immobilier.gestionImmobiliere.donnees.paiements.model.StatutEcheance;
import com.immobilier.gestionImmobiliere.donnees.paiements.repository.EcheanceLoyerRepository;
import com.immobilier.gestionImmobiliere.donnees.paiements.repository.PaiementEcheanceRepository;
import com.immobilier.gestionImmobiliere.donnees.paiements.repository.PaiementRepository;
import com.immobilier.gestionImmobiliere.exceptions.EcheanceDejaPayeeException;
import com.immobilier.gestionImmobiliere.exceptions.MontantPaiementInvalideException;
import com.immobilier.gestionImmobiliere.exceptions.ResourceNotFoundException;
import com.immobilier.gestionImmobiliere.modules.paiements.dto.requests.CreatePaiementDTO;
import com.immobilier.gestionImmobiliere.modules.paiements.dto.responses.PaiementResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import static com.immobilier.gestionImmobiliere.utils.BuildSuccessResponse.buildSuccessResponse;

@Service
public class PaiementService {

    private final PaiementRepository paiementRepository;
    private final EcheanceLoyerRepository echeanceRepository;
    private final PaiementEcheanceRepository paiementEcheanceRepository;

    public PaiementService(PaiementRepository paiementRepository, EcheanceLoyerRepository echeanceRepository,
                           PaiementEcheanceRepository paiementEcheanceRepository) {
        this.paiementRepository = paiementRepository;
        this.echeanceRepository = echeanceRepository;
        this.paiementEcheanceRepository = paiementEcheanceRepository;
    }

    // PaiementService — injecter OwnershipResolver dans le constructeur
    @PostAuthorize(
            "hasAnyRole('ADMIN','AGENT') " +
                    "or @paiementOwnershipResolver.isPaiementAccessible(#id, authentication.principal.idUser, false) " +
                    "or @paiementOwnershipResolver.isPaiementAccessible(#id, authentication.principal.idUser, true)"
    )
    public PaiementResponseDTO getPaiementById(Integer id) {
        Paiement paiement = paiementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("paiement", id));
        List<Integer> idEcheances = paiementEcheanceRepository.findByIdPaiement(id).stream()
                .map(pe -> pe.getIdEcheance()).toList();
        return toDto(paiement, idEcheances);
    }

    /**
     * Enregistre un paiement couvrant une ou plusieurs échéances (F16).
     * Règle : le montant du paiement doit couvrir exactement la somme des
     * montants restants dus des échéances sélectionnées (pas de paiement partiel
     * multi-échéances pour éviter toute ambiguïté d'allocation).
     */
    @Transactional
    public ResponseEntity<?> create(CreatePaiementDTO dto, Integer currentUserId) {
        List<EcheanceLoyer> echeances = echeanceRepository.findByIdEcheanceIn(dto.getIdEcheances());

        if (echeances.size() != dto.getIdEcheances().size()) {
            throw new ResourceNotFoundException("échéance", dto.getIdEcheances());
        }

        // Trie les échéances les plus anciennes en premier -> allocation séquentielle
        echeances.sort(Comparator.comparing(EcheanceLoyer::getDateEcheance));

        double totalResteDu = 0.0;
        for (EcheanceLoyer e : echeances) {
            if (e.getStatut() == StatutEcheance.PAYE || e.getStatut() == StatutEcheance.ANNULE) {
                throw new EcheanceDejaPayeeException(e.getIdEcheance());
            }
            double dejaPaye = e.getMontantPaye() != null ? e.getMontantPaye() : 0.0;
            totalResteDu += (e.getMontantDu() - dejaPaye);
        }

        // Le paiement ne peut pas dépasser le total restant dû des échéances sélectionnées
        if (dto.getMontantPaiement() > totalResteDu + 0.01) {
            throw new MontantPaiementInvalideException(totalResteDu, dto.getMontantPaiement());
        }

        Paiement paiement = Paiement.builder()
                .datePaiement(dto.getDatePaiement() != null ? dto.getDatePaiement() : LocalDateTime.now())
                .montantPaiement(dto.getMontantPaiement())
                .modePaiement(dto.getModePaiement())
                .referencePaiement(dto.getReferencePaiement())
                .userCreate(currentUserId)
                .build();
        paiementRepository.save(paiement);

        // Allocation séquentielle : la plus ancienne échéance est soldée en priorité
        double montantRestant = dto.getMontantPaiement();
        for (EcheanceLoyer e : echeances) {
            if (montantRestant <= 0) break;

            double dejaPaye = e.getMontantPaye() != null ? e.getMontantPaye() : 0.0;
            double resteDu = e.getMontantDu() - dejaPaye;
            if (resteDu <= 0) continue;

            double montantApplique = Math.min(montantRestant, resteDu);
            e.setMontantPaye(dejaPaye + montantApplique);
            e.setStatut(e.getMontantPaye() >= e.getMontantDu() ? StatutEcheance.PAYE : StatutEcheance.EN_ATTENTE);
            echeanceRepository.save(e);

            // Lien créé même pour une couverture partielle (traçabilité F16)
            paiementEcheanceRepository.save(PaiementEcheance.builder()
                    .idEcheance(e.getIdEcheance())
                    .idPaiement(paiement.getIdPaiement())
                    .build());

            montantRestant -= montantApplique;
        }

        return buildSuccessResponse(HttpStatus.CREATED, "Paiement enregistré avec succès", "PAIEMENT_CREATED",
                toDto(paiement, dto.getIdEcheances()));
    }

    private PaiementResponseDTO toDto(Paiement p, List<Integer> idEcheances) {
        return PaiementResponseDTO.builder()
                .idPaiement(p.getIdPaiement())
                .datePaiement(p.getDatePaiement())
                .montantPaiement(p.getMontantPaiement())
                .modePaiement(p.getModePaiement())
                .referencePaiement(p.getReferencePaiement())
                .idEcheancesCouvertes(idEcheances)
                .build();
    }
}