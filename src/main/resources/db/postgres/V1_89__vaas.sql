CREATE TABLE VAAS
(
    hendelse_id      varchar(20)  NOT NULL PRIMARY KEY,
    fnr            VARCHAR(11)  NOT NULL,
    avsender         varchar(20)  NOT NULL,
    opprettet        timestamp    not null,
    hendelse_navn    varchar(100) not null,
    hendelse_lenke    varchar(100) not null,
    tiltakstype_kode varchar(20)
);

CREATE INDEX IDX_VAAS_IDENT on VAAS (ident);