package com.immobilier.gestionImmobiliere.modules.journal.audit;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.springframework.stereotype.Component;

@Component
public class AuditListenerRegistrar {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AuditListenerRegistrar.class);

    private final EntityManagerFactory entityManagerFactory;
    private final ImmoAuditEventListener auditListener;

    public AuditListenerRegistrar(EntityManagerFactory entityManagerFactory, ImmoAuditEventListener auditListener) {
        this.entityManagerFactory = entityManagerFactory;
        this.auditListener = auditListener;
    }

    @PostConstruct
    public void enregistrer() {
        SessionFactoryImplementor sessionFactory = entityManagerFactory.unwrap(SessionFactoryImplementor.class);
        EventListenerRegistry registry = sessionFactory.getServiceRegistry().getService(EventListenerRegistry.class);

        assert registry != null;
        registry.appendListeners(EventType.POST_INSERT, auditListener);
        registry.appendListeners(EventType.POST_UPDATE, auditListener);
        registry.appendListeners(EventType.POST_DELETE, auditListener);
        log.info("✅ ImmoAuditEventListener enregistré sur Hibernate (POST_INSERT/UPDATE/DELETE)");
    }
}