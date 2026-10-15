-- Un seul document valide par type et par entite (ou par periode pour le rapport mensuel).
-- Empeche les doublons lorsque deux demandes generent le meme document en parallele.
-- Les documents marques supprimes (is_deleted) sont exclus, ce qui laisse la place a une regeneration.
CREATE UNIQUE INDEX IF NOT EXISTS uq_document_entite
    ON document (type_document, entite_type, entite_id)
    WHERE entite_id IS NOT NULL AND is_deleted IS NOT TRUE;

CREATE UNIQUE INDEX IF NOT EXISTS uq_document_periode
    ON document (type_document, periode_mois)
    WHERE periode_mois IS NOT NULL AND is_deleted IS NOT TRUE;
