DROP VIEW BRUKER;
DROP INDEX OPPFOLGING_DATA_IDX;
DROP TABlE OPPFOLGING_DATA;
DROP TABLE OPPFOLGINGSBRUKER_ARENA;

ALTER TABLE DIALOG DROP SIST_OPPDATERT;

CREATE TABLE OPPFOLGING_DATA
(
    AKTOERID        VARCHAR(20) NOT NULL,
    VEILEDERID      VARCHAR(20),
    OPPFOLGING      boolean DEFAULT false NOT NULL,
    NY_FOR_VEILEDER boolean DEFAULT false NOT NULL,
    MANUELL         boolean DEFAULT false NOT NULL,
    STARTDATO       TIMESTAMP,

    PRIMARY KEY (AKTOERID)
);

CREATE UNIQUE INDEX OPPFOLGING_DATA_IDX ON OPPFOLGING_DATA (VEILEDERID);

CREATE TABLE OPPFOLGINGSBRUKER_ARENA
(
    AKTOERID                   VARCHAR(20) NOT NULL,
    FODSELSNR                  VARCHAR(33),
    FORMIDLINGSGRUPPEKODE      VARCHAR(15),
    ISERV_FRA_DATO             TIMESTAMP,
    ETTERNAVN                  VARCHAR(90),
    FORNAVN                    VARCHAR(90),
    NAV_KONTOR                 varchar(24),
    KVALIFISERINGSGRUPPEKODE   varchar(15),
    RETTIGHETSGRUPPEKODE       varchar(15),
    HOVEDMAALKODE              varchar(30),
    SIKKERHETSTILTAK_TYPE_KODE varchar(12),
    DISKRESJONSKODE            varchar(6),
    HAR_OPPFOLGINGSSAK         boolean DEFAULT false NOT NULL,
    SPERRET_ANSATT             boolean DEFAULT false NOT NULL,
    ER_DOED                    boolean DEFAULT false NOT NULL,
    DOED_FRA_DATO              TIMESTAMP,
    ENDRET_DATO                TIMESTAMP,

    PRIMARY KEY (AKTOERID)
);


CREATE VIEW OPTIMALISER_BRUKER AS
SELECT OD.AKTOERID, OD.OPPFOLGING,OD.STARTDATO, OD.NY_FOR_VEILEDER, OD.VEILEDERID, OBA.FODSELSNR, OBA.FORNAVN, OBA.ETTERNAVN, OBA.NAV_KONTOR, OBA.DISKRESJONSKODE
FROM OPPFOLGING_DATA OD
         LEFT JOIN OPPFOLGINGSBRUKER_ARENA OBA ON OBA.AKTOERID = OD.AKTOERID;


CREATE VIEW BRUKER AS
SELECT
       OD.AKTOERID, OD.OPPFOLGING, OD.STARTDATO, OD.NY_FOR_VEILEDER, OD.VEILEDERID,
       OD.MANUELL,

       OBA.FODSELSNR, OBA.FORNAVN, OBA.ETTERNAVN, OBA.NAV_KONTOR, OBA.ISERV_FRA_DATO,
       OBA.FORMIDLINGSGRUPPEKODE, OBA.KVALIFISERINGSGRUPPEKODE, OBA.RETTIGHETSGRUPPEKODE,
       OBA.HOVEDMAALKODE, OBA.SIKKERHETSTILTAK_TYPE_KODE, OBA.DISKRESJONSKODE,
       OBA.HAR_OPPFOLGINGSSAK, OBA.SPERRET_ANSATT, OBA.ER_DOED,

       D.VENTER_PA_BRUKER, D.VENTER_PA_NAV

FROM OPPFOLGING_DATA OD
         LEFT JOIN OPPFOLGINGSBRUKER_ARENA OBA ON OBA.AKTOERID = OD.AKTOERID
         LEFT JOIN DIALOG D ON D.AKTOERID = OD.AKTOERID;