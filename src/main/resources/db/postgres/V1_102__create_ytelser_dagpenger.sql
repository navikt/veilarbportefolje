CREATE TABLE YTELSER_DAGPENGER
(
    norsk_ident                       varchar(11) not null,
    nyeste_periode_fom                date        not null,
    nyeste_periode_tom                date,
    rettighetstype                    varchar(100),
    rad_sist_endret                   timestamp   not null,
    resterende_dager_beregnet_pa_dato date,
    resterende_dager_antall           integer,

    PRIMARY KEY (norsk_ident)
);
