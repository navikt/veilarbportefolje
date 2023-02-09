CREATE TABLE ef_aktivitet_type
(
    id             SERIAL PRIMARY KEY,
    aktivitet_type VARCHAR(100) NOT NULL
);

INSERT INTO ef_aktivitet_type(id, aktivitet_type)
VALUES (1, 'MIGRERING'),
       (2, 'IKKE_AKTIVITETSPLIKT'),
       (3, 'BARN_UNDER_ETT_ÅR'),
       (4, 'FORSØRGER_I_ARBEID'),
       (5, 'FORSØRGER_I_UTDANNING'),
       (6, 'FORSØRGER_REELL_ARBEIDSSØKER'),
       (7, 'FORSØRGER_ETABLERER_VIRKSOMHET'),
       (8, 'BARNET_SÆRLIG_TILSYNSKREVENDE'),
       (9, 'FORSØRGER_MANGLER_TILSYNSORDNING'),
       (10, 'FORSØRGER_ER_SYK'),
       (11, 'BARNET_ER_SYKT'),
       (12, 'UTVIDELSE_FORSØRGER_I_UTDANNING'),
       (13, 'UTVIDELSE_BARNET_SÆRLIG_TILSYNSKREVENDE'),
       (14, 'FORLENGELSE_MIDLERTIDIG_SYKDOM'),
       (15, 'FORLENGELSE_STØNAD_PÅVENTE_ARBEID'),
       (16, 'FORLENGELSE_STØNAD_PÅVENTE_ARBEID_REELL_ARBEIDSSØKER'),
       (17, 'FORLENGELSE_STØNAD_PÅVENTE_OPPSTART_KVALIFISERINGSPROGRAM'),
       (18, 'FORLENGELSE_STØNAD_PÅVENTE_TILSYNSORDNING'),
       (19, 'FORLENGELSE_STØNAD_PÅVENTE_UTDANNING'),
       (20, 'FORLENGELSE_STØNAD_UT_SKOLEÅRET');

CREATE TABLE ef_stonad_type
(
    id          SERIAL PRIMARY KEY,
    stonad_type VARCHAR(100) NOT NULL
);

INSERT INTO ef_stonad_type(id, stonad_type)
VALUES (1, 'OVERGANGSSTØNAD'),
       (2, 'BARNETILSYN'),
       (3, 'SKOLEPENGER');

CREATE TABLE ef_vedtaksperiode_type
(
    id           SERIAL PRIMARY KEY,
    periode_type VARCHAR(100) NOT NULL
);
INSERT INTO ef_vedtaksperiode_type(id, periode_type)
VALUES (1, 'MIGRERING'),
       (2, 'FORLENGELSE'),
       (3, 'HOVEDPERIODE'),
       (4, 'SANKSJON'),
       (5, 'PERIODE_FØR_FØDSEL'),
       (6, 'UTVIDELSE'),
       (7, 'NY_PERIODE_FOR_NYTT_BARN');

CREATE TABLE ef_vedtaksresultat_type
(
    id                   SERIAL PRIMARY KEY,
    vedtaksresultat_type VARCHAR(100) NOT NULL
);

INSERT INTO ef_vedtaksresultat_type(id, vedtaksresultat_type)
VALUES (1, 'INNVILGET'),
       (2, 'OPPHØRT'),
       (3, 'AVSLÅTT');


CREATE TABLE enslige_forsorgere
(
    vedtakId        bigint primary key,
    personIdent     VARCHAR(30) not null,
    stønadstype     integer REFERENCES ef_stonad_type (id),
    vedtaksresultat integer REFERENCES ef_vedtaksresultat_type (id)
);

CREATE TABLE enslige_forsorgere_periode
(
    vedtakId       integer REFERENCES enslige_forsorgere (vedtakId),
    fra_dato       TIMESTAMP,
    til_dato       TIMESTAMP,
    periodetype    integer REFERENCES ef_vedtaksperiode_type (id),
    aktivitetstype integer REFERENCES ef_aktivitet_type (id)
);

CREATE TABLE enslige_forsorgere_barn
(
    vedtakId   integer REFERENCES enslige_forsorgere (vedtakId),
    fnr        VARCHAR(11),
    termindato DATE
);