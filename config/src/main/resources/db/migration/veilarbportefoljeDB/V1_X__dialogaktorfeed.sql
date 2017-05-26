-- V1_X avventer merging
ALTER TABLE metadata ADD (dialogaktor_sist_oppdatert TIMESTAMP DEFAULT TO_TIMESTAMP('1970-01-01 00:00:00', 'YYYY-MM-DD HH24:MI:SS') NOT NULL);
ALTER TABLE brukerdata ADD (venterPaSvarFraBruker TIMESTAMP, venterPaSvarFraNav TIMESTAMP);