package com.immobilier.gestionImmobiliere.modules.user.services;

import com.immobilier.gestionImmobiliere.donnees.roles.model.ERole;
import com.immobilier.gestionImmobiliere.donnees.roles.model.Role;
import com.immobilier.gestionImmobiliere.donnees.roles.repository.RoleRepository;
import com.immobilier.gestionImmobiliere.donnees.user.model.User;
import com.immobilier.gestionImmobiliere.donnees.user.model.Validation;
import com.immobilier.gestionImmobiliere.donnees.user.repository.UserRepository;
import com.immobilier.gestionImmobiliere.exceptions.EmailAlreadyExistsException;
import com.immobilier.gestionImmobiliere.exceptions.InvalidEmailException;
import com.immobilier.gestionImmobiliere.exceptions.InvalidPasswordException;
import com.immobilier.gestionImmobiliere.exceptions.RoleNotFoundException;
import com.immobilier.gestionImmobiliere.modules.user.dto.requests.AuthenticateDTO;
import com.immobilier.gestionImmobiliere.modules.user.dto.requests.CreateUserDTO;
import com.immobilier.gestionImmobiliere.modules.user.dto.responses.UserInfoDTO;
import com.immobilier.gestionImmobiliere.modules.user.jwt.JwtUtils;
import com.immobilier.gestionImmobiliere.modules.user.jwtService.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class UserService {


    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    JwtUtils jwtUtils;

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    UserRepository userRepository;

    @Autowired
    ValidationService validationService;


    public ResponseEntity<?> authenticateUser(AuthenticateDTO authenticateDTO) {

            Authentication authentication = authenticationManager
                    .authenticate(new UsernamePasswordAuthenticationToken(authenticateDTO.getUsername(), authenticateDTO.getPassword()));

                UserDetailsImpl user = (UserDetailsImpl) authentication.getPrincipal();
                Map<String, Object> extraClaims = new HashMap<>();
                String jwtCookie = generateJwtCookie(user, extraClaims);
                List<String> roles = getUserRoles(user);

            UserInfoDTO userInfo = UserInfoDTO.builder()
                .username(user.getUsername())
                .roles(roles)
                .token(jwtCookie)
                .build();


            return buildSuccessResponse(HttpStatus.OK, "Authentification réussie", "LOGIN_SUCCESS", userInfo);

    }

    public ResponseEntity<?> createUser(CreateUserDTO createUserDTO) throws Exception {
        if(!createUserDTO.getEmail().contains(".") || !createUserDTO.getEmail().contains("@")){
            throw new InvalidEmailException(createUserDTO.getEmail());
        }

        if(checkIfExistsByUsername(createUserDTO.getEmail())){
            throw new EmailAlreadyExistsException(createUserDTO.getEmail());
        }

        if (createUserDTO.getPassword() == null || createUserDTO.getPassword().isEmpty()) {
            throw new InvalidPasswordException("Le mot de passe est invalide ou nul.");
        }

        User user = new User();
        user.setPassword(encoder.encode(createUserDTO.getPassword()));
        Role userRole = roleRepository.findByLibelleRole(ERole.ROLE_CLIENT)
                .orElseThrow(() -> new RoleNotFoundException(ERole.ROLE_CLIENT.toString()));
        user.setNom(createUserDTO.getNom());
        user.setPrenom(createUserDTO.getPrenom());
        user.setEmail(createUserDTO.getEmail());
        user.setDateNaissance(createUserDTO.getDateNaissance());
        user.setTelephone(createUserDTO.getTelephone());
        user.setRole(userRole);
        user.setFlagActif(false);
        userRepository.save(user);
        validationService.enregistrer(user);
        return buildSuccessResponse(HttpStatus.CREATED,"Utilisateur "+ createUserDTO.getEmail() +" créé avec succès. Un email d'activation vous a été envoyé.", "USER_CREATED",null);
    }

    public ResponseEntity<?> activation(Map<String, String> activation) {
        Validation validation = validationService.lireEnFontionDuCode(activation.get("code"));
        if(Instant.now().isAfter(validation.getExpiration())){
            throw new RuntimeException("Votre code a expire");
        }
        User utilisateurActiver = userRepository.findById(validation.getUser().getIdUser()).orElseThrow(()-> new RuntimeException("utilisateur inconnu"));
        utilisateurActiver.setFlagActif(true);
        userRepository.save(utilisateurActiver);
        return  buildSuccessResponse(HttpStatus.OK, "Compte activé avec succès", "ACCOUNT_ACTIVATED",null);
    }

    public List<String> getUserRoles(UserDetailsImpl user) {
        return user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
    }

    public String generateJwtCookie(UserDetailsImpl user, Map<String, Object> extraClaims) {
        return jwtUtils.generateAccessToken(user.getUsername(),extraClaims);
    }

    public Boolean checkIfExistsByUsername(String username) {
        Optional<User> optionalUser = userRepository.findByEmail(username);
        return optionalUser.isPresent();
    }

    private ResponseEntity<?> buildSuccessResponse(HttpStatus status, String message, String code, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);
        response.put("code", code);
        response.put("timestamp", Instant.now().toString());

        // Si des données supplémentaires sont fournies, les ajouter
        if (data != null) {
            response.put("data", data);
        }

        return ResponseEntity.status(status).body(response);
    }

}
