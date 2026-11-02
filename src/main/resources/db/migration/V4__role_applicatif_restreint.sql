/*==============================================================*/
/* Migration Flyway V4 : role applicatif a droits restreints     */
/* L'application ne se connecte plus avec le super-utilisateur   */
/* postgres mais avec gestimmo_app : lecture et ecriture des     */
/* donnees uniquement. Pas de creation ni de suppression de      */
/* table, pas de creation de compte, pas de TRUNCATE, pas       */
/* d'acces a l'historique des migrations.                        */
/* Flyway continue de s'executer avec le compte proprietaire.    */
/* Le mot de passe vient du placeholder apppassword, alimente   */
/* par APP_DB_PASSWORD (.env) : jamais ecrit dans ce fichier.    */
/*==============================================================*/

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'gestimmo_app') THEN
        CREATE ROLE gestimmo_app LOGIN PASSWORD '${apppassword}'
            NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION;
    ELSE
        ALTER ROLE gestimmo_app LOGIN PASSWORD '${apppassword}'
            NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION;
    END IF;
    EXECUTE format('GRANT CONNECT ON DATABASE %I TO gestimmo_app', current_database());
END
$$;

GRANT USAGE ON SCHEMA public TO gestimmo_app;
REVOKE CREATE ON SCHEMA public FROM gestimmo_app;

GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO gestimmo_app;
GRANT USAGE, SELECT, UPDATE ON ALL SEQUENCES IN SCHEMA public TO gestimmo_app;

-- L'historique des migrations n'a pas a etre lisible ni modifiable par l'application
REVOKE ALL ON flyway_schema_history FROM gestimmo_app;

-- Les tables et sequences creees par les migrations suivantes (meme compte proprietaire) sont accessibles d'office
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO gestimmo_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT USAGE, SELECT, UPDATE ON SEQUENCES TO gestimmo_app;
