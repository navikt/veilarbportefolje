CREATE TABLE BRUKERTILTAK_V2
(
    AKTIVITETID VARCHAR(25) NOT NULL,
    AKTOERID    VARCHAR(20),
    TILTAKSKODE VARCHAR(10),
    TILDATO     TIMESTAMP,
    FRADATO     TIMESTAMP,
    VERSION     BIGINT,

    PRIMARY KEY (AKTIVITETID)
);
CREATE INDEX BRUKERTILTAK_V2_AKTOERID_INDEX ON BRUKERTILTAK_V2 (AKTOERID);
