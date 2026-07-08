package com.immobilier.gestionImmobiliere.modules.localisation.services;

import com.immobilier.gestionImmobiliere.donnees.localisation.model.Pays;
import com.immobilier.gestionImmobiliere.donnees.localisation.repository.PaysRepository;
import com.immobilier.gestionImmobiliere.exceptions.CodeAlreadyExistsException;
import com.immobilier.gestionImmobiliere.exceptions.ResourceNotFoundException;
import com.immobilier.gestionImmobiliere.modules.localisation.dto.requests.CreatePaysDTO;
import com.immobilier.gestionImmobiliere.modules.localisation.dto.requests.UpdatePaysDTO;
import com.immobilier.gestionImmobiliere.modules.localisation.dto.responses.PaysResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.immobilier.gestionImmobiliere.utils.BuildSuccessResponse.buildSuccessResponse;

@Service
public class PaysService {

    private final PaysRepository paysRepository;

    public PaysService(PaysRepository paysRepository) {
        this.paysRepository = paysRepository;
    }

    public ResponseEntity<?> getAll(Pageable pageable) {
        Page<PaysResponseDTO> result = paysRepository.findAll(pageable).map(this::toDto);
        return buildSuccessResponse(HttpStatus.OK, "Liste des localisation", "PAYS_LIST", result);
    }

    public ResponseEntity<?> getById(Long id) {
        Pays pays = findOrThrow(id);
        return buildSuccessResponse(HttpStatus.OK, "Pays trouvé", "PAYS_FOUND", toDto(pays));
    }

    @Transactional
    public ResponseEntity<?> create(CreatePaysDTO dto) {
        if (paysRepository.existsByCodePaysIgnoreCase(dto.getCodePays())) {
            throw new CodeAlreadyExistsException("pays", dto.getCodePays());
        }
        Pays pays = Pays.builder()
                .codePays(dto.getCodePays())
                .nomPays(dto.getNomPays())
                .build();
        paysRepository.save(pays);
        return buildSuccessResponse(HttpStatus.CREATED, "Pays créé avec succès", "PAYS_CREATED", toDto(pays));
    }

    @Transactional
    public ResponseEntity<?> update(Long id, UpdatePaysDTO dto) {
        Pays pays = findOrThrow(id);

        if (dto.getCodePays() != null && !dto.getCodePays().equalsIgnoreCase(pays.getCodePays())
                && paysRepository.existsByCodePaysIgnoreCase(dto.getCodePays())) {
            throw new CodeAlreadyExistsException("pays", dto.getCodePays());
        }

        if (dto.getCodePays() != null) pays.setCodePays(dto.getCodePays());
        if (dto.getNomPays() != null) pays.setNomPays(dto.getNomPays());

        paysRepository.save(pays);
        return buildSuccessResponse(HttpStatus.OK, "Pays mis à jour", "PAYS_UPDATED", toDto(pays));
    }

    @Transactional
    public ResponseEntity<?> delete(Long id) {
        Pays pays = findOrThrow(id);
        paysRepository.delete(pays); // intercepté par @SQLDelete -> soft delete
        return buildSuccessResponse(HttpStatus.OK, "Pays supprimé", "PAYS_DELETED", null);
    }

    private Pays findOrThrow(Long id) {
        return paysRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Pays",id));
    }

    private PaysResponseDTO toDto(Pays p) {
        return PaysResponseDTO.builder()
                .idPays(p.getIdPays())
                .codePays(p.getCodePays())
                .nomPays(p.getNomPays())
                .build();
    }
}