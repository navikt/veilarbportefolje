DROP  TABLE  YTELSER_AAP;
CREATE TABLE YTELSER_AAP
(
    norsk_ident     varchar(11)  not null,
    status          varchar(100) not null,
    saksid          varchar(100) not null,
    nyeste_periode_fom     date,
    nyeste_periode_tom     date,
    rettighetstype  varchar(100),
    opphorsaarsak   varchar(100),
    rad_sist_endret timestamp    not null,

    PRIMARY KEY (norsk_ident)
);
