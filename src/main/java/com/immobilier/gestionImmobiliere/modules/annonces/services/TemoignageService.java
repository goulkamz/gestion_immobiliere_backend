package com.immobilier.gestionImmobiliere.modules.annonces.services;

import com.immobilier.gestionImmobiliere.donnees.annonces.model.Temoignage;
import com.immobilier.gestionImmobiliere.donnees.annonces.repository.TemoignageRepository;
import com.immobilier.gestionImmobiliere.exceptions.ResourceNotFoundException;
import com.immobilier.gestionImmobiliere.modules.annonces.dto.requests.UpdateStatutTemoignageDTO;
import com.immobilier.gestionImmobiliere.modules.annonces.dto.responses.TemoignageResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.immobilier.gestionImmobiliere.utils.BuildSuccessResponse.buildSuccessResponse;

@Service
public class TemoignageService {

    private final TemoignageRepository temoignageRepository;

    public TemoignageService(TemoignageRepository temoignageRepository) {
        this.temoignageRepository = temoignageRepository;
    }

    public ResponseEntity<?> getAllActifs() {
        List<TemoignageResponseDTO> liste = temoignageRepository
                .findByFlagActifTrueOrderByDateTemoignageDescIdTemoignageDesc()
                .stream().map(this::toDto).toList();
        return buildSuccessResponse(HttpStatus.OK, "Liste des témoignages", "TEMOIGNAGE_LIST", liste);
    }

    @Transactional
    public ResponseEntity<?> updateStatut(Integer id, UpdateStatutTemoignageDTO dto) {
        Temoignage temoignage = temoignageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("temoignage", id));
        temoignage.setFlagActif(dto.getFlagActif());
        temoignageRepository.save(temoignage);
        return buildSuccessResponse(HttpStatus.OK, "Statut mis à jour", "TEMOIGNAGE_STATUT_UPDATED", toDto(temoignage));
    }

    private TemoignageResponseDTO toDto(Temoignage t) {
        return TemoignageResponseDTO.builder()
                .idTemoignage(t.getIdTemoignage())
                .nomAuteur(t.getNomAuteur())
                .role(t.getRole())
                .texte(t.getTexte())
                .note(t.getNote())
                .photoUrl(t.getPhotoUrl())
                .date(t.getDateTemoignage())
                .build();
    }
}
