ALTER TABLE YTELSER_AAP
    ADD COLUMN IF NOT EXISTS maksdato date,
    ADD COLUMN IF NOT EXISTS sakstatus varchar(100),
    DROP COLUMN IF EXISTS opphorsaarsak;

