ALTER TABLE YTELSER_AAP
    ADD COLUMN IF NOT EXISTS maksdato date,
    DROP COLUMN IF EXISTS sakstatus,
    --ADD COLUMN IF NOT EXISTS sakstatus text,
    DROP COLUMN IF EXISTS opphorsaarsak;

