package com.immobilier.gestionImmobiliere.modules.user.services;

import com.immobilier.gestionImmobiliere.donnees.roles.model.ERole;
import com.immobilier.gestionImmobiliere.donnees.roles.model.Role;
import com.immobilier.gestionImmobiliere.donnees.roles.repository.RoleRepository;
import com.immobilier.gestionImmobiliere.donnees.user.model.User;
import com.immobilier.gestionImmobiliere.donnees.user.repository.UserRepository;
import com.immobilier.gestionImmobiliere.modules.user.dto.requests.AuthenticateDTO;
import com.immobilier.gestionImmobiliere.modules.user.dto.requests.CreateUserDTO;
import com.immobilier.gestionImmobiliere.modules.user.jwt.JwtUtils;
import com.immobilier.gestionImmobiliere.modules.user.jwtService.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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

    public PasswordEncoder cryptPassword(){
        return this.encoder;
    }

    public UserDetailsImpl authenticateUser(AuthenticateDTO authenticateDTO) {
        System.out.println("on est dans login");
        System.out.println("données :" +authenticateDTO);

        Authentication authentication = authenticationManager
                .authenticate(new UsernamePasswordAuthenticationToken(authenticateDTO.getUsername(), authenticateDTO.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        return (UserDetailsImpl) authentication.getPrincipal();
    }

    public void createUser(CreateUserDTO createUserDTO) throws Exception {
        User user = new User();
        user.setPassword(encoder.encode(createUserDTO.getPassword()));
        Role userRole = roleRepository.findByName(ERole.ROLE_CLIENT)
                .orElseThrow(() -> new Exception("Error: Role is not found."));
        user.setNom(createUserDTO.getNom());
        user.setPrenom(createUserDTO.getPrenom());
        user.setEmail(createUserDTO.getEmail());
        user.setDateNaissance(createUserDTO.getDateNaissance());
        user.setTelephone(createUserDTO.getTelephone());
        user.setRole(userRole);
        userRepository.save(user);

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
        System.out.println("on dans check");
        Optional<User> optionalUser = userRepository.findByEmail(username);
        return optionalUser.isPresent();
    }
}
