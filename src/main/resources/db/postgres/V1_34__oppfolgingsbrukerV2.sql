CREATE TABLE OPPFOLGINGSBRUKER_ARENA_V2
(
    FODSELSNR                  VARCHAR(33) NOT NULL,
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
    SPERRET_ANSATT             boolean DEFAULT false,
    ER_DOED                    boolean DEFAULT false,
    ENDRET_DATO                TIMESTAMP,

    PRIMARY KEY (FODSELSNR)
);