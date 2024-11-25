CREATE TABLE FILTERHENDELSER
(
    id                uuid         not null primary key,
    personID          varchar(11)  not null,

    hendelse_navn     varchar(100) not null,
    hendelse_dato     timestamp    not null,
    hendelse_lenke    varchar(100) not null,
    hendelse_detaljer varchar(100),

    kategori          varchar(100) not null,
    avsender          varchar(100) not null,
    opprettet         timestamp    not null, --  Tidspunktet rada vart oppretta
    sist_endret       timestamp    not null  --  Tidspunktet rada vart endra, lik som oppdateringstidspunkt som standard
);

CREATE INDEX IDX_FILTERHENDELSER_IDENT on FILTERHENDELSER (personID);
