CREATE TABLE YTELSER_DAGPENGER
(
    norsk_ident                    varchar(11)  not null,
    nyeste_periode_fom             date         not null,
    nyeste_periode_tom             date,
    rettighetstype                 varchar(100) not null,
    dato_antall_dager_ble_beregnet date,
    antall_resterende_dager        integer,
    rad_sist_endret                timestamp    not null,

    PRIMARY KEY (norsk_ident)
);
