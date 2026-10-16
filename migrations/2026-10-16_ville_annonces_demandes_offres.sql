-- Ville des annonces, demandes et offres : une ville repertoriee (id_ville) OU un nom libre (ville_autre)
-- quand la ville n'est pas dans la base. Permet le filtre par ville cote agent.
ALTER TABLE annonce ADD COLUMN IF NOT EXISTS id_ville INTEGER;
ALTER TABLE annonce ADD COLUMN IF NOT EXISTS ville_autre VARCHAR(100);
ALTER TABLE demande ADD COLUMN IF NOT EXISTS id_ville INTEGER;
ALTER TABLE demande ADD COLUMN IF NOT EXISTS ville_autre VARCHAR(100);
ALTER TABLE offre ADD COLUMN IF NOT EXISTS id_ville INTEGER;
ALTER TABLE offre ADD COLUMN IF NOT EXISTS ville_autre VARCHAR(100);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_annonce_ville') THEN
        ALTER TABLE annonce ADD CONSTRAINT fk_annonce_ville FOREIGN KEY (id_ville) REFERENCES ville(id_ville);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_demande_ville') THEN
        ALTER TABLE demande ADD CONSTRAINT fk_demande_ville FOREIGN KEY (id_ville) REFERENCES ville(id_ville);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_offre_ville') THEN
        ALTER TABLE offre ADD CONSTRAINT fk_offre_ville FOREIGN KEY (id_ville) REFERENCES ville(id_ville);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_annonce_ville ON annonce(id_ville);
CREATE INDEX IF NOT EXISTS idx_demande_ville ON demande(id_ville);
CREATE INDEX IF NOT EXISTS idx_offre_ville ON offre(id_ville);

-- Rattachement des lignes existantes : le texte libre (localisation, localisation_souhaite, adresse) contient le nom
-- de la ville ou son debut (ex. "bobo" pour Bobo-Dioulasso, "ouaga" pour Ouagadougou ; 4 lettres minimum pour
-- eviter les faux positifs). Les lignes non reconnues restent sans ville ("Autres villes" dans le filtre).
UPDATE annonce t SET id_ville = v.id_ville FROM ville v
 WHERE t.id_ville IS NULL AND t.localisation IS NOT NULL AND length(split_part(v.nom_ville, '-', 1)) >= 4
   AND lower(t.localisation) LIKE '%' || left(lower(split_part(v.nom_ville, '-', 1)), 5) || '%';
UPDATE demande t SET id_ville = v.id_ville FROM ville v
 WHERE t.id_ville IS NULL AND t.localisation_souhaite IS NOT NULL AND length(split_part(v.nom_ville, '-', 1)) >= 4
   AND lower(t.localisation_souhaite) LIKE '%' || left(lower(split_part(v.nom_ville, '-', 1)), 5) || '%';
UPDATE offre t SET id_ville = v.id_ville FROM ville v
 WHERE t.id_ville IS NULL AND t.adresse IS NOT NULL AND length(split_part(v.nom_ville, '-', 1)) >= 4
   AND lower(t.adresse) LIKE '%' || left(lower(split_part(v.nom_ville, '-', 1)), 5) || '%';
