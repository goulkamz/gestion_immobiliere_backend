package com.immobilier.gestionImmobiliere.modules.journal.audit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.sql.Timestamp;

@Component
public class AuditEventPersister {

    private static final Logger log = LoggerFactory.getLogger(AuditEventPersister.class);

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public AuditEventPersister(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAuditEvent(AuditEvent event) {
        try {
            jdbcTemplate.update("""
                    INSERT INTO journal_operation
                        (id_journal, id_user, action, entite, ligne_entite,
                         description, date_action, ancien_contenu, nouveau_contenu,
                         created_at, updated_at, is_deleted)
                    VALUES (nextval('seq_journal'), ?, ?, ?, ?, ?, ?,?::jsonb, ?::jsonb, NOW(), NOW(), FALSE)
                    """,
                    event.idUser(),
                    event.action(),
                    event.entite(),
                    event.entiteId(),
                    event.action() + " sur " + event.entite() + " id " + event.entiteId(),
                    Timestamp.valueOf(event.dateEvenement()),
                    versJson(event.ancienneValeur()),
                    versJson(event.nouvelleValeur())
            );
        } catch (Exception e) {
            // L'audit ne doit JAMAIS faire échouer la requête métier déjà commitée.
            log.error("Échec écriture journal_operation entite={} entiteId={}", event.entite(), event.entiteId(), e);
        }
    }

    private String versJson(Object valeur) {
        if (valeur == null) return null;
        try {
            JsonNode node = objectMapper.valueToTree(valeur);
            if (node.isObject()) {
                ObjectNode objectNode = (ObjectNode) node;
                objectNode.remove("password");
                objectNode.remove("motDePasse");
                objectNode.remove("accessToken");
                objectNode.remove("refreshToken");
            }
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            log.warn("Impossible de sérialiser la valeur d'audit : {}", valeur.getClass().getName(), e);
            return "{\"erreur\":\"impossible_de_serialiser\"}";
        }
    }
}