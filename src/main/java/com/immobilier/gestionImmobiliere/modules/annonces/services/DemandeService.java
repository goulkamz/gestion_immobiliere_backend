package com.immobilier.gestionImmobiliere.modules.annonces.services;

import com.immobilier.gestionImmobiliere.donnees.annonces.model.Demande;
import com.immobilier.gestionImmobiliere.donnees.annonces.model.StatutDemande;
import com.immobilier.gestionImmobiliere.donnees.annonces.repository.DemandeRepository;
import com.immobilier.gestionImmobiliere.exceptions.ResourceNotFoundException;
import com.immobilier.gestionImmobiliere.modules.annonces.dto.requests.CreateDemandeDTO;
import com.immobilier.gestionImmobiliere.modules.annonces.dto.requests.UpdateStatutDemandeDTO;
import com.immobilier.gestionImmobiliere.modules.annonces.dto.responses.DemandeResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static com.immobilier.gestionImmobiliere.utils.BuildSuccessResponse.buildSuccessResponse;

@Service
public class DemandeService {

    private final DemandeRepository demandeRepository;

    public DemandeService(DemandeRepository demandeRepository) {
        this.demandeRepository = demandeRepository;
    }

    public ResponseEntity<?> getAll(StatutDemande statut, Pageable pageable) {
        Page<Demande> page = statut != null ? demandeRepository.findByStatut(statut, pageable) : demandeRepository.findAll(pageable);
        return buildSuccessResponse(HttpStatus.OK, "Liste des demandes", "DEMANDE_LIST", page.map(this::toDto));
    }

    public ResponseEntity<?> getById(Integer id) {
        return buildSuccessResponse(HttpStatus.OK, "Demande trouvée", "DEMANDE_FOUND", toDto(findOrThrow(id)));
    }

    @Transactional
    public ResponseEntity<?> create(CreateDemandeDTO dto) {
        Demande demande = Demande.builder()
                .nomComplet(dto.getNomComplet())
                .email(dto.getEmail())
                .telephone(dto.getTelephone())
                .typeBien(dto.getTypeBien())
                .localisationSouhaite(dto.getLocalisationSouhaite())
                .budgetMax(dto.getBudgetMax())
                .description(dto.getDescription())
                .dateDemande(LocalDateTime.now())
                .statut(StatutDemande.EN_ATTENTE)
                .build();
        demandeRepository.save(demande);
        return buildSuccessResponse(HttpStatus.CREATED, "Demande déposée avec succès", "DEMANDE_CREATED", toDto(demande));
    }

    @Transactional
    public ResponseEntity<?> updateStatut(Integer id, UpdateStatutDemandeDTO dto) {
        Demande demande = findOrThrow(id);
        demande.setStatut(dto.getStatut());
        demandeRepository.save(demande);
        return buildSuccessResponse(HttpStatus.OK, "Statut mis à jour", "DEMANDE_STATUT_UPDATED", toDto(demande));
    }

    private Demande findOrThrow(Integer id) {
        return demandeRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("demande", id));
    }

    private DemandeResponseDTO toDto(Demande d) {
        return DemandeResponseDTO.builder()
                .idDemande(d.getIdDemande())
                .nomComplet(d.getNomComplet())
                .email(d.getEmail())
                .telephone(d.getTelephone())
                .typeBien(d.getTypeBien())
                .localisationSouhaite(d.getLocalisationSouhaite())
                .budgetMax(d.getBudgetMax())
                .description(d.getDescription())
                .dateDemande(d.getDateDemande())
                .statut(d.getStatut())
                .build();
    }
}