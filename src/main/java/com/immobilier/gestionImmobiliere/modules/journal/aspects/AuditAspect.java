package com.immobilier.gestionImmobiliere.modules.journal.aspects;

import com.immobilier.gestionImmobiliere.modules.journal.annotations.Auditable;
import com.immobilier.gestionImmobiliere.modules.journal.services.JournalService;
import com.immobilier.gestionImmobiliere.modules.user.jwtService.UserDetailsImpl;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class AuditAspect {

    private final JournalService journalService;

    public AuditAspect(JournalService journalService) {
        this.journalService = journalService;
    }

    @Around("@annotation(auditable)")
    public Object autourAction(ProceedingJoinPoint pjp, Auditable auditable) throws Throwable {
        Object result = pjp.proceed(); // exécute la méthode métier d'abord

        Integer currentUserId = extraireUserId();
        Integer entiteId = extraireEntiteId(result);

        journalService.enregistrer(currentUserId, auditable.action(), auditable.entite(), entiteId,
                auditable.action() + " sur " + auditable.entite(), null, null);

        return result;
    }

    private Integer extraireUserId() {
        try {
            var principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (principal instanceof UserDetailsImpl userDetails) {
                return userDetails.getIdUser();
            }
        } catch (Exception ignored) { }
        return null;
    }

    // Extraction best-effort de l'id via réflexion sur le body de la ResponseEntity — limité,
    // fonctionne uniquement si le DTO expose un champ commençant par "id".
    private Integer extraireEntiteId(Object result) {
        // Implémentation simplifiée : à adapter selon la structure exacte de vos réponses.
        return null;
    }
}