CREATE TABLE TILTAKSHENDELSE
(
    id               uuid         not null primary key,
    fnr              varchar(11)  not null,
    opprettet        timestamp    not null,
    tekst            varchar(100) not null,
    lenke            varchar(100) not null,
    tiltakstype_kode varchar(20),
    avsender         varchar(20)  not null,
    sist_endret      timestamp    not null
);

CREATE INDEX IDX_TILTAKSHENDELSE_IDENT on TILTAKSHENDELSE (fnr);
