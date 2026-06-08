package com.immobilier.gestionImmobiliere.modules.user.controllers;

import com.immobilier.gestionImmobiliere.modules.user.apis.AuthentificationAPI;
import com.immobilier.gestionImmobiliere.modules.user.dto.requests.*;
import com.immobilier.gestionImmobiliere.modules.user.services.PasswordResetService;
import com.immobilier.gestionImmobiliere.modules.user.services.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class UserController implements AuthentificationAPI {

    private final UserService userService;
    private final PasswordResetService passwordResetService;

    public UserController(UserService userService, PasswordResetService passwordResetService) {
        this.userService = userService;
        this.passwordResetService = passwordResetService;
    }

    @Override
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody AuthenticateDTO authenticateDTO) {
        return userService.authenticateUser(authenticateDTO);
    }

    @Override
    public ResponseEntity<?> createUser(@Valid @RequestBody CreateUserDTO createUserDTO) throws Exception {
           return userService.createUser(createUserDTO);
    }

    @Override
    public ResponseEntity<?> activateUser(@Valid @RequestBody ActivateUserDTO activationCode) {
        return userService.activation(activationCode);
    }

    @Override
    public ResponseEntity<?> resendCode(@Valid @RequestBody ResendCodeEmailDTO email) {
       return userService.resendCode(email);
    }

    @Override
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequestDTO forgotPasswordRequestDTO) {
        return passwordResetService.forgotPassword(forgotPasswordRequestDTO.getEmail());
    }

    @Override
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequestDTO resetPasswordRequestDTO) {
        return passwordResetService.resetPassword(resetPasswordRequestDTO.getCode(), resetPasswordRequestDTO.getNewPassword());
    }

    @Override
    public ResponseEntity<?> resendResetToken(@Valid @RequestBody ForgotPasswordRequestDTO forgotPasswordRequestDTO) {
        return passwordResetService.resendResetToken(forgotPasswordRequestDTO.getEmail());
    }
}
