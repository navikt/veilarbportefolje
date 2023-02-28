CREATE TABLE enslige_forsorgere_aktivitet_type
(
    id             SERIAL PRIMARY KEY,
    aktivitet_type VARCHAR(100) NOT NULL
);

CREATE INDEX aktivitet_enslige_forsorgere_aktivitet_typeenslige_forsorgere_aktivitet_type_indx on enslige_forsorgere_aktivitet_type (aktivitet_type);

INSERT INTO enslige_forsorgere_aktivitet_type(aktivitet_type)
VALUES ('MIGRERING'),
       ('IKKE_AKTIVITETSPLIKT'),
       ('BARN_UNDER_ETT_ÅR'),
       ('FORSØRGER_I_ARBEID'),
       ('FORSØRGER_I_UTDANNING'),
       ('FORSØRGER_REELL_ARBEIDSSØKER'),
       ('FORSØRGER_ETABLERER_VIRKSOMHET'),
       ('BARNET_SÆRLIG_TILSYNSKREVENDE'),
       ('FORSØRGER_MANGLER_TILSYNSORDNING'),
       ('FORSØRGER_ER_SYK'),
       ('BARNET_ER_SYKT'),
       ('UTVIDELSE_FORSØRGER_I_UTDANNING'),
       ('UTVIDELSE_BARNET_SÆRLIG_TILSYNSKREVENDE'),
       ('FORLENGELSE_MIDLERTIDIG_SYKDOM'),
       ('FORLENGELSE_STØNAD_PÅVENTE_ARBEID'),
       ('FORLENGELSE_STØNAD_PÅVENTE_ARBEID_REELL_ARBEIDSSØKER'),
       ('FORLENGELSE_STØNAD_PÅVENTE_OPPSTART_KVALIFISERINGSPROGRAM'),
       ('FORLENGELSE_STØNAD_PÅVENTE_TILSYNSORDNING'),
       ('FORLENGELSE_STØNAD_PÅVENTE_UTDANNING'),
       ('FORLENGELSE_STØNAD_UT_SKOLEÅRET');

CREATE TABLE enslige_forsorgere_stonad_type
(
    id          SERIAL PRIMARY KEY,
    stonad_type VARCHAR(100) NOT NULL
);

CREATE INDEX stonad_enslige_forsorgere_stonad_type_indx on enslige_forsorgere_stonad_type (stonad_type);

INSERT INTO enslige_forsorgere_stonad_type(stonad_type)
VALUES ('OVERGANGSSTØNAD'),
       ('BARNETILSYN'),
       ('SKOLEPENGER');

CREATE TABLE enslige_forsorgere_vedtaksperiode_type
(
    id           SERIAL PRIMARY KEY,
    periode_type VARCHAR(100) NOT NULL
);

CREATE INDEX periode_enslige_forsorgere_vedtaksperiode_type_indx on enslige_forsorgere_vedtaksperiode_type (periode_type);

INSERT INTO enslige_forsorgere_vedtaksperiode_type(periode_type)
VALUES ('MIGRERING'),
       ('FORLENGELSE'),
       ('HOVEDPERIODE'),
       ('SANKSJON'),
       ('PERIODE_FØR_FØDSEL'),
       ('UTVIDELSE'),
       ('NY_PERIODE_FOR_NYTT_BARN');

CREATE TABLE enslige_forsorgere_vedtaksresultat_type
(
    id                   SERIAL PRIMARY KEY,
    vedtaksresultat_type VARCHAR(100) NOT NULL
);

CREATE INDEX vedtaksresultat_enslige_forsorgere_vedtaksresultat_type_indx on enslige_forsorgere_vedtaksresultat_type (vedtaksresultat_type);

INSERT INTO enslige_forsorgere_vedtaksresultat_type(vedtaksresultat_type)
VALUES ('INNVILGET'),
       ('OPPHØRT'),
       ('AVSLÅTT');


CREATE TABLE enslige_forsorgere
(
    vedtakId        bigint primary key,
    personIdent     VARCHAR(30) not null,
    stonadstype     integer REFERENCES enslige_forsorgere_stonad_type (id),
    vedtaksresultat integer REFERENCES enslige_forsorgere_vedtaksresultat_type (id),
    oppdatert       timestamp DEFAULT now()
);

CREATE INDEX personident_enslige_forsorgere_indx ON enslige_forsorgere (personIdent);

CREATE TABLE enslige_forsorgere_periode
(
    vedtakId       bigint REFERENCES enslige_forsorgere (vedtakId),
    fra_dato       TIMESTAMP,
    til_dato       TIMESTAMP,
    periodetype    integer REFERENCES enslige_forsorgere_vedtaksperiode_type (id),
    aktivitetstype integer REFERENCES enslige_forsorgere_aktivitet_type (id)
);

CREATE TABLE enslige_forsorgere_barn
(
    vedtakId   bigint REFERENCES enslige_forsorgere (vedtakId),
    fnr        VARCHAR(11),
    termindato DATE
);
