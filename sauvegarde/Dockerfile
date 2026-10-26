# Service de sauvegarde : pg_dump (PostgreSQL 17) + mc (client MinIO) + planificateur quotidien.
FROM minio/mc:latest AS mc

FROM postgres:17
COPY --from=mc /usr/bin/mc /usr/local/bin/mc
COPY planificateur.sh sauvegarde.sh verifier-restauration.sh restaurer.sh /usr/local/bin/
RUN sed -i 's/\r$//' /usr/local/bin/*.sh && chmod +x /usr/local/bin/*.sh
ENTRYPOINT ["/usr/local/bin/planificateur.sh"]
