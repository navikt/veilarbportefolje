CREATE VIEW aktive_identer AS
SELECT bi_a.ident as aktorid, bi_f.ident as fnr
FROM bruker_identer bi_a
         inner join bruker_identer bi_f on bi_a.person = bi_f.person
where bi_a.gruppe = 'AKTORID'
  AND NOT bi_a.historisk
  and bi_f.gruppe = 'FOLKEREGISTERIDENT'
  AND NOT bi_f.historisk;


CREATE INDEX nav_kontor_idx ON oppfolgingsbruker_arena_v2 (nav_kontor);