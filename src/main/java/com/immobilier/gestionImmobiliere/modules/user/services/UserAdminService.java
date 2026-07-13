package com.immobilier.gestionImmobiliere.modules.user.services;

import com.immobilier.gestionImmobiliere.donnees.user.model.ERole;
import com.immobilier.gestionImmobiliere.donnees.user.model.Role;
import com.immobilier.gestionImmobiliere.donnees.user.repository.RoleRepository;
import com.immobilier.gestionImmobiliere.donnees.user.model.User;
import com.immobilier.gestionImmobiliere.donnees.user.repository.UserRepository;
import com.immobilier.gestionImmobiliere.exceptions.CannotDeactivateSelfException;
import com.immobilier.gestionImmobiliere.exceptions.EmailAlreadyExistsException;
import com.immobilier.gestionImmobiliere.exceptions.ResourceNotFoundException;
import com.immobilier.gestionImmobiliere.exceptions.RoleNotFoundException;
import com.immobilier.gestionImmobiliere.modules.journal.services.JournalService;
import com.immobilier.gestionImmobiliere.modules.user.dto.requests.*;
import com.immobilier.gestionImmobiliere.modules.user.dto.responses.UserAdminResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static com.immobilier.gestionImmobiliere.utils.BuildSuccessResponse.buildSuccessResponse;

@Service
public class UserAdminService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JournalService journalService;

    public UserAdminService(UserRepository userRepository, RoleRepository roleRepository,
                            PasswordEncoder passwordEncoder, JournalService journalService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.journalService = journalService;
    }

    public ResponseEntity<?> getAll(ERole role, Pageable pageable) {
        Page<User> page = role != null ? userRepository.findByRole_LibelleRole(role, pageable) : userRepository.findAll(pageable);
        return buildSuccessResponse(HttpStatus.OK, "Liste des utilisateurs", "USER_LIST", page.map(this::toDto));
    }

    public ResponseEntity<?> getById(Integer id) {
        return buildSuccessResponse(HttpStatus.OK, "Utilisateur trouvé", "USER_FOUND", toDto(findOrThrow(id)));
    }

    @Transactional
    public ResponseEntity<?> create(CreateUserByAdminDTO dto, Integer currentUserId) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new EmailAlreadyExistsException(dto.getEmail());
        }
        if (dto.getTelephone() != null && userRepository.existsByTelephone(dto.getTelephone())) {
            throw new RuntimeException("Ce numéro de téléphone est déjà utilisé");
        }

        Role role = roleRepository.findByLibelleRole(dto.getRole())
                .orElseThrow(() -> new RoleNotFoundException(dto.getRole().toString()));

        User user = new User();
        user.setEmail(dto.getEmail());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNom(dto.getNom());
        user.setPrenom(dto.getPrenom());
        user.setSexe(dto.getSexe());
        user.setTelephone(dto.getTelephone());
        user.setTelephone1(dto.getTelephone1());
        user.setDateNaissance(dto.getDateNaissance());
        user.setRole(role);
        user.setFlagActif(true);
        user.initTimestamp(); // méthode déjà utilisée dans activation() de UserService
        userRepository.save(user);

        journalService.enregistrer(currentUserId, "CREATION", "users", user.getIdUser(),
                "Création d'un compte par l'administrateur, rôle=" + dto.getRole(), null, "role=" + dto.getRole());

        return buildSuccessResponse(HttpStatus.CREATED, "Utilisateur créé avec succès", "USER_CREATED", toDto(user));
    }

    @Transactional
    public ResponseEntity<?> update(Integer id, UpdateUserByAdminDTO dto, Integer currentUserId) {
        User user = findOrThrow(id);

        if (dto.getTelephone() != null && userRepository.existsByTelephoneAndIdUserNot(dto.getTelephone(), id)) {
            throw new RuntimeException("Ce numéro de téléphone est déjà utilisé par un autre compte");
        }

        if (dto.getNom() != null) user.setNom(dto.getNom());
        if (dto.getPrenom() != null) user.setPrenom(dto.getPrenom());
        if (dto.getSexe() != null) user.setSexe(dto.getSexe());
        if (dto.getTelephone() != null) user.setTelephone(dto.getTelephone());
        if (dto.getTelephone1() != null) user.setTelephone1(dto.getTelephone1());
        if (dto.getDateNaissance() != null) user.setDateNaissance(dto.getDateNaissance());

        userRepository.save(user);
        return buildSuccessResponse(HttpStatus.OK, "Utilisateur mis à jour", "USER_UPDATED", toDto(user));
    }

    @Transactional
    public ResponseEntity<?> updateRole(Integer id, UpdateUserRoleDTO dto, Integer currentUserId) {
        User user = findOrThrow(id);
        String ancienRole = user.getRole().getLibelleRole().name();

        Role nouveauRole = roleRepository.findByLibelleRole(dto.getRole())
                .orElseThrow(() -> new RoleNotFoundException(dto.getRole().toString()));
        user.setRole(nouveauRole);
        userRepository.save(user);

        journalService.enregistrer(currentUserId, "CHANGEMENT_ROLE", "users", user.getIdUser(),
                "Changement de rôle utilisateur", "role=" + ancienRole, "role=" + dto.getRole());

        return buildSuccessResponse(HttpStatus.OK, "Rôle mis à jour", "USER_ROLE_UPDATED", toDto(user));
    }

    @Transactional
    public ResponseEntity<?> updateStatus(Integer id, UpdateUserStatusDTO dto, Integer currentUserId) {
        if (id.equals(currentUserId) && Boolean.FALSE.equals(dto.getFlagActif())) {
            throw new CannotDeactivateSelfException();
        }

        User user = findOrThrow(id);
        Boolean ancienStatut = user.isFlagActif();
        user.setFlagActif(dto.getFlagActif());
        userRepository.save(user);

        journalService.enregistrer(currentUserId, dto.getFlagActif() ? "ACTIVATION" : "DESACTIVATION", "users", user.getIdUser(),
                "Changement d'état du compte", "flagActif=" + ancienStatut, "flagActif=" + dto.getFlagActif());

        return buildSuccessResponse(HttpStatus.OK,
                dto.getFlagActif() ? "Compte activé" : "Compte désactivé", "USER_STATUS_UPDATED", toDto(user));
    }

    @Transactional
    public ResponseEntity<?> delete(Integer id, Integer currentUserId) {
        if (id.equals(currentUserId)) {
            throw new CannotDeactivateSelfException(); // même garde-fou pour la suppression
        }
        User user = findOrThrow(id);
        userRepository.delete(user); // soft delete via @SQLDelete (déjà en place sur User)

        journalService.enregistrer(currentUserId, "SUPPRESSION", "users", id,
                "Suppression logique du compte", null, null);

        return buildSuccessResponse(HttpStatus.OK, "Utilisateur supprimé", "USER_DELETED", null);
    }

    private User findOrThrow(Integer id) {
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("utilisateur", id));
    }

    private UserAdminResponseDTO toDto(User u) {
        return UserAdminResponseDTO.builder()
                .idUser(u.getIdUser())
                .email(u.getEmail())
                .nom(u.getNom())
                .prenom(u.getPrenom())
                .sexe(u.getSexe())
                .telephone(u.getTelephone())
                .telephone1(u.getTelephone1())
                .dateNaissance(u.getDateNaissance())
                .flagActif(u.isFlagActif())
                .role(u.getRole().getLibelleRole().name())
                .dateCreate(u.getDateCreate())
                .dateLastLogin(u.getDateLastLogin())
                .build();
    }
}