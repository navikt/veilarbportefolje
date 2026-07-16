CREATE TABLE LAGREDE_FILTER_VEILEDERGRUPPER
(
    FILTER_ID        SERIAL       not null,
    ENHET_ID         VARCHAR(32)  not null,
    FILTER_NAVN      text not null,
    VEILEDER_IDENTER text[]       not null,
    OPPRETTET        timestamp    not null,
    rad_sist_endret  timestamp    not null,

    PRIMARY KEY (FILTER_ID)
);

CREATE INDEX LAGREDE_FILTER_VEILEDERGRUPPER_ENHET_ID_IDX
    ON LAGREDE_FILTER_VEILEDERGRUPPER (enhet_id);
