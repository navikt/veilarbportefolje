ALTER TABLE AKTOERID_TO_PERSONID ADD OPPRETTET_TIDSPUNKT TIMESTAMP;
ALTER TABLE AKTOERID_TO_PERSONID MODIFY(OPPRETTET_TIDSPUNKT DEFAULT CURRENT_TIMESTAMP);
