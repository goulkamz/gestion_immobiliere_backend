package com.immobilier.gestionImmobiliere.modules.journal.audit;

import com.immobilier.gestionImmobiliere.donnees.Model;
import com.immobilier.gestionImmobiliere.donnees.journal.model.JournalOperation;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.event.spi.*;
import org.hibernate.proxy.HibernateProxy;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Component
public class ImmoAuditEventListener implements PostInsertEventListener, PostUpdateEventListener, PostDeleteEventListener {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ImmoAuditEventListener.class);

    private final ApplicationEventPublisher publisher;

    public ImmoAuditEventListener(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void onPostInsert(PostInsertEvent event) {
        if (!estAuditable(event.getEntity())) return;

        Map<String, Object> nouvelle = toSafeMap(event.getPersister().getPropertyNames(), event.getState());
        publier("CREATE", event.getEntity(), idAsInteger(event.getId()), null, nouvelle);
    }

    @Override
    public void onPostUpdate(PostUpdateEvent event) {
        if (!estAuditable(event.getEntity())) return;

        String[] noms = event.getPersister().getPropertyNames();
        Object[] etatAvant = event.getOldState();
        Object[] etatApres = event.getState();

        Map<String, Object> ancienne = new HashMap<>();
        Map<String, Object> nouvelle = new HashMap<>();

        for (int i = 0; i < noms.length; i++) {
            // Ignore le bruit technique (updated_at change à chaque update, sans intérêt métier)
            if (noms[i].equals("updatedAt") || noms[i].equals("createdAt")) continue;

            Object avant = etatAvant != null ? etatAvant[i] : null;
            Object apres = etatApres != null ? etatApres[i] : null;
            if (!Objects.equals(safe(avant), safe(apres))) {
                ancienne.put(noms[i], safe(avant));
                nouvelle.put(noms[i], safe(apres));
            }
        }

        if (ancienne.isEmpty()) return;

        publier("UPDATE", event.getEntity(), idAsInteger(event.getId()), ancienne, nouvelle);
    }

    @Override
    public void onPostDelete(PostDeleteEvent event) {
        if (!estAuditable(event.getEntity())) return;

        Map<String, Object> ancienne = toSafeMap(event.getPersister().getPropertyNames(), event.getDeletedState());
        publier("DELETE", event.getEntity(), idAsInteger(event.getId()), ancienne, null);
    }

    private boolean estAuditable(Object entity) {
        return entity instanceof Model && !(entity instanceof JournalOperation);
    }

    private void publier(String action, Object entity, Integer entiteId,
                         Map<String, Object> ancienne, Map<String, Object> nouvelle) {
        Integer userId = AuditContextHolder.get();

        publisher.publishEvent(new AuditEvent(
                userId,
                action,
                entity.getClass().getSimpleName(),
                entiteId,
                ancienne,
                nouvelle,
                LocalDateTime.now()
        ));
    }

    private Map<String, Object> toSafeMap(String[] noms, Object[] etats) {
        Map<String, Object> map = new HashMap<>();
        if (etats == null) return map;
        for (int i = 0; i < noms.length; i++) {
            map.put(noms[i], safe(etats[i]));
        }
        return map;
    }

    private Object safe(Object valeur) {
        if (valeur instanceof HibernateProxy proxy) {
            Object id = proxy.getHibernateLazyInitializer().getIdentifier();
            return proxy.getHibernateLazyInitializer().getPersistentClass().getSimpleName() + "#" + id;
        }
        if (valeur instanceof PersistentCollection) {
            return "[collection non chargée]";
        }
        return valeur;
    }

    private Integer idAsInteger(Object id) {
        if (id == null) return null;
        if (id instanceof Integer i) return i;
        return Integer.valueOf(id.toString());
    }

    @Override
    public boolean requiresPostCommitHandling(org.hibernate.persister.entity.EntityPersister persister) {
        return false;
    }
}