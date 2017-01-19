CREATE TABLE T4_VEILARBPORTEFOLJE.OPPFOLGINGSBRUKER
(
  PERSON_ID                  NUMBER PRIMARY KEY,
  FODSELSNR                  VARCHAR2(11)            NOT NULL,
  ETTERNAVN                  VARCHAR2(30)            NOT NULL,
  FORNAVN                    VARCHAR2(30)            NOT NULL,
  NAV_KONTOR                 VARCHAR2(2),
  FORMIDLINGSGRUPPEKODE      VARCHAR2(5)             NOT NULL,
  ISERV_FRA_DATO             TIMESTAMP,
  KVALIFISERINGSGRUPPEKODE   VARCHAR2(5)             NOT NULL,
  RETTIGHETSGRUPPEKODE       VARCHAR2(5)             NOT NULL,
  HOVEDMAALKODE              VARCHAR2(10),
  SIKKERHETSTILTAK_TYPE_KODE VARCHAR2(4),
  FR_KODE                    VARCHAR2(2),
  SPERRET_ANSATT             VARCHAR2(1) DEFAULT 'N' NOT NULL,
  ER_DOED                    VARCHAR2(1) DEFAULT 'N' NOT NULL,
  DOED_FRA_DATO              TIMESTAMP,
  TIDSSTEMPEL                TIMESTAMP                    NOT NULL,

  CONSTRAINT SPERRET_ANSATT_BOOL CHECK (SPERRET_ANSATT IN ('J', 'N')),
  CONSTRAINT ER_DOED_BOOL CHECK (ER_DOED IN ('J', 'N'))
);
CREATE UNIQUE INDEX "FODSELSNR_uindex"
  ON T4_VEILARBPORTEFOLJE.OPPFOLGINGSBRUKER (FODSELSNR);
