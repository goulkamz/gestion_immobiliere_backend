package com.immobilier.gestionImmobiliere.modules.annonces.services;

import com.immobilier.gestionImmobiliere.donnees.annonces.model.StatutTemoignage;
import com.immobilier.gestionImmobiliere.donnees.annonces.model.Temoignage;
import com.immobilier.gestionImmobiliere.donnees.annonces.repository.TemoignageRepository;
import com.immobilier.gestionImmobiliere.exceptions.ResourceNotFoundException;
import com.immobilier.gestionImmobiliere.exceptions.TooManyRequestsException;
import com.immobilier.gestionImmobiliere.modules.annonces.dto.requests.CreateTemoignageDTO;
import com.immobilier.gestionImmobiliere.modules.annonces.dto.requests.CreateTemoignagePublicDTO;
import com.immobilier.gestionImmobiliere.modules.annonces.dto.requests.UpdateStatutTemoignageDTO;
import com.immobilier.gestionImmobiliere.modules.annonces.dto.responses.TemoignageResponseDTO;
import com.immobilier.gestionImmobiliere.modules.user.jwt.RateLimitService;
import com.immobilier.gestionImmobiliere.utils.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import static com.immobilier.gestionImmobiliere.utils.BuildSuccessResponse.buildSuccessResponse;

@Service
public class TemoignageService {

    // Depot public : quota serre par IP (le temoignage reste EN_ATTENTE jusqu'a validation)
    private static final int DEPOTS_PUBLICS_MAX = 3;
    private static final Duration DEPOTS_PUBLICS_FENETRE = Duration.ofHours(1);

    private final TemoignageRepository temoignageRepository;
    private final RateLimitService rateLimitService;
    private final ClientIpResolver clientIpResolver;

    public TemoignageService(TemoignageRepository temoignageRepository, RateLimitService rateLimitService,
                             ClientIpResolver clientIpResolver) {
        this.temoignageRepository = temoignageRepository;
        this.rateLimitService = rateLimitService;
        this.clientIpResolver = clientIpResolver;
    }

    public ResponseEntity<?> getAllActifs() {
        List<TemoignageResponseDTO> liste = temoignageRepository
                .findByStatutOrderByDateTemoignageDescIdTemoignageDesc(StatutTemoignage.PUBLIE)
                .stream().map(this::toDto).toList();
        return buildSuccessResponse(HttpStatus.OK, "Liste des témoignages", "TEMOIGNAGE_LIST", liste);
    }

    // Moderation : tous les temoignages quel que soit leur statut (filtre optionnel)
    public ResponseEntity<?> getAll(StatutTemoignage statut, Pageable pageable) {
        Page<Temoignage> page = statut != null
                ? temoignageRepository.findByStatut(statut, pageable)
                : temoignageRepository.findAll(pageable);
        return buildSuccessResponse(HttpStatus.OK, "Liste des témoignages", "TEMOIGNAGE_ADMIN_LIST",
                page.map(this::toAdminDto));
    }

    // Saisie par l'agence (de confiance) : publie des la creation, un admin peut le retirer ensuite
    @Transactional
    public ResponseEntity<?> create(CreateTemoignageDTO dto) {
        Temoignage temoignage = Temoignage.builder()
                .nomAuteur(dto.getNomAuteur())
                .role(dto.getRole())
                .texte(dto.getTexte())
                .note(dto.getNote())
                .photoUrl(dto.getPhotoUrl())
                .dateTemoignage(dto.getDate() != null ? dto.getDate() : LocalDate.now())
                .statut(StatutTemoignage.PUBLIE)
                .build();
        temoignageRepository.save(temoignage);
        return buildSuccessResponse(HttpStatus.CREATED, "Témoignage créé avec succès", "TEMOIGNAGE_CREATED", toAdminDto(temoignage));
    }

    @Transactional
    public ResponseEntity<?> createPublic(CreateTemoignagePublicDTO dto, HttpServletRequest request) {
        if (rateLimitService.estLimiteDepassee("temoignage:" + clientIpResolver.resolve(request),
                DEPOTS_PUBLICS_MAX, DEPOTS_PUBLICS_FENETRE)) {
            throw new TooManyRequestsException("Trop de témoignages envoyés. Veuillez réessayer plus tard.");
        }

        // Piege a robots : on repond comme un succes sans rien enregistrer, pour ne pas renseigner le bot
        if (dto.getSiteWeb() != null && !dto.getSiteWeb().isBlank()) {
            return buildSuccessResponse(HttpStatus.CREATED, "Merci pour votre témoignage, il sera publié après validation", "TEMOIGNAGE_CREATED", null);
        }

        Temoignage temoignage = Temoignage.builder()
                .nomAuteur(dto.getNomAuteur().trim())
                .statut(StatutTemoignage.EN_ATTENTE) // valide par un admin avant publication
                .role(dto.getRole())
                .texte(dto.getTexte().trim())
                .note(dto.getNote())
                .dateTemoignage(LocalDate.now())
                .build();
        temoignageRepository.save(temoignage);
        return buildSuccessResponse(HttpStatus.CREATED, "Merci pour votre témoignage, il sera publié après validation", "TEMOIGNAGE_CREATED", toDto(temoignage));
    }

    @Transactional
    public ResponseEntity<?> updateStatut(Integer id, UpdateStatutTemoignageDTO dto) {
        Temoignage temoignage = temoignageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("temoignage", id));
        temoignage.setStatut(dto.getStatut());
        temoignageRepository.save(temoignage);
        return buildSuccessResponse(HttpStatus.OK, "Statut mis à jour", "TEMOIGNAGE_STATUT_UPDATED", toAdminDto(temoignage));
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

    private TemoignageResponseDTO toAdminDto(Temoignage t) {
        TemoignageResponseDTO dto = toDto(t);
        dto.setStatut(t.getStatut());
        return dto;
    }
}
