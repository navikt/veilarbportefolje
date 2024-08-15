CREATE TABLE TILTAKSHENDELSE
(
    hendelse_id        varchar(20)  not null primary key,
    fnr                varchar(11)  not null,
    hendelse_opprettet timestamp    not null,
    hendelse_tekst     varchar(100) not null,
    hendelse_lenke     varchar(100) not null,
    tiltakstype_kode   varchar(20),
    avsender           varchar(20)  not null,
    sist_endret        timestamp    not null
);

CREATE INDEX IDX_TILTAKSHENDELSE_IDENT on TILTAKSHENDELSE (fnr);
