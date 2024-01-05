CREATE INDEX IF NOT EXISTS idx_bruker_identer_gruppe
    ON bruker_identer (gruppe);

CREATE INDEX IF NOT EXISTS idx_bruker_identer_historisk
    ON bruker_identer (historisk);