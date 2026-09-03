package com.immobilier.gestionImmobiliere.modules.paiements.services;

import com.immobilier.gestionImmobiliere.donnees.paiements.model.*;
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
                .map(PaiementEcheance::getIdEcheance).toList();
        return toDto(paiement, idEcheances);
    }

    /**
     * Enregistre un paiement couvrant une ou plusieurs échéances (F16).
     * Règle : le montant du paiement doit couvrir exactement la somme des
     * montants restants dus des échéances sélectionnées (pas de paiement partiel
     * multi-échéances pour éviter toute ambiguïté d'allocation).
     */
    /**
     * Point d'entrée public — paiement d'un locataire couvrant ses échéances LOCATION.
     * Sens toujours ENTREE (argent reçu par l'agence).
     */
    @Transactional
    public ResponseEntity<?> create(CreatePaiementDTO dto, Integer currentUserId) {
        Paiement paiement = soldeEcheances(
                dto.getIdEcheances(),
                dto.getMontantPaiement(),
                dto.getModePaiement(),
                dto.getReferencePaiement(),
                SensPaiement.ENTREE,
                currentUserId,
                dto.getDatePaiement() != null ? dto.getDatePaiement() : LocalDateTime.now());

        return buildSuccessResponse(HttpStatus.CREATED, "Paiement enregistré avec succès", "PAIEMENT_CREATED",
                toDto(paiement, dto.getIdEcheances()));
    }

    /**
     * Moteur générique de règlement d'échéance(s) — réutilisable par n'importe quel flux
     * (paiement client LOCATION en ENTREE, virement bailleur MANDAT en SORTIE, etc.).
     * Règles communes, quel que soit l'appelant :
     * - le montant ne peut jamais dépasser le reste dû des échéances sélectionnées ;
     * - allocation séquentielle, la plus ancienne échéance soldée en priorité ;
     * - chaque échéance touchée est liée au paiement via paiement_echeance, même
     *   pour une couverture partielle (traçabilité F16).
     */
    @Transactional
    public Paiement soldeEcheances(List<Integer> idEcheances, Double montant, String modePaiement,
                                   String reference, SensPaiement sens, Integer currentUserId, LocalDateTime datePaiement) {
        List<EcheanceLoyer> echeances = echeanceRepository.findByIdEcheanceIn(idEcheances);

        if (echeances.size() != idEcheances.size()) {
            throw new ResourceNotFoundException("échéance", idEcheances);
        }

        echeances.sort(Comparator.comparing(EcheanceLoyer::getDateEcheance));

        double totalResteDu = 0.0;
        for (EcheanceLoyer e : echeances) {
            if (e.getStatut() == StatutEcheance.PAYE || e.getStatut() == StatutEcheance.ANNULE) {
                throw new EcheanceDejaPayeeException(e.getIdEcheance());
            }
            double dejaPaye = e.getMontantPaye() != null ? e.getMontantPaye() : 0.0;
            totalResteDu += (e.getMontantDu() - dejaPaye);
        }

        if (montant > totalResteDu + 0.01) {
            throw new MontantPaiementInvalideException(totalResteDu, montant);
        }

        Paiement paiement = Paiement.builder()
                .datePaiement(datePaiement)
                .montantPaiement(montant)
                .modePaiement(modePaiement)
                .referencePaiement(reference)
                .sens(sens)
                .userCreate(currentUserId)
                .build();
        paiementRepository.save(paiement);

        double montantRestant = montant;
        for (EcheanceLoyer e : echeances) {
            if (montantRestant <= 0) break;

            double dejaPaye = e.getMontantPaye() != null ? e.getMontantPaye() : 0.0;
            double resteDu = e.getMontantDu() - dejaPaye;
            if (resteDu <= 0) continue;

            double montantApplique = Math.min(montantRestant, resteDu);
            e.setMontantPaye(dejaPaye + montantApplique);
            e.setStatut(e.getMontantPaye() >= e.getMontantDu() ? StatutEcheance.PAYE : StatutEcheance.EN_ATTENTE);
            echeanceRepository.save(e);

            paiementEcheanceRepository.save(PaiementEcheance.builder()
                    .idEcheance(e.getIdEcheance())
                    .idPaiement(paiement.getIdPaiement())
                    .build());

            montantRestant -= montantApplique;
        }

        return paiement;
    }

    private PaiementResponseDTO toDto(Paiement p, List<Integer> idEcheances) {
        return PaiementResponseDTO.builder()
                .idPaiement(p.getIdPaiement())
                .datePaiement(p.getDatePaiement())
                .montantPaiement(p.getMontantPaiement())
                .sens(String.valueOf(p.getSens()))
                .modePaiement(p.getModePaiement())
                .referencePaiement(p.getReferencePaiement())
                .idEcheancesCouvertes(idEcheances)
                .build();
    }
}