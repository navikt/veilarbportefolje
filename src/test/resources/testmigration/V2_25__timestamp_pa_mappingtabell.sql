ALTER TABLE AKTOERID_TO_PERSONID ADD OPPRETTET_TIDSPUNKT TIMESTAMP;
ALTER TABLE AKTOERID_TO_PERSONID ALTER COLUMN OPPRETTET_TIDSPUNKT SET DEFAULT CURRENT_TIMESTAMP;