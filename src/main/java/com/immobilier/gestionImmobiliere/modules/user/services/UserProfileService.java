package com.immobilier.gestionImmobiliere.modules.user.services;

import com.immobilier.gestionImmobiliere.donnees.user.model.User;
import com.immobilier.gestionImmobiliere.donnees.user.repository.UserRepository;
import com.immobilier.gestionImmobiliere.exceptions.ResourceNotFoundException;
import com.immobilier.gestionImmobiliere.modules.user.dto.requests.UpdateProfileDTO;
import com.immobilier.gestionImmobiliere.modules.user.dto.responses.ProfileResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.immobilier.gestionImmobiliere.utils.BuildSuccessResponse.buildSuccessResponse;

@Service
public class UserProfileService {

    private final UserRepository userRepository;

    public UserProfileService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public ResponseEntity<?> getMe(Integer currentUserId) {
        return buildSuccessResponse(HttpStatus.OK, "Profil récupéré", "PROFILE_FOUND", toDto(findOrThrow(currentUserId)));
    }

    @Transactional
    public ResponseEntity<?> updateMe(Integer currentUserId, UpdateProfileDTO dto) {
        User user = findOrThrow(currentUserId);

        if (dto.getTelephone() != null && userRepository.existsByTelephoneAndIdUserNot(dto.getTelephone(), currentUserId)) {
            throw new RuntimeException("Ce numéro de téléphone est déjà utilisé");
        }

        if (dto.getNom() != null) user.setNom(dto.getNom());
        if (dto.getPrenom() != null) user.setPrenom(dto.getPrenom());
        if (dto.getTelephone() != null) user.setTelephone(dto.getTelephone());
        if (dto.getTelephone1() != null) user.setTelephone1(dto.getTelephone1());
        if (dto.getDateNaissance() != null) user.setDateNaissance(dto.getDateNaissance());
        // email et role : jamais touchés ici, conforme F4

        userRepository.save(user);
        return buildSuccessResponse(HttpStatus.OK, "Profil mis à jour", "PROFILE_UPDATED", toDto(user));
    }

    private User findOrThrow(Integer id) {
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("utilisateur", id));
    }

    private ProfileResponseDTO toDto(User u) {
        return ProfileResponseDTO.builder()
                .idUser(u.getIdUser())
                .email(u.getEmail())
                .nom(u.getNom())
                .prenom(u.getPrenom())
                .sexe(u.getSexe())
                .telephone(u.getTelephone())
                .telephone1(u.getTelephone1())
                .dateNaissance(u.getDateNaissance())
                .role(u.getRole().getLibelleRole().name())
                .build();
    }
}