CREATE TABLE YTELSER_UNGDOMSPROGRAM
(
    norsk_ident           varchar(11) not null,
    nyeste_periode_fom    date        not null,
    nyeste_periode_tom    date,
    maksdato              date        not null,
    har_forlenget_periode boolean     not null,
    rad_sist_endret       timestamp   not null,

    PRIMARY KEY (norsk_ident)
);
