package com.immobilier.gestionImmobiliere.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.*;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {


    // ========== EXCEPTIONS D'AUTHENTIFICATION ==========

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<?> handleDisabledException(DisabledException ex) {
        return buildErrorResponse(HttpStatus.FORBIDDEN, "Votre compte est désactivé. Veuillez contacter l'administrateur.", "ACCOUNT_DISABLED", ex);
    }

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<?> handleLockedException(LockedException ex) {
        return buildErrorResponse(HttpStatus.FORBIDDEN, "Votre compte est verrouillé. Veuillez réessayer plus tard.", "ACCOUNT_LOCKED", ex);
    }

    @ExceptionHandler(AccountExpiredException.class)
    public ResponseEntity<?> handleAccountExpiredException(AccountExpiredException ex) {
        return buildErrorResponse(HttpStatus.FORBIDDEN, "Votre compte a expiré. Veuillez contacter l'administrateur.", "ACCOUNT_EXPIRED", ex);
    }

    @ExceptionHandler(CredentialsExpiredException.class)
    public ResponseEntity<?> handleCredentialsExpiredException(CredentialsExpiredException ex) {
        return buildErrorResponse(HttpStatus.FORBIDDEN, "Votre mot de passe a expiré. Veuillez le réinitialiser.", "CREDENTIALS_EXPIRED", ex);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<?> handleBadCredentialsException(BadCredentialsException ex) {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Email ou mot de passe invalide", "INVALID_CREDENTIALS", ex);
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<?> handleUsernameNotFoundException(UsernameNotFoundException ex) {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Aucun compte associé à cet email", "USER_NOT_FOUND", ex);
    }

    @ExceptionHandler(InternalAuthenticationServiceException.class)
    public ResponseEntity<?> handleInternalAuthenticationServiceException(InternalAuthenticationServiceException ex) {
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Une erreur technique est survenue. Veuillez réessayer.", "INTERNAL_AUTH_ERROR", ex);
    }

    @ExceptionHandler(AuthenticationServiceException.class)
    public ResponseEntity<?> handleAuthenticationServiceException(AuthenticationServiceException ex) {
        return buildErrorResponse(HttpStatus.SERVICE_UNAVAILABLE, "Le service d'authentification est temporairement indisponible.", "AUTH_SERVICE_UNAVAILABLE", ex);
    }

    @ExceptionHandler(AuthenticationCredentialsNotFoundException.class)
    public ResponseEntity<?> handleAuthenticationCredentialsNotFoundException(AuthenticationCredentialsNotFoundException ex) {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Vous devez vous connecter pour accéder à cette ressource.", "NOT_AUTHENTICATED", ex);
    }

    @ExceptionHandler(InsufficientAuthenticationException.class)
    public ResponseEntity<?> handleInsufficientAuthenticationException(InsufficientAuthenticationException ex) {
        return buildErrorResponse(HttpStatus.FORBIDDEN, "Vous n'avez pas les droits nécessaires pour accéder à cette ressource.", "INSUFFICIENT_PRIVILEGES", ex);
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<?> handleSecurityException(SecurityException ex) {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, ex.getMessage(), "INVALID_REFRESH_TOKEN", ex);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );
        if ("dev".equals(System.getProperty("spring.profiles.active"))) {
            ex.printStackTrace();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", "Erreur de validation des champs");
        response.put("code", "VALIDATION_ERROR");
        response.put("timestamp", Instant.now().toString());
        response.put("errors", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgument(IllegalArgumentException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), "INVALID_ARGUMENT",ex);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleRuntime(RuntimeException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), "RUNTIME_ERROR",ex);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGenericException(Exception ex) {
        ex.printStackTrace();
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Une erreur interne est survenue", "INTERNAL_ERROR",ex);
    }


    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<?> handleEmailExists(EmailAlreadyExistsException ex) {
        return  buildErrorResponse(HttpStatus.CONFLICT,ex.getMessage(),"EMAIL_ALREADY_EXISTS",ex);
    }

    @ExceptionHandler(InvalidEmailException.class)
    public ResponseEntity<?> handleInvalidEmail(InvalidEmailException ex) {
        return  buildErrorResponse(HttpStatus.BAD_REQUEST,ex.getMessage(),"INVALID_EMAIL",ex);
    }

    @ExceptionHandler(InvalidPasswordException.class)
    public ResponseEntity<?> handleInvalidPassword(InvalidPasswordException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), "INVALID_PASSWORD",ex);
    }

    @ExceptionHandler(RoleNotFoundException.class)
    public ResponseEntity<?> handleRoleNotFound(RoleNotFoundException ex) {
        return  buildErrorResponse(HttpStatus.BAD_REQUEST,ex.getMessage(),"ROLE_NOT_FOUND",ex);
    }



    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<?> handleInvalidToken(InvalidTokenException e) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST,e.getMessage(),"INVALID_TOKEN", e );
    }

        @ExceptionHandler(TokenAlreadyUsedException.class)
        public ResponseEntity<?> handleTokenUsed(TokenAlreadyUsedException e) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST,e.getMessage(),"TOKEN_ALREADY_USED", e);
        }

        @ExceptionHandler(TokenExpiredException.class)
        public ResponseEntity<?> handleTokenExpired(TokenExpiredException e) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST,e.getMessage(),"TOKEN_EXPIRED", e);
        }

        @ExceptionHandler(SamePasswordException.class)
        public ResponseEntity<?> handleSamePassword(SamePasswordException e) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST,e.getMessage(),"SAME_PASSWORD",e);
        }


    @ExceptionHandler(CodeAlreadyExistsException.class)
    public ResponseEntity<?> handleCodeExists(CodeAlreadyExistsException ex) {
        return buildErrorResponse(HttpStatus.CONFLICT, ex.getMessage(), "CODE_ALREADY_EXISTS", ex);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<?> handleResourceNotFound(ResourceNotFoundException ex) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), ex.getCode(), ex);
    }

    @ExceptionHandler(InvalidStatutTransitionException.class)
    public ResponseEntity<?> handleInvalidTransition(InvalidStatutTransitionException ex) {
        return buildErrorResponse(HttpStatus.CONFLICT, ex.getMessage(), "INVALID_STATUT_TRANSITION", ex);
    }

    @ExceptionHandler(MandatActifExistantException.class)
    public ResponseEntity<?> handleMandatActif(MandatActifExistantException ex) {
        return buildErrorResponse(HttpStatus.CONFLICT, ex.getMessage(), "MANDAT_ACTIF_EXISTANT", ex);
    }

    @ExceptionHandler(MaisonIndisponibleException.class)
    public ResponseEntity<?> handleMaisonIndisponible(MaisonIndisponibleException ex) {
        return buildErrorResponse(HttpStatus.CONFLICT, ex.getMessage(), "MAISON_INDISPONIBLE", ex);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> handleAccessDenied(AccessDeniedException ex) {
        return buildErrorResponse(HttpStatus.FORBIDDEN, "Vous n'êtes pas autorisé à accéder à cette ressource.", "ACCESS_DENIED", ex);
    }

    @ExceptionHandler(EcheanceDejaPayeeException.class)
    public ResponseEntity<?> handleEcheanceDejaPayee(EcheanceDejaPayeeException ex) {
        return buildErrorResponse(HttpStatus.CONFLICT, ex.getMessage(), "ECHEANCE_DEJA_PAYEE", ex);
    }

    @ExceptionHandler(MontantPaiementInvalideException.class)
    public ResponseEntity<?> handleMontantInvalide(MontantPaiementInvalideException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), "MONTANT_PAIEMENT_INVALIDE", ex);
    }

    @ExceptionHandler(DateExpirationInvalideException.class)
    public ResponseEntity<?> handleDateExpirationInvalide(DateExpirationInvalideException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), "DATE_EXPIRATION_INVALIDE", ex);
    }

    @ExceptionHandler(ConflitReservationException.class)
    public ResponseEntity<?> handleConflitReservation(ConflitReservationException ex) {
        return buildErrorResponse(HttpStatus.CONFLICT, ex.getMessage(), "CONFLIT_RESERVATION", ex);
    }

    @ExceptionHandler(CannotDeactivateSelfException.class)
    public ResponseEntity<?> handleCannotDeactivateSelf(CannotDeactivateSelfException ex) {
        return buildErrorResponse(HttpStatus.FORBIDDEN, ex.getMessage(), "CANNOT_DEACTIVATE_SELF", ex);
    }

    @ExceptionHandler(FormatMediaInvalideException.class)
    public ResponseEntity<?> handleFormatInvalide(FormatMediaInvalideException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), "FORMAT_MEDIA_INVALIDE", ex);
    }

    @ExceptionHandler(TailleMediaExcessiveException.class)
    public ResponseEntity<?> handleTailleExcessive(TailleMediaExcessiveException ex) {
        return buildErrorResponse(HttpStatus.PAYLOAD_TOO_LARGE, ex.getMessage(), "TAILLE_MEDIA_EXCESSIVE", ex);
    }

    @ExceptionHandler(MediaStorageException.class)
    public ResponseEntity<?> handleStorageError(MediaStorageException ex) {
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors du traitement du fichier", "MEDIA_STORAGE_ERROR", ex);
    }


    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<?> handleTooManyRequests(TooManyRequestsException ex) {
        return buildErrorResponse(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage(), "TOO_MANY_REQUESTS", ex);
    }


    private ResponseEntity<?> buildErrorResponse(HttpStatus status, String message, String code,Exception ex) {
        // Afficher la stack trace pour le debug
        if (ex != null && !(ex instanceof IllegalArgumentException) && !(ex instanceof BadCredentialsException) && "dev".equals(System.getProperty("spring.profiles.active"))) {
            ex.printStackTrace();
        }
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        response.put("code", code);
        response.put("timestamp", Instant.now().toString());

        // En développement, ajouter les détails de l'erreur
        if (ex != null && System.getProperty("spring.profiles.active", "").equals("dev")) {
            response.put("debug_message", ex.getMessage());
            response.put("exception_type", ex.getClass().getSimpleName());
        }

        return ResponseEntity.status(status).body(response);
    }
}