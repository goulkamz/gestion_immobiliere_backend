/*==============================================================*/
/* Migration Flyway V12 : contestation d'une depense par le      */
/* bailleur et retablissement d'une creance abandonnee           */
/*   - le bailleur signale qu'il conteste une depense ; l'agence */
/*     repond et cloture (rien n'est annule automatiquement)     */
/*   - l'admin peut retablir une creance abandonnee par erreur   */
/*==============================================================*/

ALTER TABLE depense_bien ADD COLUMN motif_contestation VARCHAR(500);
ALTER TABLE depense_bien ADD COLUMN date_contestation TIMESTAMP(6);
ALTER TABLE depense_bien ADD COLUMN reponse_contestation VARCHAR(500);
ALTER TABLE depense_bien ADD COLUMN date_reponse_contestation TIMESTAMP(6);
ALTER TABLE depense_bien ADD COLUMN user_reponse_contestation INTEGER;

ALTER TABLE depense_bien ADD COLUMN motif_retablissement VARCHAR(254);
ALTER TABLE depense_bien ADD COLUMN date_retablissement TIMESTAMP(6);
ALTER TABLE depense_bien ADD COLUMN user_retablissement INTEGER;
