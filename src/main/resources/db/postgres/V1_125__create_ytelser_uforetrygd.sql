CREATE TABLE YTELSER_UFORETRYGD
(
    norsk_ident     varchar(11) not null,
    virkningsdato   date        not null,
    uforegrad       integer     not null,
    rad_sist_endret timestamp   not null,

    PRIMARY KEY (norsk_ident)
);
