package com.immobilier.gestionImmobiliere.modules.user.apis;

import com.immobilier.gestionImmobiliere.modules.user.dto.requests.AuthenticateDTO;
import com.immobilier.gestionImmobiliere.modules.user.dto.requests.CreateUserDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/auth")
public interface AuthentificationAPI {

        @PostMapping("/signin")
        ResponseEntity<?> authenticateUser(@Valid @RequestBody AuthenticateDTO authenticateDTO);

        @PostMapping("/signup")
        ResponseEntity<?> createUser(@Valid @RequestBody CreateUserDTO createUserDTO);

    }
