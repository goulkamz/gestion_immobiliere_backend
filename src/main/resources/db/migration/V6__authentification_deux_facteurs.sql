/*==============================================================*/
/* Migration Flyway V6 : double authentification (TOTP)          */
/* Facultative, ouverte au personnel (agents, admin...).         */
/* Le secret est chiffre (AES-GCM) par l'application ; les codes */
/* de secours sont stockes haches (SHA-256), a usage unique.     */
/*==============================================================*/

ALTER TABLE users ADD COLUMN totp_secret VARCHAR(512);
ALTER TABLE users ADD COLUMN totp_actif BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE codes_secours_2fa (
    id_code BIGSERIAL PRIMARY KEY,
    id_user INTEGER NOT NULL,
    code_hash VARCHAR(64) NOT NULL,
    utilise_le TIMESTAMP(6),
    created_at TIMESTAMP(6) NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_codes_secours_user FOREIGN KEY (id_user) REFERENCES users(id_user)
);

CREATE INDEX idx_codes_secours_user ON codes_secours_2fa(id_user);
