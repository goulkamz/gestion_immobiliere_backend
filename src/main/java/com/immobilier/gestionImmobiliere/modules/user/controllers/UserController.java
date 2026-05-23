package com.immobilier.gestionImmobiliere.modules.user.controllers;

import com.immobilier.gestionImmobiliere.modules.user.apis.AuthentificationAPI;
import com.immobilier.gestionImmobiliere.modules.user.dto.requests.AuthenticateDTO;
import com.immobilier.gestionImmobiliere.modules.user.dto.requests.CreateUserDTO;
import com.immobilier.gestionImmobiliere.modules.user.dto.responses.UserInfoDTO;
import com.immobilier.gestionImmobiliere.modules.user.jwtService.UserDetailsImpl;
import com.immobilier.gestionImmobiliere.modules.user.services.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserController implements AuthentificationAPI {

    @Autowired
    UserService userService;

    @Override
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody AuthenticateDTO authenticateDTO) {

        UserDetailsImpl user = userService.authenticateUser(authenticateDTO);
        Map<String, Object> extraClaims = new HashMap<>();
        String jwtCookie = userService.generateJwtCookie(user,extraClaims);
        List<String> roles = userService.getUserRoles(user);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, jwtCookie) // JWT ajouté en cookie
                .body(UserInfoDTO.builder()
                        .username(user.getUsername())
                        .roles(roles)
                        .build());


    }

    @Override
    public ResponseEntity<?> createUser(@Valid @RequestBody CreateUserDTO createUserDTO) {
        Map<String, Object> response = new HashMap<>();
        try {
            if(userService.checkIfExistsByUsername((createUserDTO.getEmail())))
                return ResponseEntity.badRequest().body("Nom d'utilisateur déjà utilisé");

            userService.createUser(createUserDTO);

            response.put("success", true);
            response.put("message", "Utilisateur enregistré");
            return ResponseEntity.ok(response);
        } catch(Exception ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }
}
