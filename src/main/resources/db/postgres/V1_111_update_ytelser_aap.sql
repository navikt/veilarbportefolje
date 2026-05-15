ALTER TABLE YTELSER_AAP
    ADD COLUMN maksdato date,
    DROP COLUMN saksid;
-- oppdater hva som kan være null og ikke i resten av tabellen, og om maksdato skal være null eller ikke
