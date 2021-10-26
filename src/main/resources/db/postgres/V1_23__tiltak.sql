CREATE TABLE BRUKERTILTAK
(
    AKTIVITETID VARCHAR(25) NOT NULL,
    AKTOERID    VARCHAR(20),
    PERSONID    VARCHAR(20),
    TILTAKSKODE VARCHAR(10),
    TILDATO     TIMESTAMP,
    FRADATO     TIMESTAMP,

    PRIMARY KEY (AKTIVITETID)
);

CREATE TABLE TILTAKKODEVERKET
(
    KODE  VARCHAR(10),
    VERDI VARCHAR(80),

    PRIMARY KEY (KODE)
);

CREATE INDEX PERSONID_BRUKERTILTAK_V2_IDX ON BRUKERTILTAK (AKTOERID);
