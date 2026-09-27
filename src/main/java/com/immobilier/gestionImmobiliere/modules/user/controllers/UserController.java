package com.immobilier.gestionImmobiliere.modules.user.controllers;

import com.immobilier.gestionImmobiliere.modules.user.apis.AuthentificationAPI;
import com.immobilier.gestionImmobiliere.modules.user.dto.requests.*;
import com.immobilier.gestionImmobiliere.modules.user.services.PasswordResetService;
import com.immobilier.gestionImmobiliere.modules.user.services.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController implements AuthentificationAPI {

    private final UserService userService;
    private final PasswordResetService passwordResetService;

    public UserController(UserService userService, PasswordResetService passwordResetService) {
        this.userService = userService;
        this.passwordResetService = passwordResetService;
    }

    @Override
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody AuthenticateDTO authenticateDTO, HttpServletRequest request, HttpServletResponse response) {
        return userService.authenticateUser(authenticateDTO,request,response);
    }

    @Override
    public ResponseEntity<?> createUser(@Valid @RequestBody CreateUserDTO createUserDTO, HttpServletRequest request) throws Exception {
           return userService.createUser(createUserDTO, request);
    }

    @Override
    public ResponseEntity<?> activateUser(@Valid @RequestBody ActivateUserDTO activationCode, HttpServletRequest request) {
        return userService.activation(activationCode, request);
    }

    @Override
    public ResponseEntity<?> resendCode(@Valid @RequestBody ResendCodeEmailDTO email, HttpServletRequest request) {
       return userService.resendCode(email, request);
    }

    @Override
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequestDTO forgotPasswordRequestDTO) {
        return passwordResetService.forgotPassword(forgotPasswordRequestDTO.getEmail());
    }

    @Override
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequestDTO resetPasswordRequestDTO, HttpServletRequest request) {
        return passwordResetService.resetPassword(resetPasswordRequestDTO.getCode(), resetPasswordRequestDTO.getNewPassword(), request);
    }

    @Override
    public ResponseEntity<?> resendResetToken(@Valid @RequestBody ForgotPasswordRequestDTO forgotPasswordRequestDTO, HttpServletRequest request) {
        return passwordResetService.resendResetToken(forgotPasswordRequestDTO.getEmail(), request);
    }

    @Override
    public ResponseEntity<?> refreshToken(HttpServletRequest request, HttpServletResponse response) {
        return userService.refreshToken(request, response);
    }

    @Override
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        return userService.logout(request, response);
    }

    @Override
    public ResponseEntity<?> logoutAllDevices(HttpServletRequest request, HttpServletResponse response) {
        return userService.logoutAllDevices(request, response);
    }
}
