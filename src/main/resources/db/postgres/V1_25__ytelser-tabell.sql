CREATE TABLE YTELSESVEDTAK
(
    VEDTAKSID               VARCHAR(20) NOT NULL,
    AKTORID                 VARCHAR(20) NOT NULL,
    PERSONID                VARCHAR(20),
    YTELSESTYPE             VARCHAR(15),
    SAKSID                  VARCHAR(30),
    SAKSTYPEKODE            VARCHAR(10),
    RETTIGHETSTYPEKODE      VARCHAR(10),
    STARTDATO               TIMESTAMP,
    UTLOPSDATO              TIMESTAMP,
    ANTALLUKERIGJEN         INTEGER,
    ANTALLPERMITTERINGSUKER INTEGER,
    ANTALLUKERIGJENUNNTAK   INTEGER,
    PRIMARY KEY (VEDTAKSID)
);

CREATE INDEX AKTORID_YTELSER_IDX ON YTELSESVEDTAK(AKTORID);
