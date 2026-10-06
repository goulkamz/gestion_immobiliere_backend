package com.immobilier.gestionImmobiliere.modules.annonces.services;

import com.immobilier.gestionImmobiliere.donnees.annonces.model.Offre;
import com.immobilier.gestionImmobiliere.donnees.annonces.model.StatutOffre;
import com.immobilier.gestionImmobiliere.donnees.annonces.repository.OffreRepository;
import com.immobilier.gestionImmobiliere.exceptions.ResourceNotFoundException;
import com.immobilier.gestionImmobiliere.exceptions.TooManyRequestsException;
import com.immobilier.gestionImmobiliere.modules.annonces.dto.requests.CreateOffreDTO;
import com.immobilier.gestionImmobiliere.modules.annonces.dto.requests.UpdateStatutOffreDTO;
import com.immobilier.gestionImmobiliere.modules.annonces.dto.responses.OffreResponseDTO;
import com.immobilier.gestionImmobiliere.modules.user.jwt.RateLimitService;
import com.immobilier.gestionImmobiliere.utils.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static com.immobilier.gestionImmobiliere.utils.BuildSuccessResponse.buildSuccessResponse;

@Service
public class OffreService {

    private final OffreRepository offreRepository;
    private final RateLimitService rateLimitService;
    private final ClientIpResolver clientIpResolver;

    public OffreService(OffreRepository offreRepository, RateLimitService rateLimitService, ClientIpResolver clientIpResolver) {
        this.offreRepository = offreRepository;
        this.rateLimitService = rateLimitService;
        this.clientIpResolver = clientIpResolver;
    }

    public ResponseEntity<?> getAll(StatutOffre statut, Pageable pageable) {
        Page<Offre> offres = statut != null
                ? offreRepository.findByStatut(statut, pageable)
                : offreRepository.findAll(pageable);
        Page<OffreResponseDTO> page = offres.map(this::toDto);
        return buildSuccessResponse(HttpStatus.OK, "Liste des offres", "OFFRE_LIST", page);
    }

    public ResponseEntity<?> getById(Integer id) {
        return buildSuccessResponse(HttpStatus.OK, "Offre trouvée", "OFFRE_FOUND", toDto(findOrThrow(id)));
    }

    @Transactional
    public ResponseEntity<?> create(CreateOffreDTO dto, HttpServletRequest request) {
        if (rateLimitService.estLimiteDepassee("offre:" + clientIpResolver.resolve(request))) {
            throw new TooManyRequestsException("Trop d'offres envoyées. Veuillez réessayer plus tard.");
        }

        Offre offre = Offre.builder()
                .nomComplet(dto.getNomComplet())
                .email(dto.getEmail())
                .telephone(dto.getTelephone())
                .typeOffre(dto.getTypeOffre())
                .titre(dto.getTitre())
                .description(dto.getDescription())
                .adresse(dto.getAdresse())
                .dateOffre(LocalDateTime.now())
                .statut(StatutOffre.EN_ATTENTE)
                .build();
        offreRepository.save(offre);
        return buildSuccessResponse(HttpStatus.CREATED, "Offre déposée avec succès", "OFFRE_CREATED", toDto(offre));
    }

    @Transactional
    public ResponseEntity<?> updateStatut(Integer id, UpdateStatutOffreDTO dto) {
        Offre offre = findOrThrow(id);
        offre.setStatut(dto.getStatut());
        offreRepository.save(offre);
        return buildSuccessResponse(HttpStatus.OK, "Statut mis à jour", "OFFRE_STATUT_UPDATED", toDto(offre));
    }

    private Offre findOrThrow(Integer id) {
        return offreRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("offre", id));
    }

    private OffreResponseDTO toDto(Offre o) {
        return OffreResponseDTO.builder()
                .idOffre(o.getIdOffre())
                .nomComplet(o.getNomComplet())
                .email(o.getEmail())
                .telephone(o.getTelephone())
                .typeOffre(o.getTypeOffre())
                .titre(o.getTitre())
                .description(o.getDescription())
                .adresse(o.getAdresse())
                .dateOffre(o.getDateOffre())
                .statut(o.getStatut())
                .build();
    }
}