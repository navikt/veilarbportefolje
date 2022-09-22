ALTER TABLE BRUKER_DATA
    ADD sikkerhetstiltak_type VARCHAR(20);
ALTER TABLE BRUKER_DATA
    ADD sikkerhetstiltak_beskrivelse VARCHAR(255);
ALTER TABLE BRUKER_DATA
    ADD sikkerhetstiltak_gyldigfra DATE;
ALTER TABLE BRUKER_DATA
    ADD sikkerhetstiltak_gyldigtil DATE;
