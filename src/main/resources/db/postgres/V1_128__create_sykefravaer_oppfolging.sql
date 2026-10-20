CREATE TABLE SYKEFRAVAER_OPPFOLGING
(
    norsk_ident      varchar(11) not null,
    start            date        not null,
    slutt            date        not null,
    har_arbeidsgiver boolean     not null,
    rad_sist_endret  timestamp   not null,

    PRIMARY KEY (norsk_ident)
);
