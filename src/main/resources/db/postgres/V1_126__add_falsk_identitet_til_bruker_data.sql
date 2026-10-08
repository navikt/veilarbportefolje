ALTER TABLE bruker_data
    ADD COLUMN falsk_identitet BOOLEAN;

ALTER TABLE bruker_data
    ADD CONSTRAINT chk_falsk_identitet CHECK (falsk_identitet IS NULL OR falsk_identitet IS TRUE);

COMMENT ON COLUMN bruker_data.falsk_identitet IS
'Manuelt vedlikeholdt av team Obo. TRUE: informasjon om falsk identitet er mottatt fra PDL. NULL: informasjon er ikke registrert på denne raden. Markeringen slettes sammen med raden.';
