drop table if exists LAGREDE_FILTER_MINE_FILTER;;

/*CREATE TABLE LAGREDE_FILTER_MINE_FILTER
(
    FILTER_ID              SERIAL      not null,
    VEILEDER_IDENT         VARCHAR(32) not null,
    FILTER_NAVN            text        not null,
    AKTIVE_FILTER_VALG     jsonb       not null,
    SORT_ORDER             integer     not null default 0,
    AKTIV                  boolean     not null default true,
    IKKE_AKTIV_BESKRIVELSE text        not null default '',
    OPPRETTET              timestamp   not null,
    RAD_SIST_ENDRET        timestamp   not null,

    PRIMARY KEY (FILTER_ID)
);

CREATE INDEX MINE_FILTER_VEILEDER_IDENT_IDX
    ON LAGREDE_FILTER_MINE_FILTER (VEILEDER_IDENT);*/
