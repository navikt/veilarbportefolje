CREATE TABLE arbeidssoekerregister_data
(
    identitetsnummer                     TEXT PRIMARY KEY,

    -- Arbeidssøkerperiode
    periode_id                           UUID,
    periode_start_tidspunkt              TIMESTAMPTZ,
    periode_avsluttet_tidspunkt          TIMESTAMPTZ,
    periode_rad_sist_endret              TIMESTAMPTZ,

    -- Opplysninger om arbeidssøker
    opplysning_id                        UUID,
    opplysning_jobbsituasjon_beskrivelse TEXT[],
    opplysning_utdanning_nus_kode        INTEGER,
    opplysning_utdanning_bestatt         BOOLEAN,
    opplysning_utdanning_godkjent        BOOLEAN,
    opplysning_sendt_inn_tidspunkt       TIMESTAMPTZ,
    opplysning_rad_sist_endret           TIMESTAMPTZ,

    -- Profilering
    profilering_id                       UUID,
    profilering_resultat                 TEXT,
    profilering_sendt_inn_tidspunkt      TIMESTAMPTZ,
    profilering_rad_sist_endret          TIMESTAMPTZ,

    synkronisering_status                TEXT        NOT NULL,

    rad_opprettet                        TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    rad_sist_endret                      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX periode_id_idx ON arbeidssoekerregister_data(periode_id);
CREATE INDEX synkronisering_status_idx ON arbeidssoekerregister_data(synkronisering_status);