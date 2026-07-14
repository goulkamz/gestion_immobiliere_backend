package com.immobilier.gestionImmobiliere.modules.journal.audit;

import java.time.LocalDateTime;
import java.util.Map;

public record AuditEvent(
        Integer idUser,
        String action,       // CREATE, UPDATE, DELETE
        String entite,
        Integer entiteId,
        Map<String, Object> ancienneValeur,
        Map<String, Object> nouvelleValeur,
        LocalDateTime dateEvenement
) {}