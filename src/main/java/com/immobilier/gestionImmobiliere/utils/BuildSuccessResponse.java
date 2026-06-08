package com.immobilier.gestionImmobiliere.utils;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class BuildSuccessResponse {
    public static ResponseEntity<?> buildSuccessResponse(HttpStatus status, String message, String code, Object data) {
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
