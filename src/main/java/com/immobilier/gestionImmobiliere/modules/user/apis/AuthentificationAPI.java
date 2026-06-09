package com.immobilier.gestionImmobiliere.modules.user.apis;

import com.immobilier.gestionImmobiliere.modules.user.dto.requests.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

@RequestMapping("/auth")
public interface AuthentificationAPI {

        @PostMapping("/signin")
        ResponseEntity<?> authenticateUser(@Valid AuthenticateDTO authenticateDTO, HttpServletRequest request, HttpServletResponse response);

        @PostMapping("/signup")
        ResponseEntity<?> createUser(@Valid CreateUserDTO createUserDTO) throws Exception;

        @PostMapping("/activation")
        ResponseEntity<?> activateUser(@Valid ActivateUserDTO activateUserDTO);

        @PostMapping("/resend-code")
        ResponseEntity<?> resendCode(@Valid ResendCodeEmailDTO email);

        @PostMapping("/forgot-password")
        ResponseEntity<?> forgotPassword(@Valid ForgotPasswordRequestDTO forgotPasswordRequestDTO);

        @PostMapping("/reset-password")
        ResponseEntity<?> resetPassword(@Valid ResetPasswordRequestDTO resetPasswordRequestDTO);

        @PostMapping("/forgot-password/resend")
        ResponseEntity<?> resendResetToken(@Valid ForgotPasswordRequestDTO forgotPasswordRequestDTO);

}
